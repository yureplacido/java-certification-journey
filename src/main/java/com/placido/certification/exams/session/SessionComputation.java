package com.placido.certification.exams.session;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final record SessionComputation(int correct, int wrong, int unanswered, int scorePercent,
        boolean passing, List<String> correctQuestions, List<String> wrongQuestions,
        List<String> unansweredQuestions, Map<Integer, Map<String, Object>> bySection,
        Map<String, Map<String, Object>> byTopic) {

    static SessionComputation compute(Map<String, Map<String, Object>> questions, ExamDefinition definition) {
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        List<String> correctQuestions = new ArrayList<>();
        List<String> wrongQuestions = new ArrayList<>();
        List<String> unansweredQuestions = new ArrayList<>();
        Map<Integer, int[]> sectionBuckets = new LinkedHashMap<>();
        Map<String, int[]> topicBuckets = new LinkedHashMap<>();

        for (String id : definition.questionOrder()) {
            QuestionVerdict verdict = GradingRules.classify(questions.get(id),
                    String.valueOf(definition.answers().get(id).get("correctOption")));
            switch (verdict) {
                case CORRECT -> {
                    correct++;
                    correctQuestions.add(id);
                }
                case WRONG -> {
                    wrong++;
                    wrongQuestions.add(id);
                }
                case UNANSWERED -> {
                    unanswered++;
                    unansweredQuestions.add(id);
                }
            }
            int section = sectionOf(definition, id);
            int[] sectionCount = sectionBuckets.computeIfAbsent(section, k -> new int[2]);
            sectionCount[1]++;
            if (verdict == QuestionVerdict.CORRECT) {
                sectionCount[0]++;
            }
            String topic = String.valueOf(definition.questions().get(id).get("topic"));
            int[] topicCount = topicBuckets.computeIfAbsent(topic, k -> new int[2]);
            topicCount[1]++;
            if (verdict == QuestionVerdict.CORRECT) {
                topicCount[0]++;
            }
        }

        int scorePercent = GradingRules.scorePercent(correct, definition.totalQuestions());
        boolean passing = scorePercent >= definition.passingScore();

        return new SessionComputation(correct, wrong, unanswered, scorePercent, passing,
                List.copyOf(correctQuestions), List.copyOf(wrongQuestions),
                List.copyOf(unansweredQuestions), toSectionMap(sectionBuckets), toTopicMap(topicBuckets));
    }

    Map<String, Object> summaryMap() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("correct", correct);
        summary.put("wrong", wrong);
        summary.put("unanswered", unanswered);
        summary.put("scorePercent", scorePercent);
        summary.put("passing", passing);
        return summary;
    }

    void requireConsistentResult(Map<String, Object> result, Path file) {
        requireResultInt(result, "correct", correct, file);
        requireResultInt(result, "wrong", wrong, file);
        requireResultInt(result, "unanswered", unanswered, file);
        requireResultInt(result, "scorePercent", scorePercent, file);
        Object storedPassing = result.get("passing");
        if (!(storedPassing instanceof Boolean) || passing != (Boolean) storedPassing) {
            throw SessionParsing.invalidStore(file, "result.passing inconsistente com a derivação: " + storedPassing);
        }
        requireConsistentBySection(bySection, result.get("bySection"), file);
    }

    private static void requireResultInt(Map<String, Object> result, String key, int expected, Path file) {
        Object value = result.get(key);
        if (!(value instanceof Number number) || number.intValue() != expected) {
            throw SessionParsing.invalidStore(
                    file, "result." + key + " inconsistente com a derivação: " + value);
        }
    }

    private static void requireConsistentBySection(Map<Integer, Map<String, Object>> derived,
            Object rawStored, Path file) {
        Map<String, Object> stored = SessionYaml.asStringMap(rawStored);
        if (stored == null) {
            throw SessionParsing.invalidStore(file, "result.bySection ausente ou inválido");
        }
        if (stored.size() != derived.size()) {
            throw SessionParsing.invalidStore(file, "result.bySection com " + stored.size()
                    + " seções, derivação tem " + derived.size());
        }
        for (Map.Entry<String, Object> entry : stored.entrySet()) {
            int section = Integer.parseInt(entry.getKey());
            Map<String, Object> expected = derived.get(section);
            if (expected == null) {
                throw SessionParsing.invalidStore(file, "result.bySection[" + section + "] não existe na derivação");
            }
            Map<String, Object> actual = SessionYaml.asStringMap(entry.getValue());
            if (actual == null) {
                throw SessionParsing.invalidStore(file, "result.bySection[" + section + "] deve ser um mapa");
            }
            requireResultInt(actual, "correct", ((Number) expected.get("correct")).intValue(), file);
            requireResultInt(actual, "total", ((Number) expected.get("total")).intValue(), file);
        }
    }

    private static Map<Integer, Map<String, Object>> toSectionMap(Map<Integer, int[]> raw) {
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, int[]> entry : raw.entrySet()) {
            result.put(entry.getKey(), countMap(entry.getValue()));
        }
        return result;
    }

    private static Map<String, Map<String, Object>> toTopicMap(Map<String, int[]> raw) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : raw.entrySet()) {
            result.put(entry.getKey(), countMap(entry.getValue()));
        }
        return result;
    }

    private static Map<String, Object> countMap(int[] buckets) {
        Map<String, Object> count = new LinkedHashMap<>();
        count.put("correct", buckets[0]);
        count.put("total", buckets[1]);
        return count;
    }

    private static int sectionOf(ExamDefinition definition, String id) {
        return ((Number) definition.questions().get(id).get("section")).intValue();
    }
}