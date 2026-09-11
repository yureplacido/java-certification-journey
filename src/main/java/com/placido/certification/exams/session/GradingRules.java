package com.placido.certification.exams.session;

import java.util.Map;

final class GradingRules {

    private GradingRules() {
    }

    static QuestionVerdict classify(Map<String, Object> question, String correctOption) {
        if (question == null || !"ANSWERED".equals(question.get("status"))) {
            return QuestionVerdict.UNANSWERED;
        }
        Object answer = question.get("answer");
        if (answer == null || String.valueOf(answer).isBlank()) {
            return QuestionVerdict.UNANSWERED;
        }
        return String.valueOf(answer).equals(correctOption)
                ? QuestionVerdict.CORRECT
                : QuestionVerdict.WRONG;
    }

    static int scorePercent(int correct, int totalQuestions) {
        return (int) Math.round(100.0 * correct / totalQuestions);
    }
}