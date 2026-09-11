package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_VERSION_MISMATCH;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_GRADABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionStatus.ABANDONED;
import static com.placido.certification.exams.session.ExamSessionStatus.ANALYZED;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.NOT_STARTED;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static com.placido.certification.exams.session.GradeResult.Outcome.ALREADY_GRADED;
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

class ExamSessionGraderTest {

    private static final OffsetDateTime GRADE_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-3));

    private static final String STARTED_AT = "2026-09-10T14:30:00-03:00";
    private static final String LAST_ACTIVITY_AT = "2026-09-10T14:50:00-03:00";
    private static final String FINISHED_AT = "2026-09-10T15:05:00-03:00";

    private static final String VALID_DEFINITION = """
            examId: mock-01
            version: 1
            examType: mock
            javaVersion: "21"
            totalQuestions: 4
            durationSeconds: 7200
            passingScore: 68
            questionOrder: [Q01, Q02, Q03, Q04]
            questions:
              Q01: { topic: language-basics, section: 1, difficulty: medium }
              Q02: { topic: language-basics, section: 1, difficulty: medium }
              Q03: { topic: oop, section: 2, difficulty: hard }
              Q04: { topic: oop, section: 2, difficulty: hard }
            answers:
              Q01: { correctOption: B, format: single-choice }
              Q02: { correctOption: D, format: single-choice }
              Q03: { correctOption: B, format: single-choice }
              Q04: { correctOption: C, format: single-choice }
            """;

    private static final Map<String, Map<String, Object>> ALL_CORRECT = Map.of(
            "Q01", Map.of("status", "ANSWERED", "answer", "B"),
            "Q02", Map.of("status", "ANSWERED", "answer", "D"),
            "Q03", Map.of("status", "ANSWERED", "answer", "B"),
            "Q04", Map.of("status", "ANSWERED", "answer", "C"));

    @TempDir
    Path temp;

    @Test
    void gradeFinishedSessionWithCorrectAnswersComputesResult() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, ALL_CORRECT, 4);

        GradeResult result = grader().grade("2026-09-10-mock-01-01");

        assertEquals(GradeResult.Outcome.GRADED, result.outcome());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("GRADED", data.get("status"));
        assertEquals("2026-09-10-mock-01-01", data.get("sessionId"));
        assertEquals("mock-01", data.get("examId"));
        assertEquals(1, integer(data, "examVersion"));
        assertEquals("Q03", data.get("currentQuestion"));
        Map<String, Object> r = resultOf(data);
        assertEquals("2026-09-10T15:00:00-03:00", r.get("gradedAt"));
        assertEquals(4, num(r, "correct"));
        assertEquals(0, num(r, "wrong"));
        assertEquals(0, num(r, "unanswered"));
        assertEquals(100, num(r, "scorePercent"));
        assertEquals(Boolean.TRUE, r.get("passing"));
    }

    @Test
    void gradeCountsWrongAnswers() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")), 4);

        grader().grade("2026-09-10-mock-01-01");

        Map<String, Object> r = resultOf(parseSession());
        assertEquals(3, num(r, "correct"));
        assertEquals(1, num(r, "wrong"));
        assertEquals(0, num(r, "unanswered"));
        assertEquals(75, num(r, "scorePercent"));
    }

    @Test
    void gradeCountsUnansweredQuestions() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "VISITED"),
                "Q03", Map.of("status", "SKIPPED")), 4);

        grader().grade("2026-09-10-mock-01-01");

        Map<String, Object> r = resultOf(parseSession());
        assertEquals(1, num(r, "correct"));
        assertEquals(0, num(r, "wrong"));
        assertEquals(3, num(r, "unanswered"));
        assertEquals(25, num(r, "scorePercent"));
        assertEquals(Boolean.FALSE, r.get("passing"));
    }

    @Test
    void invalidOptionCountsAsWrongNotUnanswered() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "Z"),
                "Q03", Map.of("status", "ANSWERED", "answer", "e"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")), 4);

        grader().grade("2026-09-10-mock-01-01");

        Map<String, Object> r = resultOf(parseSession());
        assertEquals(2, num(r, "correct"));
        assertEquals(2, num(r, "wrong"));
        assertEquals(0, num(r, "unanswered"));
    }

    @Test
    void answeredWithoutAnswerCountsAsUnanswered() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED"),
                "Q02", Map.of("status", "ANSWERED", "answer", "D"),
                "Q03", Map.of("status", "VISITED"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")), 4);

        grader().grade("2026-09-10-mock-01-01");

        Map<String, Object> r = resultOf(parseSession());
        assertEquals(2, num(r, "correct"));
        assertEquals(0, num(r, "wrong"));
        assertEquals(2, num(r, "unanswered"));
    }

    @Test
    void scorePercentFollowsRoundRule() throws IOException {
        writeDefinition("mock-01", """
                examId: mock-01
                version: 1
                examType: mock
                javaVersion: "21"
                totalQuestions: 3
                durationSeconds: 7200
                passingScore: 68
                questionOrder: [Q01, Q02, Q03]
                questions:
                  Q01: { topic: language-basics, section: 1, difficulty: medium }
                  Q02: { topic: language-basics, section: 1, difficulty: medium }
                  Q03: { topic: oop, section: 2, difficulty: hard }
                answers:
                  Q01: { correctOption: B, format: single-choice }
                  Q02: { correctOption: D, format: single-choice }
                  Q03: { correctOption: B, format: single-choice }
                """);
        seed("2026-09-10-mock-01-01", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "D"),
                "Q03", Map.of("status", "VISITED")), 3);

        grader().grade("2026-09-10-mock-01-01");

        Map<String, Object> r = resultOf(parseSession());
        assertEquals(2, num(r, "correct"));
        assertEquals(67, num(r, "scorePercent"));
        assertEquals(Boolean.FALSE, r.get("passing"));
    }

    @Test
    void passingUsesPassingScoreFromDefinition() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")), 4);
        seed("2026-09-10-mock-01-02", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "VISITED"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "VISITED")), 4);

        grader().grade("2026-09-10-mock-01-01");
        grader().grade("2026-09-10-mock-01-02");

        assertEquals(Boolean.TRUE, resultOf(SessionYaml.parse(fileOf("2026-09-10-mock-01-01"))).get("passing"));
        assertEquals(Boolean.FALSE, resultOf(SessionYaml.parse(fileOf("2026-09-10-mock-01-02"))).get("passing"));
    }

    @Test
    void bySectionComputedPerDefinitionSections() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "VISITED")), 4);

        grader().grade("2026-09-10-mock-01-01");

        Map<?, ?> bySection = (Map<?, ?>) resultOf(parseSession()).get("bySection");
        Map<String, Object> section1 = castSection(bySection.get(1));
        Map<String, Object> section2 = castSection(bySection.get(2));
        assertEquals(1, num(section1, "correct"));
        assertEquals(2, num(section1, "total"));
        assertEquals(1, num(section2, "correct"));
        assertEquals(2, num(section2, "total"));
    }

    @Test
    void gradeOfPausedSessionIsRejected() throws IOException {
        assertNotGradable(PAUSED);
    }

    @Test
    void gradeOfInProgressSessionIsRejected() throws IOException {
        assertNotGradable(IN_PROGRESS);
    }

    @Test
    void gradeOfNotStartedSessionIsRejected() throws IOException {
        assertNotGradable(NOT_STARTED);
    }

    @Test
    void gradeOfAnalyzedSessionIsRejected() throws IOException {
        assertNotGradable(ANALYZED);
    }

    @Test
    void gradeOfAbandonedSessionIsRejected() throws IOException {
        assertNotGradable(ABANDONED);
    }

    @Test
    void gradeOfNonExistentSessionFailsAndCreatesNothing() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> grader().grade("2026-09-10-mock-99-01"));

        assertEquals(SESSION_NOT_FOUND, e.kind());
        assertTrue(e.getMessage().contains("2026-09-10-mock-99-01"));
        assertEquals(0, countSessionFiles());
    }

    @Test
    void gradeWithoutDefinitionSignalsNotFound() throws IOException {
        seed("2026-09-10-mock-01-01", FINISHED, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> grader().grade("2026-09-10-mock-01-01"));

        assertEquals(EXAM_DEFINITION_NOT_FOUND, e.kind());
    }

    @Test
    void gradeWithInvalidDefinitionSignalsInvalid() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("examId: mock-01", "examId: mock-02"));
        seed("2026-09-10-mock-01-01", FINISHED, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> grader().grade("2026-09-10-mock-01-01"));

        assertEquals(EXAM_DEFINITION_INVALID, e.kind());
    }

    @Test
    void gradeWithVersionMismatchSignalsMismatch() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("version: 1", "version: 2"));
        seed("2026-09-10-mock-01-01", FINISHED, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> grader().grade("2026-09-10-mock-01-01"));

        assertEquals(EXAM_DEFINITION_VERSION_MISMATCH, e.kind());
    }

    @Test
    void gradeWithCorruptedSessionYamlSignalsInvalidStore() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        Files.writeString(sessions.resolve("2026-09-10-mock-01-01.yaml"), "a: [\ninvalid");

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> grader().grade("2026-09-10-mock-01-01"));

        assertEquals(SESSION_STORE_INVALID, e.kind());
    }

    @Test
    void repeatedGradeOnGradedSessionIsIdempotent() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, ALL_CORRECT, 4);
        ExamSessionGrader grader = grader();

        GradeResult first = grader.grade("2026-09-10-mock-01-01");
        GradeResult second = grader.grade("2026-09-10-mock-01-01");

        assertEquals(GradeResult.Outcome.GRADED, first.outcome());
        assertEquals(ALREADY_GRADED, second.outcome());
        assertEquals(first.sessionId(), second.sessionId());
        Map<String, Object> data = SessionYaml.parse(second.sessionFile());
        assertEquals("GRADED", data.get("status"));
        Map<String, Object> r = resultOf(data);
        assertEquals(4, num(r, "correct"));
        assertEquals(0, num(r, "wrong"));
        assertEquals(0, num(r, "unanswered"));
        assertEquals(100, num(r, "scorePercent"));
        assertEquals(Boolean.TRUE, r.get("passing"));
        assertEquals(1, countSessionFiles());
    }

    @Test
    void regradeRecomputesAcademicResultInsteadOfTrustingStoredBlock() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seedGradedWithTaintedResult("2026-09-10-mock-01-01");

        GradeResult result = grader().grade("2026-09-10-mock-01-01");

        assertEquals(ALREADY_GRADED, result.outcome());
        Map<String, Object> r = resultOf(parseSession());
        assertEquals(4, num(r, "correct"));
        assertEquals(0, num(r, "wrong"));
        assertEquals(0, num(r, "unanswered"));
        assertEquals(100, num(r, "scorePercent"));
        assertEquals(Boolean.TRUE, r.get("passing"));
        assertEquals("2026-09-10T15:00:00-03:00", r.get("gradedAt"));
    }

    @Test
    void gradeDoesNotModifyQuestionsOrCounters() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed("2026-09-10-mock-01-01", FINISHED, ALL_CORRECT, 4);
        Map<String, Object> before = SessionYaml.parse(fileOf("2026-09-10-mock-01-01"));
        Object questionsBefore = before.get("questions");

        grader().grade("2026-09-10-mock-01-01");

        Map<String, Object> after = SessionYaml.parse(fileOf("2026-09-10-mock-01-01"));
        assertEquals(questionsBefore, after.get("questions"));
        assertEquals(4, integer(after, "answeredQuestions"));
        assertEquals(0, integer(after, "skippedQuestions"));
        assertEquals(4, integer(after, "visitedQuestions"));
        assertEquals(0, integer(after, "flaggedQuestions"));
        assertEquals(4, integer(after, "totalQuestions"));
        assertEquals(2700, integer(after, "elapsedSeconds"));
        assertEquals(4500, integer(after, "remainingSeconds"));
        assertEquals("Q03", after.get("currentQuestion"));
    }

    private void assertNotGradable(ExamSessionStatus status) throws IOException {
        String sessionId = "2026-09-10-mock-01-01";
        writeDefinition("mock-01", VALID_DEFINITION);
        seed(sessionId, status, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> grader().grade(sessionId));

        assertEquals(SESSION_NOT_GRADABLE, e.kind());
        assertTrue(e.getMessage().contains(status.name()));
    }

    private ExamSessionGrader grader() {
        return new ExamSessionGrader(temp, () -> GRADE_AT);
    }

    private void writeDefinition(String examId, String content) throws IOException {
        Path dir = temp.resolve(examId);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("definition.yaml"), content);
    }

    private void seed(String sessionId, ExamSessionStatus status,
            Map<String, Map<String, Object>> questions, int totalQuestions) throws IOException {
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        ExamSession session = new ExamSession(1, sessionId, "mock-01", 1, status,
                STARTED_AT, LAST_ACTIVITY_AT, FINISHED_AT, "Q03",
                2700, 4500, totalQuestions,
                count(questions, "ANSWERED"), count(questions, "SKIPPED"),
                questions.size(), countFlagged(questions), questions);
        Files.writeString(sessions.resolve(sessionId + ".yaml"), session.toYamlText());
    }

    private void seedGradedWithTaintedResult(String sessionId) throws IOException {
        Path sessions = temp.resolve("sessions");
        Files.createDirectories(sessions);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("schemaVersion", 1);
        data.put("sessionId", sessionId);
        data.put("examId", "mock-01");
        data.put("examVersion", 1);
        data.put("status", "GRADED");
        data.put("startedAt", STARTED_AT);
        data.put("lastActivityAt", LAST_ACTIVITY_AT);
        data.put("finishedAt", FINISHED_AT);
        data.put("currentQuestion", "Q03");
        data.put("elapsedSeconds", 2700);
        data.put("remainingSeconds", 4500);
        data.put("totalQuestions", 4);
        data.put("answeredQuestions", 4);
        data.put("skippedQuestions", 0);
        data.put("visitedQuestions", 4);
        data.put("flaggedQuestions", 0);
        data.put("questions", ALL_CORRECT);
        Map<String, Object> tainted = new LinkedHashMap<>();
        tainted.put("gradedAt", "2026-09-10T14:00:00-03:00");
        tainted.put("correct", 99);
        tainted.put("wrong", 0);
        tainted.put("unanswered", 0);
        tainted.put("scorePercent", 99);
        tainted.put("passing", Boolean.TRUE);
        tainted.put("bySection", new LinkedHashMap<>());
        data.put("result", tainted);
        Files.writeString(sessions.resolve(sessionId + ".yaml"), SessionYaml.dump(data));
    }

    private static int count(Map<String, Map<String, Object>> questions, String status) {
        return (int) questions.values().stream()
                .filter(q -> status.equals(q.get("status")))
                .count();
    }

    private static int countFlagged(Map<String, Map<String, Object>> questions) {
        return (int) questions.values().stream()
                .filter(q -> Boolean.TRUE.equals(q.get("flagged")))
                .count();
    }

    private Path fileOf(String sessionId) {
        return temp.resolve("sessions").resolve(sessionId + ".yaml");
    }

    private Map<String, Object> parseSession() throws IOException {
        return SessionYaml.parse(fileOf("2026-09-10-mock-01-01"));
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resultOf(Map<String, Object> data) {
        return (Map<String, Object>) data.get("result");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castSection(Object value) {
        return (Map<String, Object>) value;
    }

    private static int num(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }

    private static int integer(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).intValue();
    }
}