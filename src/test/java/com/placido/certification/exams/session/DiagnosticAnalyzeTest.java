package com.placido.certification.exams.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Analyze exclusivo do diagnóstico do dia 1: executar explicitamente para reproduzir")
class DiagnosticAnalyzeTest {

    private static final OffsetDateTime ANALYZE_TIME =
            OffsetDateTime.of(2026, 9, 11, 12, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final String SESSION_ID = "2026-09-10-diagnostic-01";

    @Test
    void analyzeDiagnosticDay1() {
        Path examsRoot = Path.of(System.getProperty("user.dir"), "exams");
        AnalysisResult result = new ExamSessionAnalyzer(examsRoot, () -> ANALYZE_TIME)
                .analyze(SESSION_ID);

        System.out.println("=== ANALYZE ===");
        System.out.println("outcome: " + result.outcome());
        System.out.println("artifactFile: " + result.artifactFile());
        System.out.println("sessionFile: " + result.sessionFile());

        assertTrue(Files.isRegularFile(result.artifactFile()));
        Map<String, Object> session = SessionYaml.parse(result.sessionFile());
        Map<String, Object> artifact = SessionYaml.parse(result.artifactFile());

        assertSessionPreserved(session);
        assertArtifact(artifact);
        assertConsistent(session, artifact);
    }

    private void assertSessionPreserved(Map<String, Object> session) {
        assertEquals("ANALYZED", session.get("status"));
        assertEquals("Q01", session.get("currentQuestion"));
        assertEquals(43, num(session, "answeredQuestions"));
        assertEquals(0, num(session, "skippedQuestions"));
        assertEquals(43, num(session, "visitedQuestions"));
        assertEquals(0, num(session, "flaggedQuestions"));
        assertEquals(3600, num(session, "elapsedSeconds"));
        assertEquals(3600, num(session, "remainingSeconds"));

        Map<String, Object> resultBlock = SessionYaml.asStringMap(session.get("result"));
        assertNotNull(resultBlock);
        assertEquals(30, num(resultBlock, "correct"));
        assertEquals(13, num(resultBlock, "wrong"));
        assertEquals(7, num(resultBlock, "unanswered"));
        assertEquals(60, num(resultBlock, "scorePercent"));
        assertEquals(false, resultBlock.get("passing"));
    }

    private void assertArtifact(Map<String, Object> artifact) {
        assertEquals(ExamSessionAnalyzer.ARTIFACT_SCHEMA_VERSION, num(artifact, "schemaVersion"));
        assertEquals(SESSION_ID, artifact.get("sessionId"));
        assertEquals("diagnostic", artifact.get("examId"));
        assertEquals(1, num(artifact, "examVersion"));
        assertEquals(SessionTime.timestamp(ANALYZE_TIME), artifact.get("analyzedAt"));

        assertNotNull(artifact.get("summary"));
        Map<String, Object> summary = SessionYaml.asStringMap(artifact.get("summary"));
        assertEquals(30, num(summary, "correct"));
        assertEquals(13, num(summary, "wrong"));
        assertEquals(7, num(summary, "unanswered"));
        assertEquals(60, num(summary, "scorePercent"));
        assertEquals(false, summary.get("passing"));

        List<?> correctQs = (List<?>) artifact.get("correctQuestions");
        List<?> wrongQs = (List<?>) artifact.get("wrongQuestions");
        List<?> unansweredQs = (List<?>) artifact.get("unansweredQuestions");
        assertEquals(30, correctQs.size());
        assertEquals(13, wrongQs.size());
        assertEquals(7, unansweredQs.size());

        assertTrue(correctQs.stream().noneMatch(wrongQs::contains));
        assertTrue(correctQs.stream().noneMatch(unansweredQs::contains));
        assertTrue(wrongQs.stream().noneMatch(unansweredQs::contains));

        Map<?, ?> bySection = (Map<?, ?>) artifact.get("bySection");
        assertNotNull(bySection);
        assertFalse(bySection.isEmpty());

        Map<?, ?> byTopic = (Map<?, ?>) artifact.get("byTopic");
        assertNotNull(byTopic);
        assertFalse(byTopic.isEmpty());

        assertFalse(artifact.containsKey("gaps"));
        assertFalse(artifact.containsKey("traps"));
        assertFalse(artifact.containsKey("knowledgeGaps"));
        assertFalse(artifact.containsKey("byDifficulty"));
    }

    private void assertConsistent(Map<String, Object> session, Map<String, Object> artifact) {
        Map<String, Object> resultBlock = SessionYaml.asStringMap(session.get("result"));
        Map<String, Object> summary = SessionYaml.asStringMap(artifact.get("summary"));
        assertEquals(num(resultBlock, "correct"), num(summary, "correct"));
        assertEquals(num(resultBlock, "wrong"), num(summary, "wrong"));
        assertEquals(num(resultBlock, "unanswered"), num(summary, "unanswered"));
        assertEquals(num(resultBlock, "scorePercent"), num(summary, "scorePercent"));
        assertEquals(resultBlock.get("passing"), summary.get("passing"));

        Map<?, ?> sessionBySection = (Map<?, ?>) resultBlock.get("bySection");
        Map<?, ?> artifactBySection = (Map<?, ?>) artifact.get("bySection");
        assertEquals(sessionBySection.keySet(), artifactBySection.keySet());
    }

    private static int num(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }
}