package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_PAUSABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.ABANDONED;
import static com.placido.certification.exams.session.ExamSessionStatus.ANALYZED;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.NOT_STARTED;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static com.placido.certification.exams.session.PauseResult.Outcome.ALREADY_PAUSED;
import static com.placido.certification.exams.session.PauseResult.Outcome.EXPIRED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExamSessionPauserTest {

    private static final OffsetDateTime PAUSE_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime LATER_AT =
            OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, ZoneOffset.ofHours(-3));

    private static final String STARTED_AT = "2026-09-10T14:30:00-03:00";
    private static final String LAST_ACTIVITY_AT = "2026-09-10T14:50:00-03:00";

    private static final Map<String, Map<String, Object>> QUESTION_STATE = Map.of(
            "Q01", Map.of("status", "ANSWERED", "answer", "B"),
            "Q30", Map.of("status", "VISITED", "flagged", Boolean.TRUE));

    @TempDir
    Path temp;

    @Test
    void pauseInProgressSessionTransitionsToPausedAndPreservesState() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        PauseResult result = pauser(PAUSE_AT).pause("2026-09-10-mock-01-01");

        assertEquals(PauseResult.Outcome.PAUSED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("PAUSED", data.get("status"));
        assertEquals("2026-09-10-mock-01-01", data.get("sessionId"));
        assertEquals("mock-01", data.get("examId"));
        assertEquals(1, integer(data, "examVersion"));
        assertEquals("Q30", data.get("currentQuestion"));
        assertEquals(16, integer(data, "answeredQuestions"));
        assertEquals(2, integer(data, "skippedQuestions"));
        assertEquals(30, integer(data, "visitedQuestions"));
        assertEquals(4, integer(data, "flaggedQuestions"));
        assertEquals(STARTED_AT, data.get("startedAt"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("lastActivityAt"));
        assertNull(data.get("finishedAt"));
        assertEquals("B", questionOf(data, "Q01").get("answer"));
        assertEquals(Map.of("status", "VISITED", "flagged", Boolean.TRUE), questionsOf(data).get("Q30"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void pauseAccountsElapsedTimeSinceLastActivity() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        PauseResult result = pauser(PAUSE_AT).pause("2026-09-10-mock-01-01");

        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals(2700, integer(data, "elapsedSeconds"));
        assertEquals(4500, integer(data, "remainingSeconds"));
    }

    @Test
    void invariantElapsedPlusRemainingEqualsDurationIsKept() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        PauseResult result = pauser(PAUSE_AT).pause("2026-09-10-mock-01-01");

        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals(7200, integer(data, "elapsedSeconds") + integer(data, "remainingSeconds"));
    }

    @Test
    void pauseAlreadyPausedIsIdempotentAndLeavesFileUnchanged() throws IOException {
        String sessionId = "2026-09-10-mock-01-01";
        seed(sessionId, PAUSED, 2100, 5100);
        Path file = temp.resolve("sessions").resolve(sessionId + ".yaml");
        byte[] before = Files.readAllBytes(file);

        PauseResult result = pauser(PAUSE_AT).pause(sessionId);

        assertEquals(ALREADY_PAUSED, result.outcome());
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void clockStaysFrozenForAlreadyPausedSessionEvenAfterWallTimePasses() throws IOException {
        seed("2026-09-10-mock-01-01", PAUSED, 2100, 5100);

        PauseResult result = pauser(LATER_AT).pause("2026-09-10-mock-01-01");

        assertEquals(ALREADY_PAUSED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals(2100, integer(data, "elapsedSeconds"));
        assertEquals(5100, integer(data, "remainingSeconds"));
    }

    @Test
    void pauseNonExistentSessionFailsExplicitlyAndCreatesNothing() {
        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> pauser(PAUSE_AT).pause("2026-09-10-mock-99-01"));

        assertEquals(SESSION_NOT_FOUND, e.kind());
        assertTrue(e.getMessage().contains("2026-09-10-mock-99-01"));
        assertEquals(0, countSessionFiles());
    }

    @Test
    void pauseNotStartedSessionFails() throws IOException {
        assertNotPausable("2026-09-10-mock-01-01", NOT_STARTED);
    }

    @Test
    void pauseFinishedSessionFails() throws IOException {
        assertNotPausable("2026-09-10-mock-01-01", FINISHED);
    }

    @Test
    void pauseGradedSessionFails() throws IOException {
        assertNotPausable("2026-09-10-mock-01-01", GRADED);
    }

    @Test
    void pauseAnalyzedSessionFails() throws IOException {
        assertNotPausable("2026-09-10-mock-01-01", ANALYZED);
    }

    @Test
    void pauseAbandonedSessionFails() throws IOException {
        assertNotPausable("2026-09-10-mock-01-01", ABANDONED);
    }

    @Test
    void pauseWhenTimeRunsOutExpiresToFinished() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 6900, 300);

        PauseResult result = pauser(PAUSE_AT).pause("2026-09-10-mock-01-01");

        assertEquals(EXPIRED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("FINISHED", data.get("status"));
        assertEquals(7200, integer(data, "elapsedSeconds"));
        assertEquals(0, integer(data, "remainingSeconds"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("finishedAt"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("lastActivityAt"));
        assertEquals("B", questionOf(data, "Q01").get("answer"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void pauseWhenTimeReachesExactZeroExpiresInsteadOfStayingPaused() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 6600, 600);

        PauseResult result = pauser(PAUSE_AT).pause("2026-09-10-mock-01-01");

        assertEquals(EXPIRED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("FINISHED", data.get("status"));
        assertEquals(0, integer(data, "remainingSeconds"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("finishedAt"));
        assertEquals("Q30", data.get("currentQuestion"));
    }

    @Test
    void repeatedPauseDoesNotCreateAnotherSession() throws IOException {
        String sessionId = "2026-09-10-mock-01-01";
        seed(sessionId, IN_PROGRESS, 2100, 5100);
        ExamSessionPauser pauser = pauser(PAUSE_AT);

        PauseResult first = pauser.pause(sessionId);
        PauseResult second = pauser.pause(sessionId);

        assertEquals(PauseResult.Outcome.PAUSED, first.outcome());
        assertEquals(ALREADY_PAUSED, second.outcome());
        assertEquals(first.sessionId(), second.sessionId());
        assertEquals(1, countSessionFiles());
        Map<String, Object> data = SessionYaml.parse(second.sessionFile());
        assertEquals(2700, integer(data, "elapsedSeconds"));
        assertEquals(4500, integer(data, "remainingSeconds"));
    }

    private void assertNotPausable(String sessionId, ExamSessionStatus status) throws IOException {
        seed(sessionId, status, 2100, 5100);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> pauser(PAUSE_AT).pause(sessionId));

        assertEquals(SESSION_NOT_PAUSABLE, e.kind());
        assertTrue(e.getMessage().contains(status.name()));
    }

    private ExamSessionPauser pauser(OffsetDateTime now) {
        return new ExamSessionPauser(temp, () -> now);
    }

    private void seed(String sessionId, ExamSessionStatus status, int elapsed, int remaining) throws IOException {
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        ExamSession session = new ExamSession(1, sessionId, "mock-01", 1, status,
                STARTED_AT, LAST_ACTIVITY_AT, null, "Q30",
                elapsed, remaining, 50, 16, 2, 30, 4, QUESTION_STATE);
        Files.writeString(sessions.resolve(sessionId + ".yaml"), session.toYamlText());
    }

    private long countSessionFiles() {
        Path sessions = temp.resolve("sessions");
        if (!Files.isDirectory(sessions)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(sessions)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".yaml")).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> questionOf(Map<String, Object> data, String id) {
        return (Map<String, Object>) questionsOf(data).get(id);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> questionsOf(Map<String, Object> data) {
        return (Map<String, Object>) data.get("questions");
    }

    private static int integer(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).intValue();
    }
}