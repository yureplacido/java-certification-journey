package com.placido.certification.exams.session;

import java.util.LinkedHashMap;
import java.util.Map;

public record ExamSession(
        int schemaVersion,
        String sessionId,
        String examId,
        int examVersion,
        ExamSessionStatus status,
        String startedAt,
        String lastActivityAt,
        String finishedAt,
        String currentQuestion,
        int elapsedSeconds,
        int remainingSeconds,
        int totalQuestions,
        int answeredQuestions,
        int skippedQuestions,
        int visitedQuestions,
        int flaggedQuestions,
        Map<String, Map<String, Object>> questions) {

    public Map<String, Object> toYamlMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("schemaVersion", schemaVersion);
        data.put("sessionId", sessionId);
        data.put("examId", examId);
        data.put("examVersion", examVersion);
        data.put("status", status.name());
        data.put("startedAt", startedAt);
        data.put("lastActivityAt", lastActivityAt);
        if (finishedAt != null) {
            data.put("finishedAt", finishedAt);
        }
        data.put("currentQuestion", currentQuestion);
        data.put("elapsedSeconds", elapsedSeconds);
        data.put("remainingSeconds", remainingSeconds);
        data.put("totalQuestions", totalQuestions);
        data.put("answeredQuestions", answeredQuestions);
        data.put("skippedQuestions", skippedQuestions);
        data.put("visitedQuestions", visitedQuestions);
        data.put("flaggedQuestions", flaggedQuestions);
        data.put("questions", questions);
        return data;
    }

    public String toYamlText() {
        return SessionYaml.dump(toYamlMap());
    }
}