package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_VERSION_MISMATCH;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.INVALID_ANSWER_OPTION;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.QUESTION_NOT_IN_EXAM;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_ANSWERABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.placido.certification.exams.session.AnswerResult.Outcome;

public final class ExamSessionAnswerer {

    private static final int SCHEMA_VERSION = 1;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final Pattern CANONICAL_QUESTION_ID = Pattern.compile("Q\\d{2,}");
    private static final Set<String> DEFAULT_OPTIONS = Set.of("A", "B", "C", "D");

    private final Path examsRoot;
    private final Supplier<OffsetDateTime> clock;

    public ExamSessionAnswerer(Path examsRoot) {
        this(examsRoot, () -> OffsetDateTime.now(DEFAULT_ZONE));
    }

    public ExamSessionAnswerer(Path examsRoot, Supplier<OffsetDateTime> clock) {
        this.examsRoot = Objects.requireNonNull(examsRoot, "examsRoot");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AnswerResult answer(String sessionId, String questionId, String answer) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(questionId, "questionId");
        Objects.requireNonNull(answer, "answer");
        if (answer.isBlank()) {
            throw new ExamSessionException(INVALID_ANSWER_OPTION,
                    "Resposta vazia ou em branco para " + questionId + " (opções permitidas: A-D)");
        }

        Path file = examsRoot.resolve("sessions").resolve(sessionId + ".yaml");
        if (!Files.isRegularFile(file)) {
            throw new ExamSessionException(SESSION_NOT_FOUND,
                    "Sessão não encontrada: " + sessionId + " (" + file + ")");
        }
        Map<String, Object> data = SessionYaml.parse(file);

        String storedSessionId = SessionParsing.requireString(data, "sessionId", file);
        if (!storedSessionId.equals(sessionId)) {
            throw SessionParsing.invalidStore(file,
                    "sessionId interno diverge do nome do arquivo: " + storedSessionId);
        }
        String examId = SessionParsing.requireString(data, "examId", file);
        int examVersion = SessionParsing.requireInt(data, "examVersion", file);
        ExamSessionStatus status = SessionParsing.parseStatus(data, file);
        String currentQuestion = SessionParsing.requireString(data, "currentQuestion", file);
        if (!CANONICAL_QUESTION_ID.matcher(currentQuestion).matches()) {
            throw SessionParsing.invalidStore(file,
                    "currentQuestion não é um questionId canônico: " + currentQuestion);
        }
        int elapsedSeconds = SessionParsing.requireInt(data, "elapsedSeconds", file);
        int remainingSeconds = SessionParsing.requireInt(data, "remainingSeconds", file);
        int totalQuestions = SessionParsing.requireInt(data, "totalQuestions", file);
        String startedAt = SessionParsing.requireString(data, "startedAt", file);
        String lastActivityAt = SessionParsing.requireString(data, "lastActivityAt", file);
        Map<String, Map<String, Object>> questions = SessionParsing.readQuestions(data, file);

        if (status != IN_PROGRESS) {
            throw new ExamSessionException(SESSION_NOT_ANSWERABLE,
                    "Sessão " + sessionId + " em estado '" + status
                            + "' não pode registrar resposta (answer)");
        }

        ExamDefinition definition = ExamDefinition.load(
                examsRoot.resolve(examId).resolve("definition.yaml"), examId);
        if (definition.version() != examVersion) {
            throw new ExamSessionException(EXAM_DEFINITION_VERSION_MISMATCH,
                    "Sessão " + sessionId + " referencia definition v" + examVersion
                            + " mas definition.yaml está na v" + definition.version());
        }
        if (!definition.questionOrder().contains(questionId)) {
            throw new ExamSessionException(QUESTION_NOT_IN_EXAM,
                    "Questão " + questionId + " não pertence ao questionOrder de " + examId);
        }

        Set<String> allowed = allowedOptions(definition, questionId);
        if (!allowed.contains(answer)) {
            throw new ExamSessionException(INVALID_ANSWER_OPTION,
                    "Resposta '" + answer + "' inválida para " + questionId
                            + " (opções permitidas: " + allowed + ")");
        }

        OffsetDateTime now = clock.get();
        String nowStamp = SessionTime.timestamp(now);
        SessionTime.Accounting accounting = SessionTime.consumeFromLastActivity(
                lastActivityAt, elapsedSeconds, remainingSeconds, now, file);

        Map<String, Map<String, Object>> updated = new LinkedHashMap<>(questions);
        Map<String, Object> previous = updated.get(questionId);
        Outcome outcome = outcomeOf(previous, answer);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("status", "ANSWERED");
        entry.put("answer", answer);
        entry.put("updatedAt", nowStamp);
        if (previous != null && previous.get("flagged") instanceof Boolean flagged) {
            entry.put("flagged", flagged);
        }
        updated.put(questionId, entry);

        Counters counters = deriveCounters(updated);

        ExamSession session = new ExamSession(
                SCHEMA_VERSION, sessionId, examId, examVersion, IN_PROGRESS,
                startedAt, nowStamp, null, currentQuestion,
                accounting.elapsedSeconds(), accounting.remainingSeconds(), totalQuestions,
                counters.answered(), counters.skipped(), counters.visited(), counters.flagged(),
                updated);

        persistAtomically(file, session.toYamlText());
        return new AnswerResult(outcome, sessionId, file, questionId);
    }

    private static Outcome outcomeOf(Map<String, Object> previous, String answer) {
        if (previous != null && "ANSWERED".equals(previous.get("status"))) {
            Object stored = previous.get("answer");
            if (stored != null && String.valueOf(stored).equals(answer)) {
                return Outcome.UNCHANGED;
            }
            return Outcome.UPDATED;
        }
        return Outcome.ANSWERED;
    }

    private static Set<String> allowedOptions(ExamDefinition definition, String questionId) {
        Object options = definition.answers().get(questionId).get("options");
        if (options instanceof List<?> list) {
            Set<String> result = new LinkedHashSet<>();
            for (Object option : list) {
                result.add(String.valueOf(option));
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return DEFAULT_OPTIONS;
    }

    private static Counters deriveCounters(Map<String, Map<String, Object>> questions) {
        int answered = 0;
        int skipped = 0;
        int visited = 0;
        int flagged = 0;
        for (Map<String, Object> entry : questions.values()) {
            Object rawStatus = entry.get("status");
            String status = rawStatus == null ? "" : String.valueOf(rawStatus);
            if ("ANSWERED".equals(status)) {
                answered++;
            }
            if ("SKIPPED".equals(status)) {
                skipped++;
            }
            if (!status.isBlank() && !"NOT_VISITED".equals(status)) {
                visited++;
            }
            if (Boolean.TRUE.equals(entry.get("flagged"))) {
                flagged++;
            }
        }
        return new Counters(answered, skipped, visited, flagged);
    }

    private static void persistAtomically(Path file, String content) {
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temp, content);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new ExamSessionException(SESSION_STORE_UNREADABLE,
                    "Não foi possível persistir a sessão " + file + ": " + e.getMessage());
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup do arquivo temporário
        }
    }

    private record Counters(int answered, int skipped, int visited, int flagged) {
    }
}