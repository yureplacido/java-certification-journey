package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_NOT_FOUND;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_VERSION_MISMATCH;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.PROGRESS_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.PROGRESS_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionStatus.ABANDONED;
import static com.placido.certification.exams.session.ExamSessionStatus.FINISHED;
import static com.placido.certification.exams.session.ExamSessionStatus.IN_PROGRESS;
import static com.placido.certification.exams.session.ExamSessionStatus.NOT_STARTED;
import static com.placido.certification.exams.session.ExamSessionStatus.PAUSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgressTest {

    private static final OffsetDateTime GRADE_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime GRADE_AT_2 =
            OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime ANALYZE_AT =
            OffsetDateTime.of(2026, 9, 10, 15, 10, 0, 0, ZoneOffset.ofHours(-3));

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

    private static final String SESSION_A = "2026-09-10-mock-01-01";
    private static final String SESSION_B = "2026-09-10-mock-01-02";
    private static final String SESSION_C = "2026-09-10-mock-01-03";

    private static final Map<String, Map<String, Object>> ALL_CORRECT = Map.of(
            "Q01", Map.of("status", "ANSWERED", "answer", "B"),
            "Q02", Map.of("status", "ANSWERED", "answer", "D"),
            "Q03", Map.of("status", "ANSWERED", "answer", "B"),
            "Q04", Map.of("status", "ANSWERED", "answer", "C"));

    private static final Map<String, Map<String, Object>> HALF_CORRECT = Map.of(
            "Q01", Map.of("status", "ANSWERED", "answer", "B"),
            "Q02", Map.of("status", "ANSWERED", "answer", "A"),
            "Q03", Map.of("status", "ANSWERED", "answer", "B"),
            "Q04", Map.of("status", "ANSWERED", "answer", "A"));

    private static final String PROGRESS_FIXTURE = """
            # Comentario antes de meta.
            meta:
              projeto: java-certification-journey
              semana_atual: 1
            # Comentario depois de meta.

            # Comentario relacionado a topicos.
            topicos:
              language:     { status: planned }
              oop:          { status: planned }

            exames:
              diagnostic:   { status: in_progress, data: null, nota: null, relatorio: "exams/diagnostic/" }
              mock-01:      { status: pending, data: null, nota: null, relatorio: "exams/mock-01/" }
              mock-02:      { status: pending, data: null, nota: null, relatorio: "exams/mock-02/" }
              mock-03:      { status: pending, data: null, nota: null, relatorio: "exams/mock-03/" }
              final:        { status: pending, data: null, nota: null, relatorio: "exams/final/" }

            # Comentario relacionado a java25.
            java25:
              delta:      { status: pending }
              language:   { status: pending }
            # Comentario no tail/final do documento.
            """;

    private static final Set<String> FORBIDDEN_KEYS = Set.of("mastery", "dominio", "forte", "fraco",
            "gaps", "traps", "trends", "byDifficulty", "recomendacoes", "recomendation", "projecoes");

    @TempDir
    Path temp;

    @Test
    void singleGradedSessionProducesOneAttemptWithoutAnalyzedAt() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);

        ProgressResult result = new Progress(examsRoot()).run();

        assertEquals(1, result.sessionsScanned());
        assertEquals(1, result.attemptsWritten());
        assertEquals(0, result.sessionsIgnored());
        Map<String, Object> exam = examOf("mock-01");
        assertEquals("pending", exam.get("status"));
        List<?> tentativas = list(exam.get("tentativas"));
        assertEquals(1, tentativas.size());
        Map<String, Object> attempt = cast(tentativas.get(0));
        assertEquals(SESSION_A, attempt.get("sessionId"));
        assertEquals(1, num(attempt, "examVersion"));
        assertEquals("GRADED", attempt.get("status"));
        assertEquals("2026-09-10T15:00:00-03:00", attempt.get("gradedAt"));
        assertFalse(attempt.containsKey("analyzedAt"));
        assertEquals(4, num(attempt, "correct"));
        assertEquals(0, num(attempt, "wrong"));
        assertEquals(0, num(attempt, "unanswered"));
        assertEquals(100, num(attempt, "scorePercent"));
        assertEquals(Boolean.TRUE, attempt.get("passing"));
        assertEquals("2026-09-10T15:00:00-03:00", exam.get("data"));
        assertEquals(100, num(exam, "nota"));
        assertHumanPartsPreserved(original);
        assertNoForbiddenWords();
    }

    @Test
    void analyzedSessionReadsAnalyzedAtAndStatusFromSession() throws IOException {
        fixtureProgress();
        analyzed(SESSION_A, ALL_CORRECT, () -> ANALYZE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> attempt = onlyAttempt();
        assertEquals("ANALYZED", attempt.get("status"));
        assertEquals("2026-09-10T15:10:00-03:00", attempt.get("analyzedAt"));
        assertEquals(100, num(attempt, "scorePercent"));
    }

    @Test
    void dataAndNotaComeFromMostRecentAttemptBySessionId() throws IOException {
        fixtureProgress();
        graded(SESSION_B, HALF_CORRECT, () -> GRADE_AT_2);
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> exam = examOf("mock-01");
        assertEquals("2026-09-10T16:00:00-03:00", exam.get("data"));
        assertEquals(50, num(exam, "nota"));
        List<?> tentativas = list(exam.get("tentativas"));
        assertEquals(List.of(SESSION_A, SESSION_B),
                tentativas.stream().map(this::sessionOf).toList());
        assertEquals(50, num(cast(tentativas.get(1)), "scorePercent"));
    }

    @Test
    void melhorIsHighestScorePercentAndTieGoesToFirstSessionId() throws IOException {
        fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        graded(SESSION_B, ALL_CORRECT, () -> GRADE_AT);
        graded(SESSION_C, HALF_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> melhor = cast(examOf("mock-01").get("melhor"));
        assertEquals(SESSION_A, melhor.get("sessionId"));
        assertEquals(100, num(melhor, "scorePercent"));
        assertEquals(Boolean.TRUE, melhor.get("passing"));
        assertEquals(List.of(SESSION_A, SESSION_B, SESSION_C),
                list(examOf("mock-01").get("tentativas")).stream().map(this::sessionOf).toList());
    }

    @Test
    void noEligibleAttemptsYieldsNullDateNotaMelhorAndEmptyTentativas() throws IOException {
        fixtureProgress();
        seed(SESSION_A, IN_PROGRESS, ALL_CORRECT, 4);

        ProgressResult result = new Progress(examsRoot()).run();

        assertEquals(1, result.sessionsScanned());
        assertEquals(1, result.sessionsIgnored());
        assertEquals(0, result.attemptsWritten());
        Map<String, Object> exam = examOf("mock-01");
        assertNull(exam.get("data"));
        assertNull(exam.get("nota"));
        assertNull(exam.get("melhor"));
        assertTrue(list(exam.get("tentativas")).isEmpty());
    }

    @Test
    void ineligibleStatusesAreIgnoredSilently() throws IOException {
        fixtureProgress();
        seed(SESSION_A, NOT_STARTED, ALL_CORRECT, 4);
        seed(SESSION_B, IN_PROGRESS, ALL_CORRECT, 4);
        seed(SESSION_C, PAUSED, ALL_CORRECT, 4);
        seed("2026-09-10-mock-01-04", FINISHED, ALL_CORRECT, 4);
        seed("2026-09-10-mock-01-05", ABANDONED, ALL_CORRECT, 4);

        ProgressResult result = new Progress(examsRoot()).run();

        assertEquals(5, result.sessionsScanned());
        assertEquals(5, result.sessionsIgnored());
        assertEquals(0, result.attemptsWritten());
        assertTrue(list(examOf("mock-01").get("tentativas")).isEmpty());
    }

    @Test
    void gradedSessionWithoutResultIsIgnored() throws IOException {
        fixtureProgress();
        seed(SESSION_A, GRADED_STATUS(), ALL_CORRECT, 4);

        ProgressResult result = new Progress(examsRoot()).run();

        assertEquals(1, result.sessionsIgnored());
        assertEquals(0, result.attemptsWritten());
        assertTrue(list(examOf("mock-01").get("tentativas")).isEmpty());
    }

    @Test
    void analyzedSessionWithoutArtifactSkipsTopicValidationAndOmitsAnalyzedAt() throws IOException {
        fixtureProgress();
        analyzed(SESSION_A, ALL_CORRECT, () -> ANALYZE_AT);
        Files.delete(artifactOf(SESSION_A));

        new Progress(examsRoot()).run();

        Map<String, Object> attempt = onlyAttempt();
        assertEquals("ANALYZED", attempt.get("status"));
        assertFalse(attempt.containsKey("analyzedAt"));
    }

    @Test
    void reScoresAreRecomputedFromQuestionsAndMatchResult() throws IOException {
        fixtureProgress();
        graded(SESSION_A, HALF_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> attempt = onlyAttempt();
        assertEquals(2, num(attempt, "correct"));
        assertEquals(2, num(attempt, "wrong"));
        assertEquals(0, num(attempt, "unanswered"));
        assertEquals(50, num(attempt, "scorePercent"));
        assertEquals(Boolean.FALSE, attempt.get("passing"));
    }

    @Test
    void perAttemptSectionsMatchComputationInNumericOrder() throws IOException {
        fixtureProgress();
        graded(SESSION_A, HALF_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> sections = cast(onlyAttempt().get("sections"));
        assertEquals(List.of(1, 2), new ArrayList<>(sections.keySet()));
        assertEquals(1, num(cast(sections.get(1)), "correct"));
        assertEquals(2, num(cast(sections.get(1)), "total"));
        assertEquals(1, num(cast(sections.get(2)), "correct"));
        assertEquals(2, num(cast(sections.get(2)), "total"));
    }

    @Test
    void perAttemptTopicsFollowFirstAppearanceInQuestionOrder() throws IOException {
        fixtureProgress();
        graded(SESSION_A, HALF_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> topics = cast(onlyAttempt().get("topics"));
        assertEquals(List.of("language-basics", "oop"), new ArrayList<>(topics.keySet()));
        assertEquals(1, num(cast(topics.get("language-basics")), "correct"));
        assertEquals(2, num(cast(topics.get("language-basics")), "total"));
        assertEquals(1, num(cast(topics.get("oop")), "correct"));
        assertEquals(2, num(cast(topics.get("oop")), "total"));
    }

    @Test
    void sectionsAggregateSumsAcrossAttempts() throws IOException {
        fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        graded(SESSION_B, HALF_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> sections = cast(examOf("mock-01").get("sections"));
        assertEquals(List.of(1, 2), new ArrayList<>(sections.keySet()));
        assertEquals(3, num(cast(sections.get(1)), "correct"));
        assertEquals(4, num(cast(sections.get(1)), "total"));
        assertEquals(3, num(cast(sections.get(2)), "correct"));
        assertEquals(4, num(cast(sections.get(2)), "total"));
    }

    @Test
    void topicsAggregateSumsAcrossAttempts() throws IOException {
        fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        graded(SESSION_B, HALF_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        Map<String, Object> topics = cast(examOf("mock-01").get("topics"));
        assertEquals(3, num(cast(topics.get("language-basics")), "correct"));
        assertEquals(4, num(cast(topics.get("language-basics")), "total"));
        assertEquals(3, num(cast(topics.get("oop")), "correct"));
        assertEquals(4, num(cast(topics.get("oop")), "total"));
    }

    @Test
    void noGlobalAggregateAndNewExamIdGetsPendingStatusAndDefaultRelatorio() throws IOException {
        fixtureProgress();
        writeDefinition("mock-09", validDefinitionFor("mock-09"));
        graded("2026-09-10-mock-09-01", ALL_CORRECT, () -> GRADE_AT, "mock-09");

        new Progress(examsRoot()).run();

        Map<String, Object> progress = parsedProgress();
        assertFalse(progress.containsKey("total"));
        assertFalse(progress.containsKey("overall"));
        assertFalse(progress.containsKey("global"));
        Map<String, Object> exam = cast(examOfRaw("mock-09"));
        assertEquals("pending", exam.get("status"));
        assertEquals("exams/mock-09/", exam.get("relatorio"));
        assertEquals(1, list(exam.get("tentativas")).size());
        assertEquals("pending", examOf("mock-01").get("status"));
    }

    @Test
    void humanPartsArePreservedByteForByte() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        assertHumanPartsPreserved(original);
        Map<String, Object> exam = examOf("mock-01");
        assertEquals("pending", exam.get("status"));
        assertEquals("exams/mock-01/", exam.get("relatorio"));
        assertEquals("in_progress", examOf("diagnostic").get("status"));
        assertEquals("exams/diagnostic/", examOf("diagnostic").get("relatorio"));
        assertTrue(list(examOf("mock-02").get("tentativas")).isEmpty());
        assertNull(examOf("mock-02").get("nota"));
    }

    @Test
    void runWritesOnlyTheProgressFile() throws IOException {
        fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        List<String> before = relativeFiles();

        new Progress(examsRoot()).run();

        List<String> after = relativeFiles();
        assertEquals(before, after);
        assertNoForbiddenWords();
    }

    @Test
    void topicosAndMetaBlocksAreNotTouched() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        String regenerated = Files.readString(progressFile());
        assertEquals(region(original, "topicos:", "exames:"), region(regenerated, "topicos:", "exames:"));
        assertEquals(region(original, "meta:", "topicos:"), region(regenerated, "meta:", "topicos:"));
        assertEquals(region(original, "java25:", null), region(regenerated, "java25:", null));
    }

    @Test
    void twoRunsProduceByteIdenticalOutput() throws IOException {
        fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        new Progress(examsRoot()).run();
        String first = Files.readString(progressFile());

        new Progress(examsRoot()).run();

        assertEquals(first, Files.readString(progressFile()));
    }

    @Test
    void reGradeOrReAnalyzeKeepsSingleAttemptPerSessionId() throws IOException {
        fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        new Progress(examsRoot()).run();
        assertEquals(1, list(examOf("mock-01").get("tentativas")).size());

        tamperResult(SESSION_A, "gradedAt", "2026-09-10T17:00:00-03:00");
        new Progress(examsRoot()).run();

        List<?> tentativas = list(examOf("mock-01").get("tentativas"));
        assertEquals(1, tentativas.size());
        assertEquals("2026-09-10T17:00:00-03:00",
                ((Map<String, Object>) tentativas.get(0)).get("gradedAt"));
    }

    @Test
    void missingSessionsDirectoryYieldsEmptyDerivation() throws IOException {
        fixtureProgress();
        Files.deleteIfExists(examsRoot().resolve("sessions"));

        new Progress(examsRoot()).run();

        assertEquals(0, list(examOf("mock-01").get("tentativas")).size());
        assertNull(examOf("mock-01").get("nota"));
    }

    @Test
    void sessionFilesNotMatchingNamingPatternAreIgnored() throws IOException {
        fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        Files.writeString(examsRoot().resolve("sessions").resolve("2026-09-10-mock-01-1.yaml"), "stray\n");
        Files.writeString(examsRoot().resolve("sessions").resolve("session-example.yaml"), "stray\n");

        ProgressResult result = new Progress(examsRoot()).run();

        assertEquals(1, result.sessionsScanned());
        assertEquals(1, result.attemptsWritten());
    }

    @Test
    void corruptedSessionYamlFailsHardAndLeavesProgressIntact() throws IOException {
        String original = fixtureProgress();
        Files.createDirectories(examsRoot().resolve("sessions"));
        Files.writeString(examsRoot().resolve("sessions").resolve(SESSION_A + ".yaml"), "a: [\ninvalid");

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(SESSION_STORE_INVALID, e.kind());
        assertTrue(Files.readString(progressFile()).equals(original));
    }

    @Test
    void inconsistentStoredResultFailsHardAndLeavesProgressIntact() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        tamperResult(SESSION_A, "correct", 99);

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(SESSION_STORE_INVALID, e.kind());
        assertTrue(e.getMessage().contains("correct"));
        assertTrue(Files.readString(progressFile()).equals(original));
    }

    @Test
    void divergingArtifactTopicsFailsHardAndLeavesProgressIntact() throws IOException {
        String original = fixtureProgress();
        analyzed(SESSION_A, ALL_CORRECT, () -> ANALYZE_AT);
        assertTrue(Files.isRegularFile(artifactOf(SESSION_A)));
        Map<String, Object> artifact = SessionYaml.parse(artifactOf(SESSION_A));
        Map<String, Object> tamperedByTopic = new LinkedHashMap<>();
        tamperedByTopic.put("language-basics", Map.of("correct", 99, "total", 2));
        artifact.put("byTopic", tamperedByTopic);
        Files.writeString(artifactOf(SESSION_A), SessionYaml.dump(artifact));

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(SESSION_STORE_INVALID, e.kind());
        assertTrue(Files.readString(progressFile()).equals(original));
    }

    @Test
    void versionMismatchFailsHardAndLeavesProgressIntact() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        writeDefinition("mock-01", VALID_DEFINITION.replace("version: 1", "version: 2"));

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(EXAM_DEFINITION_VERSION_MISMATCH, e.kind());
        assertTrue(Files.readString(progressFile()).equals(original));
    }

    @Test
    void missingDefinitionFailsHardAndLeavesProgressIntact() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        Files.delete(examsRoot().resolve("mock-01").resolve("definition.yaml"));

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(EXAM_DEFINITION_NOT_FOUND, e.kind());
        assertTrue(Files.readString(progressFile()).equals(original));
    }

    @Test
    void invalidDefinitionFailsHardAndLeavesProgressIntact() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        writeDefinition("mock-01", VALID_DEFINITION.replace("examId: mock-01", "examId: mock-02"));

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(EXAM_DEFINITION_INVALID, e.kind());
        assertTrue(Files.readString(progressFile()).equals(original));
    }

    @Test
    void malformedProgressYamlSignalsInvalid() throws IOException {
        Files.createDirectories(temp.resolve("docs"));
        String broken = "a: [\ninvalid";
        Files.writeString(progressFile(), broken);

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(PROGRESS_INVALID, e.kind());
        assertTrue(Files.readString(progressFile()).equals(broken));
    }

    @Test
    void progressWithoutExamesBlockSignalsInvalid() throws IOException {
        Files.createDirectories(temp.resolve("docs"));
        String withoutExames = "meta:\n  projeto: x\n";
        Files.writeString(progressFile(), withoutExames);

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(PROGRESS_INVALID, e.kind());
        assertTrue(Files.readString(progressFile()).equals(withoutExames));
    }

    @Test
    void unreadableProgressSignalsUnreadable() throws IOException {
        Files.createDirectories(temp.resolve("docs").resolve("progress.yaml"));

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(PROGRESS_UNREADABLE, e.kind());
    }

    @Test
    void sessionIdMismatchingFileNameFailsHard() throws IOException {
        fixtureProgress();
        writeDefinition("mock-01", VALID_DEFINITION);
        Files.createDirectories(examsRoot().resolve("sessions"));
        Files.writeString(examsRoot().resolve("sessions").resolve(SESSION_A + ".yaml"),
                "sessionId: 2026-09-10-mock-01-99\nexamId: mock-01\n");

        ExamSessionException e = assertThrows(ExamSessionException.class, () -> new Progress(examsRoot()).run());

        assertEquals(SESSION_STORE_INVALID, e.kind());
    }

    @Test
    void writeFailureLeavesOriginalByteIntactAndNoTempResidual() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);
        // Bloqueia a escrita do arquivo temporário de forma determinística e independente de
        // privilégios do usuário: o caminho do temp existe como diretório, então a escrita
        // em docs/progress.yaml.tmp falha ("Is a directory") antes de qualquer toque no original.
        Files.createDirectories(temp.resolve("docs").resolve("progress.yaml.tmp"));

        ExamSessionException e = assertThrows(ExamSessionException.class,
                () -> new Progress(examsRoot()).run());

        assertEquals(PROGRESS_UNREADABLE, e.kind());
        assertEquals(original, Files.readString(progressFile()));
        assertFalse(Files.exists(temp.resolve("docs").resolve("progress.yaml.tmp")));
    }

    @Test
    void attemptKeepsItsExamVersion() throws IOException {
        fixtureProgress();
        writeDefinition("mock-01", VALID_DEFINITION.replace("version: 1", "version: 2"));
        seed(SESSION_A, FINISHED, ALL_CORRECT, 4, "mock-01", 2);
        new ExamSessionGrader(examsRoot(), () -> GRADE_AT).grade(SESSION_A);

        new Progress(examsRoot()).run();

        Map<String, Object> attempt = onlyAttempt();
        assertEquals(2, num(attempt, "examVersion"));
        assertEquals(100, num(attempt, "scorePercent"));
        assertEquals(Boolean.TRUE, attempt.get("passing"));
        assertEquals("2026-09-10T15:00:00-03:00", attempt.get("gradedAt"));
        assertEquals("2026-09-10T15:00:00-03:00", examOf("mock-01").get("data"));
        assertEquals(100, examOf("mock-01").get("nota"));
        Map<String, Object> sections = cast(attempt.get("sections"));
        assertEquals(2, num(cast(sections.get(1)), "correct"));
        assertEquals(2, num(cast(sections.get(1)), "total"));
        List<?> tentativas = list(examOf("mock-01").get("tentativas"));
        assertEquals(1, tentativas.size());
        assertEquals(2, num(cast(tentativas.get(0)), "examVersion"));
    }

    private Map<String, Object> onlyAttempt() throws IOException {
        return cast(list(examOf("mock-01").get("tentativas")).get(0));
    }

    private void graded(String sessionId, Map<String, Map<String, Object>> questions,
            java.util.function.Supplier<OffsetDateTime> clock) throws IOException {
        graded(sessionId, questions, clock, "mock-01");
    }

    private void graded(String sessionId, Map<String, Map<String, Object>> questions,
            java.util.function.Supplier<OffsetDateTime> clock, String examId) throws IOException {
        writeDefinition(examId, validDefinitionFor(examId));
        seed(sessionId, FINISHED, questions, 4, examId);
        new ExamSessionGrader(examsRoot(), clock).grade(sessionId);
    }

    private void analyzed(String sessionId, Map<String, Map<String, Object>> questions,
            java.util.function.Supplier<OffsetDateTime> clock) throws IOException {
        graded(sessionId, questions, () -> GRADE_AT);
        new ExamSessionAnalyzer(examsRoot(), clock).analyze(sessionId);
    }

    private Map<String, Object> examOf(String examId) throws IOException {
        return castEntryOf(parsedProgress(), examId);
    }

    private Map<String, Object> examOfRaw(String examId) throws IOException {
        return cast(examOfRawProgress().get(examId));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castEntryOf(Map<String, Object> progress, String examId) {
        return (Map<String, Object>) ((Map<String, Object>) progress.get("exames")).get(examId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> examOfRawProgress() throws IOException {
        return (Map<String, Object>) parsedProgress().get("exames");
    }

    private Map<String, Object> parsedProgress() throws IOException {
        return SessionYaml.parse(progressFile());
    }

    private String fixtureProgress() throws IOException {
        Files.createDirectories(temp.resolve("docs"));
        Files.writeString(progressFile(), PROGRESS_FIXTURE);
        return PROGRESS_FIXTURE;
    }

    private Path examsRoot() {
        return temp.resolve("exams");
    }

    private Path progressFile() {
        return temp.resolve("docs").resolve("progress.yaml");
    }

    private Path artifactOf(String sessionId) {
        return temp.resolve("docs").resolve("study-log").resolve(sessionId + ".analysis.yaml");
    }

    private String validDefinitionFor(String examId) {
        return VALID_DEFINITION.replace("examId: mock-01", "examId: " + examId);
    }

    private void writeDefinition(String examId, String content) throws IOException {
        Path dir = examsRoot().resolve(examId);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("definition.yaml"), content);
    }

    private void seed(String sessionId, ExamSessionStatus status,
            Map<String, Map<String, Object>> questions, int totalQuestions) throws IOException {
        seed(sessionId, status, questions, totalQuestions, "mock-01");
    }

    private void seed(String sessionId, ExamSessionStatus status,
            Map<String, Map<String, Object>> questions, int totalQuestions, String examId)
            throws IOException {
        seed(sessionId, status, questions, totalQuestions, examId, 1);
    }

    private void seed(String sessionId, ExamSessionStatus status,
            Map<String, Map<String, Object>> questions, int totalQuestions, String examId,
            int examVersion) throws IOException {
        Path sessions = examsRoot().resolve("sessions");
        Files.createDirectories(sessions);
        ExamSession session = new ExamSession(1, sessionId, examId, examVersion, status,
                STARTED_AT, LAST_ACTIVITY_AT, FINISHED_AT, "Q03",
                2700, 4500, totalQuestions,
                count(questions, "ANSWERED"), count(questions, "SKIPPED"),
                questions.size(), countFlagged(questions), questions);
        Files.writeString(sessions.resolve(sessionId + ".yaml"), session.toYamlText());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resultOf(Map<String, Object> data) {
        return (Map<String, Object>) data.get("result");
    }

    private void tamperResult(String sessionId, String key, Object value) throws IOException {
        Path file = examsRoot().resolve("sessions").resolve(sessionId + ".yaml");
        Map<String, Object> data = SessionYaml.parse(file);
        resultOf(data).put(key, value);
        Files.writeString(file, SessionYaml.dump(data));
    }

    private void assertHumanPartsPreserved(String original) throws IOException {
        String regenerated = Files.readString(progressFile());
        assertEquals(beginning(original), beginning(regenerated));
        assertEquals(fromStartOfTail(original), fromStartOfTail(regenerated));
    }

    @Test
    void commentsInHumanRegionsArePreservedByteForByte() throws IOException {
        String original = fixtureProgress();
        graded(SESSION_A, ALL_CORRECT, () -> GRADE_AT);

        new Progress(examsRoot()).run();

        String regenerated = Files.readString(progressFile());
        // byte-level: as regiões compartilhadas (head e tail) são cópia textual integral
        assertEquals(beginning(original), beginning(regenerated));
        assertEquals(fromStartOfTail(original), fromStartOfTail(regenerated));
        // cada comentário continua presente, na ordem original
        int last = -1;
        for (String comment : List.of(
                "# Comentario antes de meta.",
                "# Comentario depois de meta.",
                "# Comentario relacionado a topicos.",
                "# Comentario relacionado a java25.",
                "# Comentario no tail/final do documento.")) {
            int index = regenerated.indexOf(comment);
            assertTrue(index > last, "comentário perdido ou reordenado: " + comment);
            last = index;
        }
        // Limitação documentada da decisão atual: comentários dentro do bloco derivado
        // `exames:` são regenerados e portanto NÃO são preservados byte-a-byte. Não testar
        // como se fossem preservados — a garantia vale apenas para as regiões humanas acima.
    }

    private static String region(String text, String start, String end) {
        int from = text.indexOf(start);
        int to = end == null ? text.length() : text.indexOf(end);
        return text.substring(from, to);
    }

    private static String beginning(String text) {
        return text.substring(0, text.indexOf("exames:") + "exames:".length());
    }

    private static String fromStartOfTail(String text) {
        return text.substring(text.indexOf("# Comentario relacionado a java25."));
    }

    private void assertNoForbiddenWords() throws IOException {
        String text = Files.readString(progressFile());
        for (Map.Entry<String, Object> entry : parsedProgress().entrySet()) {
            assertFalse(FORBIDDEN_KEYS.contains(entry.getKey()),
                    "chave proibida no progresso: " + entry.getKey());
        }
        for (String word : FORBIDDEN_KEYS) {
            assertFalse(text.contains(word), "palavra proibida no progresso: " + word);
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

    private String sessionOf(Object attempt) {
        return String.valueOf(((Map<String, Object>) attempt).get("sessionId"));
    }

    private static ExamSessionStatus GRADED_STATUS() {
        return ExamSessionStatus.GRADED;
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

    private static List<?> list(Object raw) {
        return (List<?>) raw;
    }

    private static int num(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }
}