package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static com.placido.certification.exams.session.StartResult.Outcome.ALREADY_ACTIVE;
import static com.placido.certification.exams.session.StartResult.Outcome.CREATED;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ExamSessionStarter {

    private static final int SCHEMA_VERSION = 1;
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    private final Path examsRoot;
    private final Supplier<OffsetDateTime> clock;

    public ExamSessionStarter(Path examsRoot) {
        this(examsRoot, () -> OffsetDateTime.now(DEFAULT_ZONE));
    }

    public ExamSessionStarter(Path examsRoot, Supplier<OffsetDateTime> clock) {
        this.examsRoot = Objects.requireNonNull(examsRoot, "examsRoot");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public StartResult start(String examId) {
        Objects.requireNonNull(examId, "examId");
        ExamDefinition definition = ExamDefinition.load(definitionFile(examId), examId);

        Path sessionsDir = examsRoot.resolve("sessions");
        StoreIndex store = readStore(sessionsDir);

        for (StoredSession existing : store.sessions) {
            if (existing.status().isActive()
                    && existing.examId().equals(examId)
                    && existing.examVersion() == definition.version()) {
                return new StartResult(ALREADY_ACTIVE, existing.sessionId(), existing.file());
            }
        }

        OffsetDateTime now = clock.get();
        String timestamp = SessionTime.timestamp(now);
        String sessionId = nextSessionId(now.toLocalDate(), examId, store.sessionIds, sessionsDir);

        ExamSession session = new ExamSession(
                SCHEMA_VERSION,
                sessionId,
                examId,
                definition.version(),
                IN_PROGRESS,
                timestamp,
                timestamp,
                null,
                definition.questionOrder().getFirst(),
                0,
                definition.durationSeconds(),
                definition.totalQuestions(),
                0, 0, 0, 0,
                new LinkedHashMap<>());

        Path file = sessionsDir.resolve(sessionId + ".yaml");
        try {
            Files.createDirectories(sessionsDir);
            Files.writeString(file, session.toYamlText());
        } catch (IOException e) {
            throw new ExamSessionException(SESSION_STORE_UNREADABLE,
                    "Não foi possível persistir a sessão " + file + ": " + e.getMessage());
        }
        return new StartResult(CREATED, sessionId, file);
    }

    private Path definitionFile(String examId) {
        return examsRoot.resolve(examId).resolve("definition.yaml");
    }

    private StoreIndex readStore(Path sessionsDir) {
        Set<String> sessionIds = new HashSet<>();
        List<StoredSession> sessions = new ArrayList<>();
        if (!Files.isDirectory(sessionsDir)) {
            return new StoreIndex(sessionIds, sessions);
        }
        try (Stream<Path> files = Files.list(sessionsDir)) {
            List<Path> sessionFiles = files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".yaml"))
                    .filter(file -> !file.getFileName().toString().startsWith("."))
                    .sorted()
                    .toList();
            for (Path file : sessionFiles) {
                sessions.add(readSessionFile(file, sessionIds));
            }
        } catch (IOException e) {
            throw new ExamSessionException(SESSION_STORE_UNREADABLE,
                    "Não foi possível ler o diretório de sessões " + sessionsDir + ": " + e.getMessage());
        }
        return new StoreIndex(sessionIds, sessions);
    }

    private StoredSession readSessionFile(Path file, Set<String> sessionIds) {
        Map<String, Object> data = SessionYaml.parse(file);
        String sessionId = requireStoreString(data, "sessionId", file);
        String examId = requireStoreString(data, "examId", file);
        String statusName = requireStoreString(data, "status", file);
        Object versionValue = data.get("examVersion");
        if (!(versionValue instanceof Number number)) {
            throw invalidStore(file, "campo 'examVersion' ausente ou não inteiro");
        }
        ExamSessionStatus status;
        try {
            status = ExamSessionStatus.valueOf(statusName);
        } catch (IllegalArgumentException e) {
            throw invalidStore(file, "status desconhecido: '" + statusName + "'");
        }
        sessionIds.add(sessionId);
        return new StoredSession(sessionId, examId, number.intValue(), status, file);
    }

    private static String requireStoreString(Map<String, Object> data, String key, Path file) {
        Object value = data.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw invalidStore(file, "campo '" + key + "' ausente ou inválido");
        }
        return s;
    }

    private static ExamSessionException invalidStore(Path file, String detail) {
        return new ExamSessionException(SESSION_STORE_INVALID,
                "Sessão inválida em " + file + ": " + detail);
    }

    private static String nextSessionId(LocalDate date, String examId, Set<String> usedIds, Path sessionsDir) {
        int sequence = 1;
        while (true) {
            String candidate = sessionId(date, examId, sequence);
            if (!usedIds.contains(candidate) && !Files.exists(sessionsDir.resolve(candidate + ".yaml"))) {
                return candidate;
            }
            sequence++;
        }
    }

    private static String sessionId(LocalDate date, String examId, int sequence) {
        return "%s-%s-%02d".formatted(date, examId, sequence);
    }

    private record StoredSession(String sessionId, String examId, int examVersion, ExamSessionStatus status, Path file) {
    }

    private record StoreIndex(Set<String> sessionIds, List<StoredSession> sessions) {
    }
}