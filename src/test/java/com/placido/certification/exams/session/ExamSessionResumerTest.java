package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_RESUMABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionStatus.ABANDONED;
import static com.placido.certification.exams.session.ExamSessionStatus.ANALYZED;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.NOT_STARTED;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static com.placido.certification.exams.session.ResumeResult.Outcome.ALREADY_ACTIVE;
import static com.placido.certification.exams.session.ResumeResult.Outcome.EXPIRED;
import static com.placido.certification.exams.session.ResumeResult.Outcome.RESUMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExamSessionResumerTest {

    private static final OffsetDateTime RESUME_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime EXPIRED_AT =
            OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, ZoneOffset.ofHours(-3));

    private static final String STARTED_AT = "2026-09-10T14:30:00-03:00";
    private static final String LAST_ACTIVITY_AT = "2026-09-10T14:50:00-03:00";

    private static final Map<String, Map<String, Object>> QUESTION_STATE = Map.of(
            "Q01", Map.of("status", "ANSWERED", "answer", "B"),
            "Q30", Map.of("status", "VISITED", "flagged", Boolean.TRUE));

    @TempDir
    Path temp;

    @Test
    void resumeOfPausedSessionSetsInProgressAndPreservesState() throws IOException {
        seed("2026-09-10-mock-01-01", PAUSED);

        ResumeResult result = resumer(RESUME_AT).resume("2026-09-10-mock-01-01");

        assertEquals(RESUMED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("IN_PROGRESS", data.get("status"));
        assertEquals("2026-09-10-mock-01-01", data.get("sessionId"));
        assertEquals("mock-01", data.get("examId"));
        assertEquals(1, integer(data, "examVersion"));
        assertEquals("Q30", data.get("currentQuestion"));
        assertEquals(2100, integer(data, "elapsedSeconds"));
        assertEquals(5100, integer(data, "remainingSeconds"));
        assertEquals(16, integer(data, "answeredQuestions"));
        assertEquals(2, integer(data, "skippedQuestions"));
        assertEquals(30, integer(data, "visitedQuestions"));
        assertEquals(4, integer(data, "flaggedQuestions"));
        assertEquals(STARTED_AT, data.get("startedAt"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("lastActivityAt"));
        assertTrue(data.get("finishedAt") == null);
        Map<?, ?> questions = (Map<?, ?>) data.get("questions");
        @SuppressWarnings("unchecked")
        Map<String, Object> q01 = (Map<String, Object>) questions.get("Q01");
        assertEquals("ANSWERED", q01.get("status"));
        assertEquals("B", q01.get("answer"));
        assertEquals(Map.of("status", "VISITED", "flagged", Boolean.TRUE), questions.get("Q30"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void pausedTimeIsFrozenResumeDoesNotConsumeSeconds() throws IOException {
        seed("2026-09-10-mock-01-01", PAUSED);

        ResumeResult result = resumer(EXPIRED_AT).resume("2026-09-10-mock-01-01");

        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("IN_PROGRESS", data.get("status"));
        assertEquals(5100, integer(data, "remainingSeconds"));
        assertEquals(2100, integer(data, "elapsedSeconds"));
    }

    @Test
    void resumeOfInProgressSessionKeepsItActiveAndConsumesElapsedGap() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS);

        ResumeResult result = resumer(RESUME_AT).resume("2026-09-10-mock-01-01");

        assertEquals(ALREADY_ACTIVE, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("IN_PROGRESS", data.get("status"));
        assertEquals("Q30", data.get("currentQuestion"));
        assertEquals(2700, integer(data, "elapsedSeconds"));
        assertEquals(4500, integer(data, "remainingSeconds"));
        assertEquals(7200, integer(data, "elapsedSeconds") + integer(data, "remainingSeconds"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void repeatedResumeOnInProgressIsIdempotent() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS);
        ExamSessionResumer resumer = resumer(RESUME_AT);

        ResumeResult first = resumer.resume("2026-09-10-mock-01-01");
        ResumeResult second = resumer.resume("2026-09-10-mock-01-01");

        assertEquals(ALREADY_ACTIVE, first.outcome());
        assertEquals(ALREADY_ACTIVE, second.outcome());
        assertEquals(first.sessionId(), second.sessionId());
        Map<String, Object> data = SessionYaml.parse(second.sessionFile());
        assertEquals("Q30", data.get("currentQuestion"));
        assertEquals("B", questionOf(data, "Q01").get("answer"));
        assertEquals(2700, integer(data, "elapsedSeconds"));
        assertEquals(4500, integer(data, "remainingSeconds"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void resumeOfExpiredPausedSessionGoesToFinished() throws IOException {
        seed("2026-09-10-mock-01-01", PAUSED, 7200, 0);

        ResumeResult result = resumer(RESUME_AT).resume("2026-09-10-mock-01-01");

        assertEquals(EXPIRED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("FINISHED", data.get("status"));
        assertEquals(0, integer(data, "remainingSeconds"));
        assertEquals(7200, integer(data, "elapsedSeconds"));
        assertEquals("2026-09-10T15:00:00-03:00", data.get("finishedAt"));
        assertEquals("B", questionOf(data, "Q01").get("answer"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void resumeOfExpiredInProgressSessionGoesToFinished() throws IOException {
        seed("2026-09-10-mock-01-01", IN_PROGRESS, 7100, 100);

        ResumeResult result = resumer(EXPIRED_AT).resume("2026-09-10-mock-01-01");

        assertEquals(EXPIRED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("FINISHED", data.get("status"));
        assertEquals(0, integer(data, "remainingSeconds"));
        assertEquals(7200, integer(data, "elapsedSeconds"));
        assertEquals("2026-09-10T16:00:00-03:00", data.get("finishedAt"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void resumeOfNonExistentSessionFailsExplicitlyAndCreatesNothing() {
        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> resumer(RESUME_AT).resume("2026-09-10-mock-99-01"));

        assertEquals(SESSION_NOT_FOUND, e.kind());
        assertTrue(e.getMessage().contains("2026-09-10-mock-99-01"));
    }

    @Test
    void resumeOfNotStartedSessionFails() throws IOException {
        assertNotResumable("2026-09-10-mock-01-01", NOT_STARTED);
    }

    @Test
    void resumeOfFinishedSessionFails() throws IOException {
        assertNotResumable("2026-09-10-mock-01-01", FINISHED);
    }

    @Test
    void resumeOfGradedSessionFails() throws IOException {
        assertNotResumable("2026-09-10-mock-01-01", GRADED);
    }

    @Test
    void resumeOfAnalyzedSessionFails() throws IOException {
        assertNotResumable("2026-09-10-mock-01-01", ANALYZED);
    }

    @Test
    void resumeOfAbandonedSessionFails() throws IOException {
        assertNotResumable("2026-09-10-mock-01-01", ABANDONED);
    }

    @Test
    void resumeRejectsNonCanonicalCurrentQuestion() throws IOException {
        String questionId = "2026-09-10-mock-01-01";
        seed(questionId, PAUSED, 2100, 5100, Map.of("currentQuestion", "1"));

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> resumer(RESUME_AT).resume(questionId));

        assertEquals(SESSION_STORE_INVALID, e.kind());
    }

    @Test
    void resumeWithFileNamedAfterAnotherSessionIdFails() throws IOException {
        Path sessions = sessionsDir();
        Files.writeString(sessions.resolve("2026-09-10-mock-01-01.yaml"),
                session("2026-09-10-mock-01-02", PAUSED, 2100, 5100, Map.of()).toYamlText());

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> resumer(RESUME_AT).resume("2026-09-10-mock-01-01"));

        assertEquals(SESSION_STORE_INVALID, e.kind());
    }

    private void assertNotResumable(String sessionId, ExamSessionStatus status) throws IOException {
        seed(sessionId, status);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> resumer(RESUME_AT).resume(sessionId));

        assertEquals(SESSION_NOT_RESUMABLE, e.kind());
        assertTrue(e.getMessage().contains(status.name()));
    }

    private ExamSessionResumer resumer(OffsetDateTime now) {
        return new ExamSessionResumer(temp, () -> now);
    }

    private void seed(String sessionId, ExamSessionStatus status) throws IOException {
        seed(sessionId, status, 2100, 5100);
    }

    private void seed(String sessionId, ExamSessionStatus status, int elapsed, int remaining) throws IOException {
        seed(sessionId, status, elapsed, remaining, Map.of());
    }

    private void seed(String sessionId, ExamSessionStatus status, int elapsed, int remaining,
            Map<String, Object> overrides) throws IOException {
        Path sessions = sessionsDir();
        ExamSession session = session(sessionId, status, elapsed, remaining, overrides);
        Files.writeString(sessions.resolve(sessionId + ".yaml"), session.toYamlText());
    }

    private ExamSession session(String sessionId, ExamSessionStatus status, int elapsed, int remaining,
            Map<String, Object> overrides) {
        String currentQuestion = overrides.getOrDefault("currentQuestion", "Q30").toString();
        Map<String, Map<String, Object>> questions = overrides.containsKey("questions")
                ? castMap(overrides.get("questions"))
                : QUESTION_STATE;
        return new ExamSession(1, sessionId, "mock-01", 1, status,
                STARTED_AT, LAST_ACTIVITY_AT, null, currentQuestion,
                elapsed, remaining, 50, 16, 2, 30, 4, questions);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> castMap(Object value) {
        return (Map<String, Map<String, Object>>) value;
    }

    private Path sessionsDir() throws IOException {
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        return sessions;
    }

    private long countSessionFiles() throws IOException {
        Path sessions = temp.resolve("sessions");
        if (!Files.isDirectory(sessions)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(sessions)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".yaml")).count();
        }
    }

    private static Map<String, Object> questionOf(Map<String, Object> data, String id) {
        @SuppressWarnings("unchecked")
        Map<String, Object> question = (Map<String, Object>) ((Map<?, ?>) data.get("questions")).get(id);
        return question;
    }

    private static int integer(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).intValue();
    }
}