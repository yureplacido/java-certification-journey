package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.AnswerResult.Outcome.ANSWERED;
import static com.placido.certification.exams.session.AnswerResult.Outcome.UNCHANGED;
import static com.placido.certification.exams.session.AnswerResult.Outcome.UPDATED;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.INVALID_ANSWER_OPTION;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.QUESTION_NOT_IN_EXAM;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_ANSWERABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExamSessionAnswererTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final String SESSION_ID = "2026-09-10-mock-01-01";
    private static final String STARTED_AT = "2026-09-10T14:30:00-03:00";
    private static final String LAST_ACTIVITY_AT = "2026-09-10T14:50:00-03:00";
    private static final String UPDATE_AT = "2026-09-10T15:00:00-03:00";

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

    private static final String DEFINITION_WITH_OPTIONS = """
            examId: mock-01
            version: 1
            examType: mock
            javaVersion: "21"
            totalQuestions: 2
            durationSeconds: 7200
            passingScore: 68
            questionOrder: [Q01, Q02]
            questions:
              Q01: { topic: language-basics, section: 1, difficulty: medium }
              Q02: { topic: oop, section: 2, difficulty: hard }
            answers:
              Q01: { correctOption: B, format: single-choice }
              Q02: { correctOption: Y, format: single-choice, options: [X, Y, Z] }
            """;

    @TempDir
    Path temp;

    @Test
    void answerOnVisitedQuestionMarksAnsweredAndUpdatesCounters() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of(
                "Q01", Map.of("status", "VISITED"),
                "Q02", Map.of("status", "VISITED")));

        AnswerResult result = answerer().answer(SESSION_ID, "Q01", "B");

        assertEquals(ANSWERED, result.outcome());
        assertEquals("Q01", result.questionId());
        Map<String, Object> data = parseSession();
        assertSessionState(data, "IN_PROGRESS", 1, 0, 2, 0);
        Map<String, Object> q01 = questionOf(data, "Q01");
        assertEquals("ANSWERED", q01.get("status"));
        assertEquals("B", q01.get("answer"));
        assertEquals(UPDATE_AT, q01.get("updatedAt"));
    }

    @Test
    void answerOnSkippedQuestionDecrementsSkippedAndMarksAnswered() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of(
                "Q01", Map.of("status", "SKIPPED"),
                "Q02", Map.of("status", "SKIPPED")));

        AnswerResult result = answerer().answer(SESSION_ID, "Q01", "B");

        assertEquals(ANSWERED, result.outcome());
        Map<String, Object> data = parseSession();
        assertSessionState(data, "IN_PROGRESS", 1, 1, 2, 0);
        assertEquals("ANSWERED", questionOf(data, "Q01").get("status"));
    }

    @Test
    void replacingAnswerOnAnsweredQuestionYieldsUpdatedAndKeepsCounters() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B"),
                "Q02", Map.of("status", "ANSWERED", "answer", "A")));

        AnswerResult result = answerer().answer(SESSION_ID, "Q01", "A");

        assertEquals(UPDATED, result.outcome());
        Map<String, Object> data = parseSession();
        assertSessionState(data, "IN_PROGRESS", 2, 0, 2, 0);
        assertEquals("A", questionOf(data, "Q01").get("answer"));
    }

    @Test
    void reAnsweringSameOptionIsIdempotentAndUnchanged() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of(
                "Q01", Map.of("status", "ANSWERED", "answer", "B")));

        AnswerResult result = answerer().answer(SESSION_ID, "Q01", "B");

        assertEquals(UNCHANGED, result.outcome());
        Map<String, Object> data = parseSession();
        assertSessionState(data, "IN_PROGRESS", 1, 0, 1, 0);
        assertEquals("B", questionOf(data, "Q01").get("answer"));
    }

    @Test
    void answerOnNotVisitedQuestionDirectlyBecomesAnswered() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of());

        AnswerResult result = answerer().answer(SESSION_ID, "Q03", "C");

        assertEquals(ANSWERED, result.outcome());
        Map<String, Object> data = parseSession();
        assertSessionState(data, "IN_PROGRESS", 1, 0, 1, 0);
        assertEquals("C", questionOf(data, "Q03").get("answer"));
    }

    @Test
    void unknownQuestionSignalsQuestionNotInExam() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of());

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> answerer().answer(SESSION_ID, "Q99", "B"));

        assertEquals(QUESTION_NOT_IN_EXAM, e.kind());
    }

    @Test
    void missingSessionSignalsNotFound() throws IOException {
        writeDefinition(VALID_DEFINITION);

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> answerer().answer(SESSION_ID, "Q01", "B"));

        assertEquals(SESSION_NOT_FOUND, e.kind());
    }

    @Test
    void pausedSessionRejectsAnswer() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(PAUSED, Map.of());

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> answerer().answer(SESSION_ID, "Q01", "B"));

        assertEquals(SESSION_NOT_ANSWERABLE, e.kind());
    }

    @Test
    void nonActiveTerminalStatesRejectAnswer() throws IOException {
        writeDefinition(VALID_DEFINITION);
        for (ExamSessionStatus terminal : new ExamSessionStatus[] {FINISHED, GRADED}) {
            seed(terminal, Map.of());
            ExamSessionException e = assertThrows(ExamSessionException.class,
                    () -> answerer().answer(SESSION_ID, "Q01", "B"));
            assertEquals(SESSION_NOT_ANSWERABLE, e.kind(), "falhou para " + terminal);
        }
    }

    @Test
    void optionOutsideAllowedSetSignalsInvalidAnswer() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of());

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> answerer().answer(SESSION_ID, "Q01", "E"));

        assertEquals(INVALID_ANSWER_OPTION, e.kind());
    }

    @Test
    void blankAnswerSignalsInvalidAnswer() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of());

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> answerer().answer(SESSION_ID, "Q01", "  "));

        assertEquals(INVALID_ANSWER_OPTION, e.kind());
    }

    @Test
    void definitionOptionsOverrideDefaultOptionSet() throws IOException {
        writeDefinition(DEFINITION_WITH_OPTIONS);
        seed(IN_PROGRESS, Map.of(), 2);

        AnswerResult accepted = answerer().answer(SESSION_ID, "Q02", "Y");
        assertEquals(ANSWERED, accepted.outcome());

        ExamSessionException rejected = assertThrows(ExamSessionException.class,
                () -> answerer().answer(SESSION_ID, "Q02", "A"));
        assertEquals(INVALID_ANSWER_OPTION, rejected.kind());
    }

    @Test
    void flagIsPreservedWhenAnswering() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of(
                "Q01", Map.of("status", "VISITED", "flagged", true),
                "Q02", Map.of("status", "VISITED", "flagged", false)));

        answerer().answer(SESSION_ID, "Q01", "B");
        answerer().answer(SESSION_ID, "Q02", "A");

        Map<String, Object> data = parseSession();
        assertEquals(Boolean.TRUE, questionOf(data, "Q01").get("flagged"));
        assertEquals(Boolean.FALSE, questionOf(data, "Q02").get("flagged"));
        assertSessionState(data, "IN_PROGRESS", 2, 0, 2, 1);
    }

    @Test
    void answerConsumesActiveTimeAndKeepsInvariant() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of());

        answerer().answer(SESSION_ID, "Q01", "B");

        Map<String, Object> data = parseSession();
        assertEquals(700, integer(data, "elapsedSeconds"));
        assertEquals(6500, integer(data, "remainingSeconds"));
        assertEquals(7200, integer(data, "elapsedSeconds") + integer(data, "remainingSeconds"));
        assertEquals(UPDATE_AT, data.get("lastActivityAt"));
        assertEquals("Q01", data.get("currentQuestion"));
    }

    @Test
    void sequenceOfAnswersPersistsConsistentState() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of());

        answerer().answer(SESSION_ID, "Q01", "B");
        answerer().answer(SESSION_ID, "Q02", "D");
        answerer().answer(SESSION_ID, "Q03", "A");

        Map<String, Object> data = parseSession();
        assertSessionState(data, "IN_PROGRESS", 3, 0, 3, 0);
        assertEquals("B", questionOf(data, "Q01").get("answer"));
        assertEquals("D", questionOf(data, "Q02").get("answer"));
        assertEquals("A", questionOf(data, "Q03").get("answer"));
        try (var files = Files.list(temp.resolve("sessions"))) {
            assertTrue(files.noneMatch(file -> file.getFileName().toString().endsWith(".tmp")),
                    "arquivos temporários não limpos");
        }
    }

    @Test
    void deterministicReplayProducesSamePersistedState() throws IOException {
        writeDefinition(VALID_DEFINITION);
        seed(IN_PROGRESS, Map.of());
        Path other = temp.resolveSibling(temp.getFileName() + "-clone");
        writeDefinitionTo(other, VALID_DEFINITION);
        seedInto(other, IN_PROGRESS, Map.of());

        answerer().answer(SESSION_ID, "Q03", "B");
        new ExamSessionAnswerer(other, () -> NOW).answer(SESSION_ID, "Q03", "B");

        assertEquals(Files.readString(sessionFile()),
                Files.readString(other.resolve("sessions").resolve(SESSION_ID + ".yaml")));
    }

    private ExamSessionAnswerer answerer() {
        return new ExamSessionAnswerer(temp, () -> NOW);
    }

    private void seed(ExamSessionStatus status, Map<String, Map<String, Object>> questions) throws IOException {
        seedInto(temp, status, questions);
    }

    private void seed(ExamSessionStatus status, Map<String, Map<String, Object>> questions, int totalQuestions)
            throws IOException {
        seedInto(temp, status, questions, totalQuestions);
    }

    private void seedInto(Path root, ExamSessionStatus status, Map<String, Map<String, Object>> questions)
            throws IOException {
        seedInto(root, status, questions, 4);
    }

    private void seedInto(Path root, ExamSessionStatus status, Map<String, Map<String, Object>> questions,
            int totalQuestions) throws IOException {
        Path sessions = root.resolve("sessions");
        Files.createDirectories(sessions);
        ExamSession session = new ExamSession(1, SESSION_ID, "mock-01", 1, status,
                STARTED_AT, LAST_ACTIVITY_AT, null, "Q01", 100, 7100, totalQuestions, 0, 0, 0, 0, questions);
        Files.writeString(sessions.resolve(SESSION_ID + ".yaml"), session.toYamlText());
    }

    private void writeDefinition(String content) throws IOException {
        writeDefinitionTo(temp, content);
    }

    private void writeDefinitionTo(Path root, String content) throws IOException {
        Path dir = root.resolve("mock-01");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("definition.yaml"), content);
    }

    private Map<String, Object> parseSession() {
        return SessionYaml.parse(sessionFile());
    }

    private Path sessionFile() {
        return temp.resolve("sessions").resolve(SESSION_ID + ".yaml");
    }

    private static Map<String, Object> questionOf(Map<String, Object> data, String questionId) {
        return SessionYaml.asStringMap(((Map<?, ?>) data.get("questions")).get(questionId));
    }

    private static void assertSessionState(Map<String, Object> data, String status, int answered,
            int skipped, int visited, int flagged) {
        assertEquals(status, data.get("status"));
        assertEquals(answered, integer(data, "answeredQuestions"));
        assertEquals(skipped, integer(data, "skippedQuestions"));
        assertEquals(visited, integer(data, "visitedQuestions"));
        assertEquals(flagged, integer(data, "flaggedQuestions"));
        Map<?, ?> questions = (Map<?, ?>) data.get("questions");
        long countAnswered = questions.values().stream()
                .filter(v -> "ANSWERED".equals(((Map<?, ?>) v).get("status"))).count();
        long countSkipped = questions.values().stream()
                .filter(v -> "SKIPPED".equals(((Map<?, ?>) v).get("status"))).count();
        long countFlagged = questions.values().stream()
                .filter(v -> Boolean.TRUE.equals(((Map<?, ?>) v).get("flagged"))).count();
        assertEquals(answered, countAnswered);
        assertEquals(skipped, countSkipped);
        assertEquals(flagged, countFlagged);
    }

    private static int integer(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).intValue();
    }
}