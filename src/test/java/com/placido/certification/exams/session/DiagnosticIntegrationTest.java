package com.placido.certification.exams.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Registro único do diagnóstico do dia 1: executar explicitamente para reproduzir")
class DiagnosticIntegrationTest {

    private static final OffsetDateTime DAY1 =
            OffsetDateTime.of(2026, 9, 10, 14, 0, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime ANSWER_TIME =
            OffsetDateTime.of(2026, 9, 10, 14, 5, 0, 0, ZoneOffset.ofHours(-3));
    private static final OffsetDateTime FINISH_TIME =
            OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.ofHours(-3));

    private static final Map<String, String> USER_ANSWERS = new LinkedHashMap<>();
    static {
        USER_ANSWERS.put("Q01", "B");
        USER_ANSWERS.put("Q02", "A");
        USER_ANSWERS.put("Q03", "B");
        USER_ANSWERS.put("Q04", "B");
        USER_ANSWERS.put("Q05", "A");
        USER_ANSWERS.put("Q06", "C");
        USER_ANSWERS.put("Q07", "B");
        USER_ANSWERS.put("Q08", "A");
        // Q09: X (não respondida)
        USER_ANSWERS.put("Q10", "B");
        USER_ANSWERS.put("Q11", "B");
        USER_ANSWERS.put("Q12", "A");
        USER_ANSWERS.put("Q13", "B");
        USER_ANSWERS.put("Q14", "C");
        USER_ANSWERS.put("Q15", "C");
        USER_ANSWERS.put("Q16", "C");
        USER_ANSWERS.put("Q17", "B");
        USER_ANSWERS.put("Q18", "B");
        USER_ANSWERS.put("Q19", "B");
        USER_ANSWERS.put("Q20", "B");
        USER_ANSWERS.put("Q21", "A");
        USER_ANSWERS.put("Q22", "A");
        USER_ANSWERS.put("Q23", "A");
        USER_ANSWERS.put("Q24", "A");
        USER_ANSWERS.put("Q25", "A");
        // Q26: X (não respondida)
        USER_ANSWERS.put("Q27", "A");
        USER_ANSWERS.put("Q28", "B");
        USER_ANSWERS.put("Q29", "A");
        USER_ANSWERS.put("Q30", "B");
        USER_ANSWERS.put("Q31", "A");
        USER_ANSWERS.put("Q32", "B");
        USER_ANSWERS.put("Q33", "A");
        USER_ANSWERS.put("Q34", "B");
        USER_ANSWERS.put("Q35", "A");
        USER_ANSWERS.put("Q36", "A");
        // Q37: X (não respondida)
        USER_ANSWERS.put("Q38", "A");
        USER_ANSWERS.put("Q39", "A");
        USER_ANSWERS.put("Q40", "A");
        USER_ANSWERS.put("Q41", "A");
        // Q42: X (não respondida)
        USER_ANSWERS.put("Q43", "A");
        // Q44: X (não respondida)
        // Q45: X (não respondida)
        // Q46: X (não respondida)
        USER_ANSWERS.put("Q47", "B");
        USER_ANSWERS.put("Q48", "B");
        USER_ANSWERS.put("Q49", "B");
        USER_ANSWERS.put("Q50", "A");
    }

    @Test
    void registerDiagnosticAnswers() throws IOException {
        Path examsRoot = Path.of(System.getProperty("user.dir"), "exams");
        Path sessionsDir = examsRoot.resolve("sessions");
        Files.createDirectories(sessionsDir);

        ExamSessionStarter starter = new ExamSessionStarter(examsRoot, () -> DAY1);
        StartResult start = starter.start("diagnostic");

        if (start.outcome() == StartResult.Outcome.ALREADY_ACTIVE) {
            System.out.println("Sessão já existente: " + start.sessionId());
        } else {
            System.out.println("Sessão criada: " + start.sessionId());
        }
        String sessionId = start.sessionId();

        ExamSessionAnswerer answerer = new ExamSessionAnswerer(examsRoot, () -> ANSWER_TIME);

        for (Map.Entry<String, String> entry : USER_ANSWERS.entrySet()) {
            AnswerResult result = answerer.answer(sessionId, entry.getKey(), entry.getValue());
            System.out.println("  " + entry.getKey() + " → " + entry.getValue() + " [" + result.outcome() + "]");
        }

        ExamSessionFinisher finisher = new ExamSessionFinisher(examsRoot, () -> FINISH_TIME);
        FinishResult finish = finisher.finish(sessionId);

        Map<String, Object> data = SessionYaml.parse(finish.sessionFile());

        System.out.println();
        System.out.println("=== RESULTADO ===");
        System.out.println("sessionId: " + finish.sessionId());
        System.out.println("finishedAt: " + data.get("finishedAt"));
        System.out.println("totalQuestions: " + data.get("totalQuestions"));
        System.out.println("elapsedSeconds: " + data.get("elapsedSeconds"));
        System.out.println("remainingSeconds: " + data.get("remainingSeconds"));
        System.out.println("sessionFile: " + finish.sessionFile());

        assertEquals("FINISHED", data.get("status"));
        assertEquals(50, integer(data, "totalQuestions"));
        assertEquals(43, integer(data, "answeredQuestions"));
        assertEquals(43, integer(data, "visitedQuestions"));
        assertEquals(0, integer(data, "skippedQuestions"));
        assertEquals(0, integer(data, "flaggedQuestions"));
        assertNotNull(data.get("finishedAt"));
        assertNull(data.get("result"));

        Map<?, ?> questions = (Map<?, ?>) data.get("questions");
        assertEquals(43, questions.size());

        for (Map.Entry<String, String> entry : USER_ANSWERS.entrySet()) {
            Map<?, ?> q = (Map<?, ?>) questions.get(entry.getKey());
            assertNotNull(q, "questão " + entry.getKey() + " deve estar no mapa");
            assertEquals("ANSWERED", q.get("status"));
            assertEquals(entry.getValue(), q.get("answer"), "resposta de " + entry.getKey());
        }

        for (String unanswered : new String[]{"Q09", "Q26", "Q37", "Q42", "Q44", "Q45", "Q46"}) {
            assertTrue(!questions.containsKey(unanswered),
                    "questão " + unanswered + " não deve estar no mapa (não respondida)");
        }

        assertTrue(!Files.exists(finish.sessionFile().resolveSibling(
                finish.sessionFile().getFileName() + ".tmp")), "arquivo temporário deve ser limpo");
    }

    private static int integer(Map<String, Object> data, String key) {
        return ((Number) data.get(key)).intValue();
    }
}