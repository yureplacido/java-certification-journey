package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_VERSION_MISMATCH;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_ANALYZABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionStatus.ABANDONED;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.NOT_STARTED;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static com.placido.certification.exams.session.AnalysisResult.Outcome.ALREADY_ANALYZED;
import static com.placido.certification.exams.session.AnalysisResult.Outcome.ANALYZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExamSessionAnalyzerTest {

    private static final OffsetDateTime GRADE_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime ANALYZE_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 10, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime REANALYZE_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 20, 0, 0, ZoneOffset.ofHours(-3));

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

    private static final String SESSION_ID = "2026-09-10-mock-01-01";

    private static final Map<String, Map<String, Object>> ALL_CORRECT = Map.of(
            "Q01", Map.of("status", "ANSWERED", "answer", "B"),
            "Q02", Map.of("status", "ANSWERED", "answer", "D"),
            "Q03", Map.of("status", "ANSWERED", "answer", "B"),
            "Q04", Map.of("status", "ANSWERED", "answer", "C"));

    @TempDir
    Path temp;

    @Test
    void analyzeOfGradedSessionProducesAnalyzedAndArtifact() throws IOException {
        gradedWith(SESSION_ID, ALL_CORRECT);

        AnalysisResult result = analyzer(ANALYZE_AT).analyze(SESSION_ID);

        assertEquals(ANALYZED, result.outcome());
        assertEquals(SESSION_ID, result.sessionId());
        Map<String, Object> data = SessionYaml.parse(result.sessionFile());
        assertEquals("ANALYZED", data.get("status"));
        assertEquals("Q03", data.get("currentQuestion"));

        Map<String, Object> resultBlock = resultOf(data);
        assertEquals("2026-09-10T15:00:00-03:00", resultBlock.get("gradedAt"));
        assertEquals(4, num(resultBlock, "correct"));
        assertEquals(0, num(resultBlock, "wrong"));
        assertEquals(0, num(resultBlock, "unanswered"));
        assertEquals(100, num(resultBlock, "scorePercent"));
        assertEquals(Boolean.TRUE, resultBlock.get("passing"));

        assertTrue(Files.isRegularFile(result.artifactFile()));
        Map<String, Object> artifact = SessionYaml.parse(result.artifactFile());
        assertEquals(1, num(artifact, "schemaVersion"));
        assertEquals(SESSION_ID, artifact.get("sessionId"));
        assertEquals("mock-01", artifact.get("examId"));
        assertEquals(1, num(artifact, "examVersion"));
        assertEquals("2026-09-10T15:10:00-03:00", artifact.get("analyzedAt"));

        Map<String, Object> summary = cast(artifact.get("summary"));
        assertEquals(4, num(summary, "correct"));
        assertEquals(0, num(summary, "wrong"));
        assertEquals(0, num(summary, "unanswered"));
        assertEquals(100, num(summary, "scorePercent"));
        assertEquals(Boolean.TRUE, summary.get("passing"));
        assertEquals(List.of("Q01", "Q02", "Q03", "Q04"), ids(artifact.get("correctQuestions")));
        assertEquals(List.of(), ids(artifact.get("wrongQuestions")));
        assertEquals(List.of(), ids(artifact.get("unansweredQuestions")));

        Map<?, ?> bySection = cast(artifact.get("bySection"));
        assertEquals(2, num(cast(bySection.get(1)), "correct"));
        assertEquals(2, num(cast(bySection.get(1)), "total"));
        assertEquals(2, num(cast(bySection.get(2)), "correct"));
        assertEquals(2, num(cast(bySection.get(2)), "total"));

        Map<?, ?> byTopic = cast(artifact.get("byTopic"));
        assertEquals(2, num(cast(byTopic.get("language-basics")), "correct"));
        assertEquals(2, num(cast(byTopic.get("language-basics")), "total"));
        assertEquals(2, num(cast(byTopic.get("oop")), "correct"));
        assertEquals(2, num(cast(byTopic.get("oop")), "total"));

        assertFalse(artifact.containsKey("gaps"));
        assertFalse(artifact.containsKey("traps"));
        assertFalse(artifact.containsKey("byDifficulty"));
        assertFalse(artifact.containsKey("knowledgeGaps"));
    }

    @Test
    void wrongAnswersAreListedAsWrong() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> artifact = artifactOf();
        assertEquals(List.of("Q01", "Q03", "Q04"), ids(artifact.get("correctQuestions")));
        assertEquals(List.of("Q02"), ids(artifact.get("wrongQuestions")));
        assertEquals(List.of(), ids(artifact.get("unansweredQuestions")));
        assertEquals(1, num(cast(artifact.get("summary")), "wrong"));
    }

    @Test
    void invalidOptionCountsAsWrongNotUnanswered() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "Z"),
                "Q03", Map.of("status", "ANSWERED", "answer", "e"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> artifact = artifactOf();
        assertEquals(List.of("Q01", "Q04"), ids(artifact.get("correctQuestions")));
        assertEquals(List.of("Q02", "Q03"), ids(artifact.get("wrongQuestions")));
        assertEquals(2, num(cast(artifact.get("summary")), "wrong"));
        assertEquals(0, num(cast(artifact.get("summary")), "unanswered"));
    }

    @Test
    void answeredWithoutAnswerIsUnanswered() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED"),
                "Q02", Map.of("status", "ANSWERED", "answer", "D"),
                "Q03", Map.of("status", "VISITED"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> artifact = artifactOf();
        assertTrue(ids(artifact.get("unansweredQuestions")).contains("Q01"));
        assertEquals(2, num(cast(artifact.get("summary")), "unanswered"));
    }

    @Test
    void visitedQuestionIsUnanswered() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "VISITED"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> artifact = artifactOf();
        assertTrue(ids(artifact.get("unansweredQuestions")).contains("Q02"));
    }

    @Test
    void skippedQuestionIsUnanswered() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "D"),
                "Q03", Map.of("status", "SKIPPED"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> artifact = artifactOf();
        assertTrue(ids(artifact.get("unansweredQuestions")).contains("Q03"));
    }

    @Test
    void unvisitedQuestionIsUnanswered() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "D"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> artifact = artifactOf();
        assertTrue(ids(artifact.get("unansweredQuestions")).contains("Q04"));
    }

    @Test
    void listsRespectQuestionOrder() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "A"),
                "Q02", Map.of("status", "ANSWERED", "answer", "D"),
                "Q03", Map.of("status", "VISITED"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> artifact = artifactOf();
        assertEquals(List.of("Q02", "Q04"), ids(artifact.get("correctQuestions")));
        assertEquals(List.of("Q01"), ids(artifact.get("wrongQuestions")));
        assertEquals(List.of("Q03"), ids(artifact.get("unansweredQuestions")));
    }

    @Test
    void bySectionMatchesGradeResult() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "VISITED")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> data = SessionYaml.parse(fileOf(SESSION_ID));
        Map<String, Object> artifact = artifactOf();
        Map<?, ?> storedBySection = (Map<?, ?>) resultOf(data).get("bySection");
        Map<?, ?> artifactBySection = (Map<?, ?>) artifact.get("bySection");
        assertEquals(storedBySection, artifactBySection);
        assertEquals(1, num(cast(artifactBySection.get(1)), "correct"));
        assertEquals(2, num(cast(artifactBySection.get(1)), "total"));
        assertEquals(1, num(cast(artifactBySection.get(2)), "correct"));
        assertEquals(2, num(cast(artifactBySection.get(2)), "total"));
    }

    @Test
    void byTopicFollowsDefinitionTopics() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A"),
                "Q03", Map.of("status", "ANSWERED", "answer", "B"),
                "Q04", Map.of("status", "VISITED")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<?, ?> byTopic = cast(artifactOf().get("byTopic"));
        assertEquals(1, num(cast(byTopic.get("language-basics")), "correct"));
        assertEquals(2, num(cast(byTopic.get("language-basics")), "total"));
        assertEquals(1, num(cast(byTopic.get("oop")), "correct"));
        assertEquals(2, num(cast(byTopic.get("oop")), "total"));
    }

    @Test
    void artifactSummaryIsConsistentWithSessionResult() throws IOException {
        gradedWith(SESSION_ID, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A"),
                "Q03", Map.of("status", "VISITED"),
                "Q04", Map.of("status", "ANSWERED", "answer", "C")));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> resultBlock = resultOf(SessionYaml.parse(fileOf(SESSION_ID)));
        Map<String, Object> summary = cast(artifactOf().get("summary"));
        assertEquals(num(resultBlock, "correct"), num(summary, "correct"));
        assertEquals(num(resultBlock, "wrong"), num(summary, "wrong"));
        assertEquals(num(resultBlock, "unanswered"), num(summary, "unanswered"));
        assertEquals(num(resultBlock, "scorePercent"), num(summary, "scorePercent"));
        assertEquals(resultBlock.get("passing"), summary.get("passing"));
    }

    @Test
    void analyzePreservesSessionDataExceptStatus() throws IOException {
        gradedWith(SESSION_ID, ALL_CORRECT);
        Map<String, Object> before = SessionYaml.parse(fileOf(SESSION_ID));

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        Map<String, Object> after = SessionYaml.parse(fileOf(SESSION_ID));
        assertEquals("ANALYZED", after.get("status"));
        assertEquals(before.get("questions"), after.get("questions"));
        assertEquals(before.get("result"), after.get("result"));
        assertEquals(before.get("currentQuestion"), after.get("currentQuestion"));
        assertEquals(before.get("lastActivityAt"), after.get("lastActivityAt"));
        assertEquals(before.get("startedAt"), after.get("startedAt"));
        assertEquals(before.get("finishedAt"), after.get("finishedAt"));
        assertEquals(before.get("elapsedSeconds"), after.get("elapsedSeconds"));
        assertEquals(before.get("remainingSeconds"), after.get("remainingSeconds"));
        assertEquals(before.get("answeredQuestions"), after.get("answeredQuestions"));
        assertEquals(before.get("skippedQuestions"), after.get("skippedQuestions"));
        assertEquals(before.get("visitedQuestions"), after.get("visitedQuestions"));
        assertEquals(before.get("flaggedQuestions"), after.get("flaggedQuestions"));
        assertEquals(before.get("totalQuestions"), after.get("totalQuestions"));
        assertEquals(before.get("tracker"), after.get("tracker"));
    }

    @Test
    void reAnalyzeOnAnalyzedSessionIsIdempotentAndOverwritesArtifact() throws IOException {
        gradedWith(SESSION_ID, ALL_CORRECT);
        ExamSessionAnalyzer analyzer = analyzer(ANALYZE_AT);

        AnalysisResult first = analyzer.analyze(SESSION_ID);
        Map<String, Object> firstArtifact = SessionYaml.parse(first.artifactFile());

        AnalysisResult second = new ExamSessionAnalyzer(temp, () -> REANALYZE_AT).analyze(SESSION_ID);

        assertEquals(ALREADY_ANALYZED, second.outcome());
        assertEquals(first.artifactFile(), second.artifactFile());
        assertEquals(1, countArtifactFiles());
        assertEquals("ANALYZED", SessionYaml.parse(fileOf(SESSION_ID)).get("status"));

        Map<String, Object> secondArtifact = SessionYaml.parse(second.artifactFile());
        assertEquals("2026-09-10T15:20:00-03:00", secondArtifact.get("analyzedAt"));
        assertEquals("2026-09-10T15:10:00-03:00", firstArtifact.get("analyzedAt"));
        Map<String, Object> firstWithoutTime = copyWithout(firstArtifact, "analyzedAt");
        Map<String, Object> secondWithoutTime = copyWithout(secondArtifact, "analyzedAt");
        assertEquals(firstWithoutTime, secondWithoutTime);
        assertEquals(4, num(cast(secondArtifact.get("summary")), "correct"));
    }

    @Test
    void analyzeOfNotStartedSessionIsRejected() throws IOException {
        assertNotAnalyzable(NOT_STARTED);
    }

    @Test
    void analyzeOfInProgressSessionIsRejected() throws IOException {
        assertNotAnalyzable(IN_PROGRESS);
    }

    @Test
    void analyzeOfPausedSessionIsRejected() throws IOException {
        assertNotAnalyzable(PAUSED);
    }

    @Test
    void analyzeOfFinishedSessionIsRejected() throws IOException {
        assertNotAnalyzable(FINISHED);
    }

    @Test
    void analyzeOfAbandonedSessionIsRejected() throws IOException {
        assertNotAnalyzable(ABANDONED);
    }

    @Test
    void analyzeOfNonExistentSessionSignalsNotFound() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze("2026-09-10-mock-99-01"));

        assertEquals(SESSION_NOT_FOUND, e.kind());
        assertTrue(e.getMessage().contains("2026-09-10-mock-99-01"));
        assertEquals(0, countArtifactFiles());
    }

    @Test
    void analyzeWithoutDefinitionSignalsNotFound() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        Files.delete(temp.resolve("mock-01").resolve("definition.yaml"));
        seed(SESSION_ID, GRADED, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze(SESSION_ID));

        assertEquals(EXAM_DEFINITION_NOT_FOUND, e.kind());
    }

    @Test
    void analyzeWithInvalidDefinitionSignalsInvalid() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("examId: mock-01", "examId: mock-02"));
        seed(SESSION_ID, GRADED, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze(SESSION_ID));

        assertEquals(EXAM_DEFINITION_INVALID, e.kind());
    }

    @Test
    void analyzeWithVersionMismatchSignalsMismatch() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION.replace("version: 1", "version: 2"));
        seed(SESSION_ID, GRADED, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze(SESSION_ID));

        assertEquals(EXAM_DEFINITION_VERSION_MISMATCH, e.kind());
    }

    @Test
    void analyzeWithMissingResultSignalsInvalidStore() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed(SESSION_ID, GRADED, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze(SESSION_ID));

        assertEquals(SESSION_STORE_INVALID, e.kind());
        assertTrue(e.getMessage().contains("result"));
        assertEquals(0, countArtifactFiles());
    }

    @Test
    void analyzeWithInconsistentResultSignalsInvalidStore() throws IOException {
        gradedWith(SESSION_ID, ALL_CORRECT);
        tamperResult(SESSION_ID, "correct", 99);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze(SESSION_ID));

        assertEquals(SESSION_STORE_INVALID, e.kind());
        assertTrue(e.getMessage().contains("correct"));
        assertEquals(0, countArtifactFiles());
        assertEquals("GRADED", SessionYaml.parse(fileOf(SESSION_ID)).get("status"));
    }

    @Test
    void analyzeWithCorruptedSessionYamlSignalsInvalidStore() throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        Files.createDirectories(temp.resolve("sessions"));
        Files.writeString(temp.resolve("sessions").resolve(SESSION_ID + ".yaml"), "a: [\ninvalid");

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze(SESSION_ID));

        assertEquals(SESSION_STORE_INVALID, e.kind());
    }

    @Test
    void analyzeOnlyCreatesSessionAndArtifactFiles() throws IOException {
        gradedWith(SESSION_ID, ALL_CORRECT);

        analyzer(ANALYZE_AT).analyze(SESSION_ID);

        List<String> files = relativeFiles();
        assertEquals(List.of("docs/study-log/" + SESSION_ID + ".analysis.yaml",
                "mock-01/definition.yaml",
                "sessions/" + SESSION_ID + ".yaml"), files);
    }

    private void assertNotAnalyzable(ExamSessionStatus status) throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed(SESSION_ID, status, ALL_CORRECT, 4);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> analyzer(ANALYZE_AT).analyze(SESSION_ID));

        assertEquals(SESSION_NOT_ANALYZABLE, e.kind());
        assertTrue(e.getMessage().contains(status.name()));
        assertEquals(0, countArtifactFiles());
    }

    private void gradedWith(String sessionId, Map<String, Map<String, Object>> questions) throws IOException {
        writeDefinition("mock-01", VALID_DEFINITION);
        seed(sessionId, FINISHED, questions, 4);
        new ExamSessionGrader(temp, () -> GRADE_AT).grade(sessionId);
    }

    private ExamSessionAnalyzer analyzer(OffsetDateTime now) {
        return new ExamSessionAnalyzer(temp, () -> now);
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

    private void tamperResult(String sessionId, String key, Object value) throws IOException {
        Path file = fileOf(sessionId);
        Map<String, Object> data = SessionYaml.parse(file);
        ((Map<String, Object>) data.get("result")).put(key, value);
        Files.writeString(file, SessionYaml.dump(data));
    }

    private Map<String, Object> artifactOf() throws IOException {
        return SessionYaml.parse(artifactFile());
    }

    private Path artifactFile() {
        return temp.resolve("docs").resolve("study-log").resolve(SESSION_ID + ".analysis.yaml");
    }

    private long countArtifactFiles() throws IOException {
        Path studyLog = temp.resolve("docs").resolve("study-log");
        if (!Files.isDirectory(studyLog)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(studyLog)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".analysis.yaml")).count();
        }
    }

    private List<String> relativeFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(temp)) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> temp.relativize(file).toString())
                    .sorted()
                    .toList();
        }
    }

    private static Map<String, Object> copyWithout(Map<String, Object> source, String key) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.remove(key);
        return copy;
    }

    private Path fileOf(String sessionId) {
        return temp.resolve("sessions").resolve(sessionId + ".yaml");
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

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resultOf(Map<String, Object> data) {
        return (Map<String, Object>) data.get("result");
    }

    private static List<String> ids(Object raw) {
        List<String> ids = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            ids.add(String.valueOf(item));
        }
        return ids;
    }

    private static int num(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }
}