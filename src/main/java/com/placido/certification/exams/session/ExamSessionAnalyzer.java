package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_VERSION_MISMATCH;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_ANALYZABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.ANALYZED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class ExamSessionAnalyzer {

    public static final int ARTIFACT_SCHEMA_VERSION = 1;

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final Pattern CANONICAL_QUESTION_ID = Pattern.compile("Q\\d{2,}");

    private final Path examsRoot;
    private final Supplier<OffsetDateTime> clock;

    public ExamSessionAnalyzer(Path examsRoot) {
        this(examsRoot, () -> OffsetDateTime.now(DEFAULT_ZONE));
    }

    public ExamSessionAnalyzer(Path examsRoot, Supplier<OffsetDateTime> clock) {
        this.examsRoot = Objects.requireNonNull(examsRoot, "examsRoot");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AnalysisResult analyze(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        Path file = examsRoot.resolve("sessions").resolve(sessionId + ".yaml");
        if (!Files.isRegularFile(file)) {
            throw new ExamSessionException(SESSION_NOT_FOUND,
                    "Sessão não encontrada: " + sessionId + " (" + file + ")");
        }
        Map<String, Object> data = SessionYaml.parse(file);

        String storedSessionId = requireString(data, "sessionId", file);
        if (!storedSessionId.equals(sessionId)) {
            throw invalidStore(file, "sessionId interno diverge do nome do arquivo: " + storedSessionId);
        }
        String examId = requireString(data, "examId", file);
        int examVersion = requireInt(data, "examVersion", file);
        ExamSessionStatus status = parseStatus(data, file);
        String currentQuestion = requireString(data, "currentQuestion", file);
        if (!CANONICAL_QUESTION_ID.matcher(currentQuestion).matches()) {
            throw invalidStore(file, "currentQuestion não é um questionId canônico: " + currentQuestion);
        }
        requireInt(data, "elapsedSeconds", file);
        requireInt(data, "remainingSeconds", file);
        requireInt(data, "totalQuestions", file);
        requireInt(data, "answeredQuestions", file);
        requireInt(data, "skippedQuestions", file);
        requireInt(data, "visitedQuestions", file);
        requireInt(data, "flaggedQuestions", file);
        requireString(data, "startedAt", file);
        requireString(data, "lastActivityAt", file);
        Map<String, Map<String, Object>> questions = readQuestions(data, file);

        return switch (status) {
            case GRADED -> analyzeSessao(file, sessionId, examId, examVersion, data, questions,
                    AnalysisResult.Outcome.ANALYZED);
            case ANALYZED -> analyzeSessao(file, sessionId, examId, examVersion, data, questions,
                    AnalysisResult.Outcome.ALREADY_ANALYZED);
            default -> throw new ExamSessionException(SESSION_NOT_ANALYZABLE,
                    "Sessão " + sessionId + " em estado '" + status + "' não pode ser analisada (analyze)");
        };
    }

    private AnalysisResult analyzeSessao(Path file, String sessionId, String examId, int examVersion,
            Map<String, Object> data, Map<String, Map<String, Object>> questions,
            AnalysisResult.Outcome outcome) {
        ExamDefinition definition = ExamDefinition.load(
                examsRoot.resolve(examId).resolve("definition.yaml"), examId);
        if (definition.version() != examVersion) {
            throw new ExamSessionException(EXAM_DEFINITION_VERSION_MISMATCH,
                    "Sessão " + sessionId + " referencia definition v" + examVersion
                            + " mas definition.yaml está na v" + definition.version());
        }

        Map<String, Object> result = SessionYaml.asStringMap(data.get("result"));
        if (result == null) {
            throw invalidStore(file, "result ausente na sessão: execute grade antes de analyze");
        }

        Derived derived = derive(questions, definition);
        checkSummaryConsistent(derived, result, file);
        checkBySectionConsistent(derived.bySection, result.get("bySection"), file);

        String nowStamp = SessionTime.timestamp(clock.get());

        Map<String, Object> persisted = new LinkedHashMap<>(data);
        persisted.put("status", "ANALYZED");

        try {
            Files.writeString(file, SessionYaml.dump(persisted));
        } catch (IOException e) {
            throw new ExamSessionException(SESSION_STORE_UNREADABLE,
                    "Não foi possível persistir a sessão " + file + ": " + e.getMessage());
        }

        Path artifact = artifactFile(sessionId);
        writeArtifact(artifact, sessionId, examId, examVersion, nowStamp, derived);
        return new AnalysisResult(outcome, sessionId, file, artifact);
    }

    private void writeArtifact(Path artifact, String sessionId, String examId, int examVersion,
            String nowStamp, Derived derived) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schemaVersion", ARTIFACT_SCHEMA_VERSION);
        content.put("sessionId", sessionId);
        content.put("examId", examId);
        content.put("examVersion", examVersion);
        content.put("analyzedAt", nowStamp);
        content.put("summary", derived.summary);
        content.put("correctQuestions", derived.correctQuestions);
        content.put("wrongQuestions", derived.wrongQuestions);
        content.put("unansweredQuestions", derived.unansweredQuestions);
        content.put("bySection", derived.bySection);
        content.put("byTopic", derived.byTopic);

        try {
            Files.createDirectories(artifact.getParent());
            Files.writeString(artifact, SessionYaml.dump(content));
        } catch (IOException e) {
            throw new ExamSessionException(SESSION_STORE_UNREADABLE,
                    "Não foi possível escrever o artefato " + artifact + ": " + e.getMessage());
        }
    }

    private Path artifactFile(String sessionId) {
        return examsRoot.resolve("docs").resolve("study-log").resolve(sessionId + ".analysis.yaml");
    }

    private static Derived derive(Map<String, Map<String, Object>> questions, ExamDefinition definition) {
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        List<String> correctQuestions = new ArrayList<>();
        List<String> wrongQuestions = new ArrayList<>();
        List<String> unansweredQuestions = new ArrayList<>();
        Map<Integer, int[]> bySection = new LinkedHashMap<>();
        Map<String, int[]> byTopic = new LinkedHashMap<>();

        for (String id : definition.questionOrder()) {
            QuestionVerdict verdict = GradingRules.classify(questions.get(id),
                    String.valueOf(definition.answers().get(id).get("correctOption")));
            switch (verdict) {
                case CORRECT -> {
                    correct++;
                    correctQuestions.add(id);
                }
                case WRONG -> {
                    wrong++;
                    wrongQuestions.add(id);
                }
                case UNANSWERED -> {
                    unanswered++;
                    unansweredQuestions.add(id);
                }
            }
            int section = sectionOf(definition, id);
            int[] sectionBuckets = bySection.computeIfAbsent(section, k -> new int[2]);
            sectionBuckets[1]++;
            if (verdict == QuestionVerdict.CORRECT) {
                sectionBuckets[0]++;
            }
            String topic = String.valueOf(definition.questions().get(id).get("topic"));
            int[] topicBuckets = byTopic.computeIfAbsent(topic, k -> new int[2]);
            topicBuckets[1]++;
            if (verdict == QuestionVerdict.CORRECT) {
                topicBuckets[0]++;
            }
        }

        int scorePercent = GradingRules.scorePercent(correct, definition.totalQuestions());
        boolean passing = scorePercent >= definition.passingScore();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("correct", correct);
        summary.put("wrong", wrong);
        summary.put("unanswered", unanswered);
        summary.put("scorePercent", scorePercent);
        summary.put("passing", passing);
        return new Derived(summary, correct, wrong, unanswered, scorePercent, passing,
                correctQuestions, wrongQuestions, unansweredQuestions,
                toSectionMap(bySection), toTopicMap(byTopic));
    }

    private static Map<Integer, Map<String, Object>> toSectionMap(Map<Integer, int[]> raw) {
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, int[]> entry : raw.entrySet()) {
            result.put(entry.getKey(), countMap(entry.getValue()));
        }
        return result;
    }

    private static Map<String, Map<String, Object>> toTopicMap(Map<String, int[]> raw) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : raw.entrySet()) {
            result.put(entry.getKey(), countMap(entry.getValue()));
        }
        return result;
    }

    private static Map<String, Object> countMap(int[] buckets) {
        Map<String, Object> count = new LinkedHashMap<>();
        count.put("correct", buckets[0]);
        count.put("total", buckets[1]);
        return count;
    }

    private static void checkSummaryConsistent(Derived derived, Map<String, Object> result, Path file) {
        checkInt(result, "correct", derived.correct, file);
        checkInt(result, "wrong", derived.wrong, file);
        checkInt(result, "unanswered", derived.unanswered, file);
        checkInt(result, "scorePercent", derived.scorePercent, file);
        Object storedPassing = result.get("passing");
        if (!(storedPassing instanceof Boolean) || derived.passing != (Boolean) storedPassing) {
            throw invalidStore(file, "result.passing inconsistente com a derivação: " + storedPassing);
        }
    }

    private static void checkInt(Map<String, Object> result, String key, int expected, Path file) {
        Object value = result.get(key);
        if (!(value instanceof Number number) || number.intValue() != expected) {
            throw invalidStore(file, "result." + key + " inconsistente com a derivação: " + value);
        }
    }

    private static void checkBySectionConsistent(Map<Integer, Map<String, Object>> derived,
            Object rawStored, Path file) {
        Map<String, Object> stored = SessionYaml.asStringMap(rawStored);
        if (stored == null) {
            throw invalidStore(file, "result.bySection ausente ou inválido");
        }
        if (stored.size() != derived.size()) {
            throw invalidStore(file, "result.bySection com " + stored.size()
                    + " seções, derivação tem " + derived.size());
        }
        for (Map.Entry<String, Object> entry : stored.entrySet()) {
            int section = Integer.parseInt(entry.getKey());
            Map<String, Object> expected = derived.get(section);
            if (expected == null) {
                throw invalidStore(file, "result.bySection[" + section + "] não existe na derivação");
            }
            Map<String, Object> actual = SessionYaml.asStringMap(entry.getValue());
            if (actual == null) {
                throw invalidStore(file, "result.bySection[" + section + "] deve ser um mapa");
            }
            checkInt(actual, "correct", ((Number) expected.get("correct")).intValue(), file);
            checkInt(actual, "total", ((Number) expected.get("total")).intValue(), file);
        }
    }

    private static int sectionOf(ExamDefinition definition, String id) {
        return ((Number) definition.questions().get(id).get("section")).intValue();
    }

    private static ExamSessionStatus parseStatus(Map<String, Object> data, Path file) {
        Object value = data.get("status");
        if (!(value instanceof String statusName)) {
            throw invalidStore(file, "campo 'status' ausente ou inválido");
        }
        try {
            return ExamSessionStatus.valueOf(statusName);
        } catch (IllegalArgumentException e) {
            throw invalidStore(file, "status desconhecido: '" + statusName + "'");
        }
    }

    private static String requireString(Map<String, Object> data, String key, Path file) {
        Object value = data.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw invalidStore(file, "campo '" + key + "' ausente ou inválido");
        }
        return s;
    }

    private static int requireInt(Map<String, Object> data, String key, Path file) {
        Object value = data.get(key);
        if (!(value instanceof Number number)) {
            throw invalidStore(file, "campo '" + key + "' ausente ou não inteiro");
        }
        return number.intValue();
    }

    private static Map<String, Map<String, Object>> readQuestions(Map<String, Object> data, Path file) {
        Map<String, Object> raw = SessionYaml.asStringMap(data.get("questions"));
        if (raw == null) {
            throw invalidStore(file, "campo 'questions' ausente ou inválido");
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            Map<String, Object> perQuestion = SessionYaml.asStringMap(entry.getValue());
            if (perQuestion == null) {
                throw invalidStore(file, "questions['" + entry.getKey() + "'] deve ser um mapa");
            }
            result.put(entry.getKey(), new LinkedHashMap<>(perQuestion));
        }
        return result;
    }

    private static ExamSessionException invalidStore(Path file, String detail) {
        return new ExamSessionException(SESSION_STORE_INVALID,
                "Sessão inválida em " + file + ": " + detail);
    }

    private record Derived(Map<String, Object> summary, int correct, int wrong, int unanswered,
            int scorePercent, boolean passing, List<String> correctQuestions,
            List<String> wrongQuestions, List<String> unansweredQuestions,
            Map<Integer, Map<String, Object>> bySection, Map<String, Map<String, Object>> byTopic) {
    }
}