package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FINISHABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.ABANDONED;
import static com.placido.certification.exams.session.ExamSessionStatus.ANALYZED;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.NOT_STARTED;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static com.placido.certification.exams.session.FinishResult.Outcome.ALREADY_FINISHED;
import static com.placido.certification.exams.session.FinishResult.Outcome.TIMED_OUT;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExamSessionFinisherTest {

    private static final OffsetDateTime FINISH_AT =
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
    void finishInProgressSessionTransitionsToFinishedAndPreservesState() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        FinishResult result = finisher(FINISH_AT).finish("2026-09-10-mock-01-01");

        assertEquals(FinishResult.Outcome.FINISHED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("FINISHED", data.get("status"));
        assertEquals("2026-09-10-mock-01-01", data.get("sessionId"));
        assertEquals("mock-01", data.get("examId"));
        assertEquals(1, integer(data, "examVersion"));
        assertEquals("Q30", data.get("currentQuestion"));
        assertEquals(16, integer(data, "answeredQuestions"));
        assertEquals(2, integer(data, "skippedQuestions"));
        assertEquals(30, integer(data, "visitedQuestions"));
        assertEquals(4, integer(data, "flaggedQuestions"));
        assertEquals(STARTED_AT, data.get("startedAt"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("finishedAt"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("lastActivityAt"));
        assertEquals("B", questionOf(data, "Q01").get("answer"));
        assertEquals(Map.of("status", "VISITED", "flagged", Boolean.TRUE), questionsOf(data).get("Q30"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void finishAccountsElapsedTimeSinceLastActivity() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        FinishResult result = finisher(FINISH_AT).finish("2026-09-10-mock-01-01");

        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals(2700, integer(data, "elapsedSeconds"));
        assertEquals(4500, integer(data, "remainingSeconds"));
    }

    @Test
    void invariantElapsedPlusRemainingEqualsDurationIsKeptOnFinish() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        FinishResult result = finisher(FINISH_AT).finish("2026-09-10-mock-01-01");

        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals(7200, integer(data, "elapsedSeconds") + integer(data, "remainingSeconds"));
    }

    @Test
    void finishWhenTimeRunsOutEndsFinishedWithZeroRemaining() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 6900, 300);

        FinishResult result = finisher(FINISH_AT).finish("2026-09-10-mock-01-01");

        assertEquals(TIMED_OUT, result.outcome());
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
    void finishWhenTimeReachesExactZeroStillFinishes() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 6600, 600);

        FinishResult result = finisher(FINISH_AT).finish("2026-09-10-mock-01-01");

        assertEquals(TIMED_OUT, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("FINISHED", data.get("status"));
        assertEquals(0, integer(data, "remainingSeconds"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("finishedAt"));
        assertEquals("Q30", data.get("currentQuestion"));
    }

    @Test
    void finishPausedSessionIsRejectedAndFileUnchanged() throws IOException {
        String sessionId = "2026-09-10-mock-01-01";
        seed(sessionId, PAUSED, 2100, 5100);
        Path file = temp.resolve("sessions").resolve(sessionId + ".yaml");
        byte[] before = Files.readAllBytes(file);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> finisher(FINISH_AT).finish(sessionId));

        assertEquals(SESSION_NOT_FINISHABLE, e.kind());
        assertTrue(e.getMessage().contains("resume"));
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void finishAlreadyFinishedIsIdempotentAndFileUnchanged() throws IOException {
        String sessionId = "2026-09-10-mock-01-01";
        seed(sessionId, FINISHED, 2700, 4500, "2026-09-10T14:59:00-03:00");
        Path file = temp.resolve("sessions").resolve(sessionId + ".yaml");
        byte[] before = Files.readAllBytes(file);

        FinishResult result = finisher(FINISH_AT).finish(sessionId);

        assertEquals(ALREADY_FINISHED, result.outcome());
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void finishNonExistentSessionFailsExplicitlyAndCreatesNothing() {
        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> finisher(FINISH_AT).finish("2026-09-10-mock-99-01"));

        assertEquals(SESSION_NOT_FOUND, e.kind());
        assertTrue(e.getMessage().contains("2026-09-10-mock-99-01"));
        assertEquals(0, countSessionFiles());
    }

    @Test
    void finishNotStartedSessionFails() throws IOException {
        assertNotFinishable("2026-09-10-mock-01-01", NOT_STARTED);
    }

    @Test
    void finishGradedSessionFails() throws IOException {
        assertNotFinishable("2026-09-10-mock-01-01", GRADED);
    }

    @Test
    void finishAnalyzedSessionFails() throws IOException {
        assertNotFinishable("2026-09-10-mock-01-01", ANALYZED);
    }

    @Test
    void finishAbandonedSessionFails() throws IOException {
        assertNotFinishable("2026-09-10-mock-01-01", ABANDONED);
    }

    @Test
    void finishDoesNotRunGrade() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        FinishResult result = finisher(FINISH_AT).finish("2026-09-10-mock-01-01");

        assertEquals(FinishResult.Outcome.FINISHED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("FINISHED", data.get("status"));
        assertNull(data.get("result"));
    }

    @Test
    void finishPersistsOnlySessionFileAndNotProgress() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 2100, 5100);

        finisher(FINISH_AT).finish("2026-09-10-mock-01-01");

        try (Stream<Path> all = Files.walk(temp)) {
            List<String> persistence = all
                    .filter(Files::isRegularFile)
                    .map(path -> temp.relativize(path).toString())
                    .sorted()
                    .toList();
            assertEquals(List.of("sessions/2026-09-10-mock-01-01.yaml"), persistence);
        }
    }

    private void assertNotFinishable(String sessionId, ExamSessionStatus status) throws IOException {
        seed(sessionId, status, 2100, 5100);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> finisher(FINISH_AT).finish(sessionId));

        assertEquals(SESSION_NOT_FINISHABLE, e.kind());
        assertTrue(e.getMessage().contains(status.name()));
    }

    private ExamSessionFinisher finisher(OffsetDateTime now) {
        return new ExamSessionFinisher(temp, () -> now);
    }

    private void seed(String sessionId, ExamSessionStatus status, int elapsed, int remaining) throws IOException {
        seed(sessionId, status, elapsed, remaining, null);
    }

    private void seed(String sessionId, ExamSessionStatus status, int elapsed, int remaining,
            String finishedAt) throws IOException {
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        ExamSession session = new ExamSession(1, sessionId, "mock-01", 1, status,
                STARTED_AT, LAST_ACTIVITY_AT, finishedAt, "Q30",
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