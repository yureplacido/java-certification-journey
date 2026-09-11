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
import java.util.LinkedHashMap;
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

        SessionComputation computation = SessionComputation.compute(questions, definition);
        computation.requireConsistentResult(result, file);

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
        writeArtifact(artifact, sessionId, examId, examVersion, nowStamp, computation);
        return new AnalysisResult(outcome, sessionId, file, artifact);
    }

    private void writeArtifact(Path artifact, String sessionId, String examId, int examVersion,
            String nowStamp, SessionComputation computation) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schemaVersion", ARTIFACT_SCHEMA_VERSION);
        content.put("sessionId", sessionId);
        content.put("examId", examId);
        content.put("examVersion", examVersion);
        content.put("analyzedAt", nowStamp);
        content.put("summary", computation.summaryMap());
        content.put("correctQuestions", computation.correctQuestions());
        content.put("wrongQuestions", computation.wrongQuestions());
        content.put("unansweredQuestions", computation.unansweredQuestions());
        content.put("bySection", computation.bySection());
        content.put("byTopic", computation.byTopic());

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
}