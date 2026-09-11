package com.placido.certification.exams.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Grade exclusivo do diagnóstico do dia 1: executar explicitamente para reproduzir")
class DiagnosticGradeTest {

    private static final OffsetDateTime GRADE_TIME =
            OffsetDateTime.of(2026, 9, 11, 10, 0, 0, 0, ZoneOffset.ofHours(-3));

    @Test
    void gradeDiagnosticDay1() {
        Path examsRoot = Path.of(System.getProperty("user.dir"), "exams");
        GradeResult grade = new ExamSessionGrader(examsRoot, () -> GRADE_TIME)
                .grade("2026-09-10-diagnostic-01");

        Map<String, Object> data = SessionYaml.parse(grade.sessionFile());
        Map<String, Object> result = SessionYaml.asStringMap(data.get("result"));
        assertResultResult(result);
        assertSessionSanity(data);

        System.out.println("=== GRADE ===");
        System.out.println("outcome: " + grade.outcome());
        System.out.println("gradedAt: " + result.get("gradedAt"));
        System.out.println("correct: " + result.get("correct"));
        System.out.println("wrong: " + result.get("wrong"));
        System.out.println("unanswered: " + result.get("unanswered"));
        System.out.println("scorePercent: " + result.get("scorePercent"));
        System.out.println("passing: " + result.get("passing"));
        System.out.println("bySection: " + result.get("bySection"));
        System.out.println("status: " + data.get("status"));
        System.out.println("sessionFile: " + grade.sessionFile());
    }

    private void assertResultResult(Map<String, Object> result) {
        assertEquals(SessionTime.timestamp(GRADE_TIME), result.get("gradedAt"));
        int correct = ((Number) result.get("correct")).intValue();
        int wrong = ((Number) result.get("wrong")).intValue();
        int unanswered = ((Number) result.get("unanswered")).intValue();
        assertEquals(50, correct + wrong + unanswered);
        assertEquals(43, correct + wrong);
        int scorePercent = ((Number) result.get("scorePercent")).intValue();
        assertEquals((int) Math.round(100.0 * correct / 50), scorePercent);
        assertTrue((Boolean) result.get("passing") == (scorePercent >= 68));
    }

    private void assertSessionSanity(Map<String, Object> data) {
        assertEquals("GRADED", data.get("status"));
        assertEquals(43, ((Number) data.get("answeredQuestions")).intValue());
        assertEquals(0, ((Number) data.get("skippedQuestions")).intValue());
        assertEquals(43, ((Number) data.get("visitedQuestions")).intValue());
        assertEquals(0, ((Number) data.get("flaggedQuestions")).intValue());
        assertEquals("Q01", data.get("currentQuestion"));
        assertEquals(3600, ((Number) data.get("elapsedSeconds")).intValue());
        assertEquals(3600, ((Number) data.get("remainingSeconds")).intValue());

        Map<?, ?> questions = (Map<?, ?>) data.get("questions");
        assertEquals(43, questions.size());
        assertFalse(questions.containsKey("Q09"));
        assertFalse(questions.containsKey("Q26"));
        assertFalse(questions.containsKey("Q37"));
        assertFalse(questions.containsKey("Q42"));
        assertFalse(questions.containsKey("Q44"));
        assertFalse(questions.containsKey("Q45"));
        assertFalse(questions.containsKey("Q46"));
    }
}