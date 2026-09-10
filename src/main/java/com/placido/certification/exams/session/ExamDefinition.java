package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_NOT_FOUND;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public record ExamDefinition(
        String examId,
        int version,
        String examType,
        String javaVersion,
        int totalQuestions,
        int durationSeconds,
        int passingScore,
        List<String> questionOrder,
        Map<String, Map<String, Object>> questions,
        Map<String, Map<String, Object>> answers) {

    private static final Pattern CANONICAL_QUESTION_ID = Pattern.compile("Q\\d{2,}");
    private static final Set<String> EXAM_TYPES = Set.of("diagnostic", "mock", "final");

    public static ExamDefinition load(Path definitionFile, String requestedExamId) {
        if (!Files.isRegularFile(definitionFile)) {
            throw new ExamSessionException(EXAM_DEFINITION_NOT_FOUND,
                    "Exam Definition não encontrada: " + definitionFile);
        }
        String content;
        try {
            content = Files.readString(definitionFile);
        } catch (IOException e) {
            throw invalid("Não foi possível ler " + definitionFile + ": " + e.getMessage());
        }
        Map<String, Object> document = parseDocument(definitionFile, content);

        String examId = requiredString(document, "examId");
        if (!examId.equals(requestedExamId)) {
            throw invalid("examId do definition.yaml (" + examId + ") diverge do pedido (" + requestedExamId + ")");
        }
        int version = positiveInt(document, "version");
        String examType = requiredString(document, "examType");
        if (!EXAM_TYPES.contains(examType)) {
            throw invalid("examType inválido: '" + examType + "' (esperado diagnostic | mock | final)");
        }
        String javaVersion = requiredString(document, "javaVersion");
        int totalQuestions = positiveInt(document, "totalQuestions");
        int durationSeconds = positiveInt(document, "durationSeconds");
        int passingScore = intInRange(document, "passingScore", 0, 100);

        List<String> questionOrder = questionOrder(document, totalQuestions);

        Map<String, Object> rawQuestions = requiredMap(document, "questions");
        Map<String, Object> rawAnswers = requiredMap(document, "answers");
        Map<String, Map<String, Object>> questions = entriesByOrder(rawQuestions, "questions", questionOrder);
        Map<String, Map<String, Object>> answers = entriesByOrder(rawAnswers, "answers", questionOrder);

        for (String id : questionOrder) {
            Map<String, Object> meta = questions.get(id);
            requiredString(meta, "topic");
            requiredString(meta, "difficulty");
            if (!(meta.get("section") instanceof Number)) {
                throw invalid("questions['" + id + "'].section deve ser um inteiro");
            }
            Map<String, Object> answer = answers.get(id);
            requiredString(answer, "correctOption");
            requiredString(answer, "format");
        }

        return new ExamDefinition(examId, version, examType, javaVersion, totalQuestions,
                durationSeconds, passingScore, List.copyOf(questionOrder), questions, answers);
    }

    private static Map<String, Object> parseDocument(Path file, String content) {
        Object document;
        try {
            document = new Yaml(new SafeConstructor(new LoaderOptions())).load(content);
        } catch (RuntimeException e) {
            throw invalid("YAML inválido em " + file + ": " + e.getMessage());
        }
        Map<String, Object> map = SessionYaml.asStringMap(document);
        if (map == null) {
            throw invalid("definition.yaml não é um mapa YAML: " + file);
        }
        return map;
    }

    private static ExamSessionException invalid(String detail) {
        return new ExamSessionException(EXAM_DEFINITION_INVALID, detail);
    }

    private static String requiredString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: '" + key + "'");
        }
        return s;
    }

    private static int asInt(Object value, String key) {
        if (!(value instanceof Number number)) {
            throw invalid("Campo '" + key + "' deve ser um inteiro");
        }
        return number.intValue();
    }

    private static int positiveInt(Map<String, Object> doc, String key) {
        int value = asInt(doc.get(key), key);
        if (value <= 0) {
            throw invalid("Campo '" + key + "' deve ser > 0, mas é " + value);
        }
        return value;
    }

    private static int intInRange(Map<String, Object> doc, String key, int min, int max) {
        int value = asInt(doc.get(key), key);
        if (value < min || value > max) {
            throw invalid("Campo '" + key + "' deve estar em [" + min + ", " + max + "], mas é " + value);
        }
        return value;
    }

    private static List<String> questionOrder(Map<String, Object> doc, int totalQuestions) {
        Object raw = doc.get("questionOrder");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw invalid("Campo obrigatório ausente ou inválido: 'questionOrder'");
        }
        List<String> order = new java.util.ArrayList<>(list.size());
        Set<String> seen = new HashSet<>();
        int width = -1;
        for (Object item : list) {
            if (!(item instanceof String id) || !CANONICAL_QUESTION_ID.matcher(id).matches()) {
                throw invalid("questionOrder contém identificador não canônico: " + item);
            }
            if (!seen.add(id)) {
                throw invalid("questionOrder contém identificador duplicado: " + id);
            }
            if (width == -1) {
                width = id.length();
            } else if (id.length() != width) {
                throw invalid("questionOrder mistura larguras de identificador (zero-padded): " + id);
            }
            order.add(id);
        }
        if (order.size() != totalQuestions) {
            throw invalid("totalQuestions (" + totalQuestions + ") diverge de questionOrder ("
                    + order.size() + " questões)");
        }
        return order;
    }

    private static Map<String, Object> requiredMap(Map<String, Object> doc, String key) {
        Map<String, Object> map = SessionYaml.asStringMap(doc.get(key));
        if (map == null) {
            throw invalid("Campo obrigatório ausente ou inválido: '" + key + "'");
        }
        return map;
    }

    private static Map<String, Map<String, Object>> entriesByOrder(
            Map<String, Object> source, String mapName, List<String> order) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        Set<String> expected = new HashSet<>(order);
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String id = entry.getKey();
            if (!expected.contains(id)) {
                throw invalid(mapName + " traz questão fora de questionOrder: " + id);
            }
            Map<String, Object> meta = SessionYaml.asStringMap(entry.getValue());
            if (meta == null) {
                throw invalid(mapName + "['" + id + "'] deve ser um mapa");
            }
            result.put(id, Map.copyOf(meta));
        }
        for (String id : order) {
            if (!result.containsKey(id)) {
                throw invalid(mapName + " não traz entrada para '" + id + "'");
            }
        }
        return result;
    }
}