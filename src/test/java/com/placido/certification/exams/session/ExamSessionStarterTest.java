package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static com.placido.certification.exams.session.StartResult.Outcome.ALREADY_ACTIVE;
import static com.placido.certification.exams.session.StartResult.Outcome.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExamSessionStarterTest {

    private static final OffsetDateTime FIXED_NOW =
            OffsetDateTime.of(2026, 9, 10, 14, 30, 0, 0, ZoneOffset.ofHours(-3));

    private static final String VALID_DEFINITION = """
            examId: mock-01
            version: 1
            examType: mock
            javaVersion: "21"
            totalQuestions: 2
            durationSeconds: 120
            passingScore: 68
            questionOrder: [Q01, Q02]
            questions:
              Q01: { topic: language-basics, section: 1, difficulty: medium }
              Q02: { topic: oop, section: 3, difficulty: hard }
            answers:
              Q01: { correctOption: B, format: single-choice }
              Q02: { correctOption: D, format: single-choice }
            """;

    @TempDir
    Path temp;

    @Test
    void createsNewSessionWithInitialStatePersisted() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);

        StartResult result = new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01");

        assertEquals(CREATED, result.outcome());
        assertTrue(result.created());
        assertEquals("2026-09-10-mock-01-01", result.sessionId());
        assertEquals(temp.resolve("sessions/2026-09-10-mock-01-01.yaml"), result.sessionFile());
        assertTrue(Files.isRegularFile(result.sessionFile()));

        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals(1, integer(data, "schemaVersion"));
        assertEquals("2026-09-10-mock-01-01", data.get("sessionId"));
        assertEquals("mock-01", data.get("examId"));
        assertEquals(1, integer(data, "examVersion"));
        assertEquals("IN_PROGRESS", data.get("status"));
        assertEquals("2026-09-10T14:30:00-03:00", data.get("startedAt"));
        assertEquals("2026-09-10T14:30:00-03:00", data.get("lastActivityAt"));
        assertEquals("Q01", data.get("currentQuestion"));
        assertEquals(0, integer(data, "elapsedSeconds"));
        assertEquals(120, integer(data, "remainingSeconds"));
        assertEquals(2, integer(data, "totalQuestions"));
        assertEquals(0, integer(data, "answeredQuestions"));
        assertEquals(0, integer(data, "skippedQuestions"));
        assertEquals(0, integer(data, "visitedQuestions"));
        assertEquals(0, integer(data, "flaggedQuestions"));
        assertEquals(Map.of(), data.get("questions"));
        assertNull(data.get("finishedAt"));
        assertNull(data.get("result"));
        assertEquals(120, integer(data, "elapsedSeconds") + integer(data, "remainingSeconds"));
    }

    @Test
    void secondStartWhileActiveReturnsExistingSessionAndDoesNotCreateAnother() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        ExamSessionStarter starter = new ExamSessionStarter(temp, () -> FIXED_NOW);
        StartResult first = starter.start("mock-01");

        StartResult second = starter.start("mock-01");

        assertEquals(ALREADY_ACTIVE, second.outcome());
        assertFalse(second.created());
        assertEquals(first.sessionId(), second.sessionId());
        assertEquals(first.sessionFile(), second.sessionFile());
        assertEquals(1, countSessionFiles());
    }

    @Test
    void pausedSessionCountsAsActiveSoStartReusesIt() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seedSession("2026-09-10-mock-01-01", "mock-01", 1, PAUSED);

        StartResult result = new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01");

        assertEquals(ALREADY_ACTIVE, result.outcome());
        assertEquals("2026-09-10-mock-01-01", result.sessionId());
        assertEquals(1, countSessionFiles());
    }

    @Test
    void finishedSessionAllowsNewSessionWithBumpedSequence() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seedSession("2026-09-10-mock-01-01", "mock-01", 1, FINISHED);

        StartResult result = new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01");

        assertEquals(CREATED, result.outcome());
        assertEquals("2026-09-10-mock-01-02", result.sessionId());
        assertEquals(2, countSessionFiles());
    }

    @Test
    void activeSessionForOlderVersionDoesNotBlockStartOfNewerVersion() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("version: 1", "version: 2"));
        seedSession("2026-09-10-mock-01-01", "mock-01", 1, IN_PROGRESS);

        StartResult result = new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01");

        assertEquals(CREATED, result.outcome());
        assertEquals("2026-09-10-mock-01-02", result.sessionId());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals(2, integer(data, "examVersion"));
    }

    @Test
    void activeSessionForOneExamDoesNotBlockAnotherExam() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        writeDefinition("mock-02", VALID_DEFINITION.replace("examId: mock-01", "examId: mock-02"));
        ExamSessionStarter starter = new ExamSessionStarter(temp, () -> FIXED_NOW);
        starter.start("mock-01");

        StartResult result = starter.start("mock-02");

        assertEquals(CREATED, result.outcome());
        assertEquals("2026-09-10-mock-02-01", result.sessionId());
        assertEquals(2, countSessionFiles());
    }

    @Test
    void startOfMissingDefinitionSignalsNotFound() {
        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-99"));

        assertEquals(EXAM_DEFINITION_NOT_FOUND, e.kind());
    }

    @Test
    void startRejectsNonCanonicalNumericQuestionIds() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("questionOrder: [Q01, Q02]", "questionOrder: [1, 2]"));

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01"));

        assertEquals(EXAM_DEFINITION_INVALID, e.kind());
    }

    @Test
    void startRejectsSingleDigitQuestionIds() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("questionOrder: [Q01, Q02]", "questionOrder: [Q1, Q2]"));

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01"));

        assertEquals(EXAM_DEFINITION_INVALID, e.kind());
    }

    @Test
    void startRejectsTotalQuestionsMismatch() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("totalQuestions: 2", "totalQuestions: 3"));

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01"));

        assertEquals(EXAM_DEFINITION_INVALID, e.kind());
    }

    @Test
    void unreadableSessionFileInStoreIsSignaled() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        Files.writeString(sessions.resolve("broken.yaml"), "a: [\ninvalid");

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01"));

        assertEquals(SESSION_STORE_INVALID, e.kind());
        assertTrue(e.getMessage().contains("broken.yaml"));
    }

    @Test
    void persistedFileKeepsTypedValuesOnReload() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);

        StartResult result = new ExamSessionStarter(temp, () -> FIXED_NOW).start("mock-01");
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());

        assertInstanceOf(Integer.class, data.get("examVersion"));
        assertInstanceOf(Integer.class, data.get("remainingSeconds"));
        assertInstanceOf(String.class, data.get("startedAt"));
        assertInstanceOf(String.class, data.get("currentQuestion"));
        assertInstanceOf(String.class, data.get("status"));
    }

    private void writeDefinition(String examId, String content) throws IOException {
        Path dir = temp.resolve(examId);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("definition.yaml"), content);
    }

    private void seedSession(String sessionId, String examId, int examVersion, ExamSessionStatus status)
            throws IOException {
        ExamSession session = new ExamSession(1, sessionId, examId, examVersion, status,
                "2026-09-10T09:00:00-03:00", "2026-09-10T09:10:00-03:00", null,
                "Q01", 100, 20, 2, 0, 0, 0, 0, new LinkedHashMap<>());
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        Files.writeString(sessions.resolve(sessionId + ".yaml"), session.toYamlText());
    }

    private long countSessionFiles() throws IOException {
        Path sessions = temp.resolve("sessions");
        if (!Files.isDirectory(sessions)) {
            return 0;
        }
        try (var files = Files.list(sessions)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".yaml")).count();
        }
    }

    private static int integer(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).intValue();
    }
}