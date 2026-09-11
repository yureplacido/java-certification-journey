package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_VERSION_MISMATCH;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_GRADABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class ExamSessionGrader {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final Pattern CANONICAL_QUESTION_ID = Pattern.compile("Q\\d{2,}");

    private final Path examsRoot;
    private final Supplier<OffsetDateTime> clock;

    public ExamSessionGrader(Path examsRoot) {
        this(examsRoot, () -> OffsetDateTime.now(DEFAULT_ZONE));
    }

    public ExamSessionGrader(Path examsRoot, Supplier<OffsetDateTime> clock) {
        this.examsRoot = Objects.requireNonNull(examsRoot, "examsRoot");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public GradeResult grade(String sessionId) {
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

        ExamDefinition definition = ExamDefinition.load(
                examsRoot.resolve(examId).resolve("definition.yaml"), examId);
        if (definition.version() != examVersion) {
            throw new ExamSessionException(EXAM_DEFINITION_VERSION_MISMATCH,
                    "Sessão " + sessionId + " referencia definition v" + examVersion
                            + " mas definition.yaml está na v" + definition.version());
        }

        OffsetDateTime now = clock.get();
        String nowStamp = SessionTime.timestamp(now);

        return switch (status) {
            case FINISHED -> repersist(file, sessionId, data, questions, definition, nowStamp,
                    GradeResult.Outcome.GRADED);
            case GRADED -> repersist(file, sessionId, data, questions, definition, nowStamp,
                    GradeResult.Outcome.ALREADY_GRADED);
            default -> throw new ExamSessionException(SESSION_NOT_GRADABLE,
                    "Sessão " + sessionId + " em estado '" + status + "' não pode ser corrigida (grade)");
        };
    }

    private GradeResult repersist(Path file, String sessionId, Map<String, Object> data,
            Map<String, Map<String, Object>> questions, ExamDefinition definition,
            String nowStamp, GradeResult.Outcome outcome) {
        Computation computation = compute(questions, definition);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gradedAt", nowStamp);
        result.put("correct", computation.correct);
        result.put("wrong", computation.wrong);
        result.put("unanswered", computation.unanswered);
        result.put("scorePercent", computation.scorePercent);
        result.put("passing", computation.passing);
        result.put("bySection", computation.bySection);

        Map<String, Object> persisted = new LinkedHashMap<>(data);
        persisted.put("status", "GRADED");
        persisted.put("result", result);

        try {
            Files.writeString(file, SessionYaml.dump(persisted));
        } catch (IOException e) {
            throw new ExamSessionException(SESSION_STORE_UNREADABLE,
                    "Não foi possível persistir a sessão " + file + ": " + e.getMessage());
        }
        return new GradeResult(outcome, sessionId, file);
    }

    private static Computation compute(Map<String, Map<String, Object>> questions, ExamDefinition definition) {
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        Map<Integer, Map<String, Object>> bySection = new LinkedHashMap<>();
        List<String> order = definition.questionOrder();
        for (String id : order) {
            int section = sectionOf(definition, id);
            Map<String, Object> buckets = bySection.get(section);
            if (buckets == null) {
                buckets = new LinkedHashMap<>();
                buckets.put("correct", 0);
                buckets.put("total", 0);
                bySection.put(section, buckets);
            }
            buckets.put("total", ((Number) buckets.get("total")).intValue() + 1);

            QuestionVerdict verdict = GradingRules.classify(questions.get(id),
                    String.valueOf(definition.answers().get(id).get("correctOption")));
            switch (verdict) {
                case CORRECT -> {
                    correct++;
                    buckets.put("correct", ((Number) buckets.get("correct")).intValue() + 1);
                }
                case WRONG -> wrong++;
                case UNANSWERED -> unanswered++;
            }
        }
        int scorePercent = GradingRules.scorePercent(correct, definition.totalQuestions());
        boolean passing = scorePercent >= definition.passingScore();
        return new Computation(correct, wrong, unanswered, scorePercent, passing, bySection);
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

    private record Computation(int correct, int wrong, int unanswered, int scorePercent,
            boolean passing, Map<Integer, Map<String, Object>> bySection) {
    }
}