package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_PAUSABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;

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

public final class ExamSessionPauser {

    private static final int SCHEMA_VERSION = 1;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final Pattern CANONICAL_QUESTION_ID = Pattern.compile("Q\\d{2,}");

    private final Path examsRoot;
    private final Supplier<OffsetDateTime> clock;

    public ExamSessionPauser(Path examsRoot) {
        this(examsRoot, () -> OffsetDateTime.now(DEFAULT_ZONE));
    }

    public ExamSessionPauser(Path examsRoot, Supplier<OffsetDateTime> clock) {
        this.examsRoot = Objects.requireNonNull(examsRoot, "examsRoot");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public PauseResult pause(String sessionId) {
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
        int elapsedSeconds = requireInt(data, "elapsedSeconds", file);
        int remainingSeconds = requireInt(data, "remainingSeconds", file);
        int totalQuestions = requireInt(data, "totalQuestions", file);
        int answeredQuestions = requireInt(data, "answeredQuestions", file);
        int skippedQuestions = requireInt(data, "skippedQuestions", file);
        int visitedQuestions = requireInt(data, "visitedQuestions", file);
        int flaggedQuestions = requireInt(data, "flaggedQuestions", file);
        String startedAt = requireString(data, "startedAt", file);
        String lastActivityAt = requireString(data, "lastActivityAt", file);
        Map<String, Map<String, Object>> questions = readQuestions(data, file);

        OffsetDateTime now = clock.get();
        String nowStamp = SessionTime.timestamp(now);

        return switch (status) {
            case IN_PROGRESS -> {
                SessionTime.Accounting accounting = SessionTime.consumeFromLastActivity(
                        lastActivityAt, elapsedSeconds, remainingSeconds, now, file);
                if (accounting.remainingSeconds() <= 0) {
                    yield expire(file, sessionId, examId, examVersion, currentQuestion,
                            accounting.elapsedSeconds(), 0, totalQuestions, answeredQuestions,
                            skippedQuestions, visitedQuestions, flaggedQuestions, startedAt, questions, nowStamp);
                }
                ExamSession paused = new ExamSession(
                        SCHEMA_VERSION, sessionId, examId, examVersion, PAUSED,
                        startedAt, nowStamp, null, currentQuestion,
                        accounting.elapsedSeconds(), accounting.remainingSeconds(), totalQuestions,
                        answeredQuestions, skippedQuestions, visitedQuestions, flaggedQuestions, questions);
                persist(file, paused);
                yield new PauseResult(PauseResult.Outcome.PAUSED, sessionId, file);
            }
            case PAUSED -> new PauseResult(PauseResult.Outcome.ALREADY_PAUSED, sessionId, file);
            default -> throw new ExamSessionException(SESSION_NOT_PAUSABLE,
                    "Sessão " + sessionId + " em estado '" + status + "' não pode ser pausada (pause)");
        };
    }

    private PauseResult expire(Path file, String sessionId, String examId, int examVersion,
            String currentQuestion, int elapsedSeconds, int remainingSeconds, int totalQuestions,
            int answeredQuestions, int skippedQuestions, int visitedQuestions, int flaggedQuestions,
            String startedAt, Map<String, Map<String, Object>> questions, String nowStamp) {
        ExamSession finished = new ExamSession(
                SCHEMA_VERSION, sessionId, examId, examVersion, FINISHED,
                startedAt, nowStamp, nowStamp, currentQuestion,
                elapsedSeconds, Math.max(0, remainingSeconds), totalQuestions,
                answeredQuestions, skippedQuestions, visitedQuestions, flaggedQuestions, questions);
        persist(file, finished);
        return new PauseResult(PauseResult.Outcome.EXPIRED, sessionId, file);
    }

    private void persist(Path file, ExamSession session) {
        try {
            Files.writeString(file, session.toYamlText());
        } catch (IOException e) {
            throw new ExamSessionException(SESSION_STORE_UNREADABLE,
                    "Não foi possível persistir a sessão " + file + ": " + e.getMessage());
        }
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