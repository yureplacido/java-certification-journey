package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.EXAM_DEFINITION_VERSION_MISMATCH;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.PROGRESS_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.PROGRESS_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;
import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_UNREADABLE;
import static com.placido.certification.exams.session.ExamSessionStatus.ANALYZED;
import static com.placido.certification.exams.session.ExamSessionStatus.GRADED;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Progress {

    private static final Pattern SESSION_FILE =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}-.+-\\d{2,}\\.yaml");

    private final Path examsRoot;

    public Progress(Path examsRoot) {
        this.examsRoot = Objects.requireNonNull(examsRoot, "examsRoot");
    }

    public ProgressResult run() {
        Path progressFile = examsRoot.resolve("docs").resolve("progress.yaml");
        SplittedDocument split = splitDocument(readProgress(progressFile), progressFile);

        List<Attempt> attempts = new ArrayList<>();
        int scanned = 0;
        int ignored = 0;
        Path sessionsDir = examsRoot.resolve("sessions");
        if (Files.isDirectory(sessionsDir)) {
            try (Stream<Path> stream = Files.list(sessionsDir)) {
                List<Path> files = stream
                        .filter(path -> SESSION_FILE.matcher(path.getFileName().toString()).matches())
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
                for (Path file : files) {
                    scanned++;
                    Attempt attempt = buildAttempt(file);
                    if (attempt == null) {
                        ignored++;
                    } else {
                        attempts.add(attempt);
                    }
                }
            } catch (IOException e) {
                throw unreadable("Não foi possível listar sessões em " + sessionsDir + ": " + e.getMessage());
            }
        }

        LinkedHashMap<String, List<Attempt>> byExam = new LinkedHashMap<>();
        for (String examId : split.humanExamIds()) {
            byExam.put(examId, new ArrayList<>());
        }
        Set<String> newExamIds = new TreeSet<>();
        for (Attempt attempt : attempts) {
            if (!byExam.containsKey(attempt.examId())) {
                newExamIds.add(attempt.examId());
            }
        }
        for (String examId : newExamIds) {
            byExam.put(examId, new ArrayList<>());
        }
        for (Attempt attempt : attempts) {
            byExam.get(attempt.examId()).add(attempt);
        }
        for (List<Attempt> list : byExam.values()) {
            list.sort(Comparator.comparing(Attempt::sessionId));
        }

        Map<String, Object> humanExam = split.humanExams();
        Map<String, Map<String, Object>> derivedExams = new LinkedHashMap<>();
        for (Map.Entry<String, List<Attempt>> entry : byExam.entrySet()) {
            derivedExams.put(entry.getKey(), buildExamEntry(entry.getKey(),
                    humanExam.get(entry.getKey()), entry.getValue()));
        }
        Map<String, Object> exames = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : derivedExams.entrySet()) {
            exames.put(entry.getKey(), entry.getValue());
        }

        String body = indentByTwo(SessionYaml.dump(exames));
        String output = merge(split, body);
        writeAtomically(progressFile, output);

        return new ProgressResult(progressFile, scanned, attempts.size(), ignored);
    }

    private Attempt buildAttempt(Path file) {
        Map<String, Object> data = SessionYaml.parse(file);

        String storedSessionId = SessionParsing.requireString(data, "sessionId", file);
        String fileName = file.getFileName().toString();
        String fileBase = fileName.substring(0, fileName.length() - ".yaml".length());
        if (!storedSessionId.equals(fileBase)) {
            throw SessionParsing.invalidStore(file, "sessionId interno diverge do nome do arquivo: " + storedSessionId);
        }
        String examId = SessionParsing.requireString(data, "examId", file);
        int examVersion = SessionParsing.requireInt(data, "examVersion", file);
        ExamSessionStatus status = SessionParsing.parseStatus(data, file);
        String currentQuestion = SessionParsing.requireString(data, "currentQuestion", file);
        if (!SessionParsing.CANONICAL_QUESTION_ID.matcher(currentQuestion).matches()) {
            throw SessionParsing.invalidStore(file, "currentQuestion não é um questionId canônico: " + currentQuestion);
        }
        SessionParsing.requireInt(data, "elapsedSeconds", file);
        SessionParsing.requireInt(data, "remainingSeconds", file);
        int totalQuestions = SessionParsing.requireInt(data, "totalQuestions", file);
        SessionParsing.requireInt(data, "answeredQuestions", file);
        SessionParsing.requireInt(data, "skippedQuestions", file);
        SessionParsing.requireInt(data, "visitedQuestions", file);
        SessionParsing.requireInt(data, "flaggedQuestions", file);
        SessionParsing.requireString(data, "startedAt", file);
        SessionParsing.requireString(data, "lastActivityAt", file);
        Map<String, Map<String, Object>> questions = SessionParsing.readQuestions(data, file);

        if (status != GRADED && status != ANALYZED) {
            return null;
        }
        Map<String, Object> result = SessionYaml.asStringMap(data.get("result"));
        if (result == null) {
            return null;
        }

        ExamDefinition definition = ExamDefinition.load(
                examsRoot.resolve(examId).resolve("definition.yaml"), examId);
        if (definition.version() != examVersion) {
            throw new ExamSessionException(EXAM_DEFINITION_VERSION_MISMATCH,
                    "Sessão " + storedSessionId + " referencia definition v" + examVersion
                            + " mas definition.yaml está na v" + definition.version());
        }
        if (totalQuestions != definition.totalQuestions()) {
            throw SessionParsing.invalidStore(file, "totalQuestions (" + totalQuestions
                    + ") diverge da definition (" + definition.totalQuestions() + ")");
        }

        SessionComputation computation = SessionComputation.compute(questions, definition);
        computation.requireConsistentResult(result, file);
        String gradedAt = SessionParsing.requireString(result, "gradedAt", file);

        String analyzedAt = null;
        if (status == ANALYZED) {
            Path artifact = artifactFile(storedSessionId);
            if (Files.isRegularFile(artifact)) {
                Map<String, Object> artifactData = SessionYaml.parse(artifact);
                Map<String, Object> artifactByTopic = SessionYaml.asStringMap(artifactData.get("byTopic"));
                if (artifactByTopic == null) {
                    throw SessionParsing.invalidStore(artifact, "byTopic ausente ou inválido no artefato");
                }
                if (!artifactByTopic.equals(computation.byTopic())) {
                    throw SessionParsing.invalidStore(
                            artifact, "byTopic do artefato diverge da recomputação da sessão");
                }
                analyzedAt = SessionParsing.requireString(artifactData, "analyzedAt", artifact);
            }
        }

        return new Attempt(storedSessionId, examId, examVersion, status, gradedAt, analyzedAt, computation);
    }

    private Map<String, Object> buildExamEntry(String examId, Object rawHuman,
            List<Attempt> attempts) {
        Map<String, Object> human = SessionYaml.asStringMap(rawHuman);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("status", humanString(human, "status", "pending"));
        entry.put("relatorio", humanString(human, "relatorio", "exams/" + examId + "/"));

        if (attempts.isEmpty()) {
            entry.put("data", null);
            entry.put("nota", null);
        } else {
            Attempt latest = attempts.get(attempts.size() - 1);
            entry.put("data", latest.gradedAt());
            entry.put("nota", latest.computation().scorePercent());
        }

        List<Map<String, Object>> tentativas = new ArrayList<>();
        for (Attempt attempt : attempts) {
            tentativas.add(attemptMap(attempt));
        }
        entry.put("tentativas", tentativas);

        if (attempts.isEmpty()) {
            entry.put("melhor", null);
        } else {
            Attempt best = attempts.get(0);
            for (Attempt attempt : attempts) {
                if (attempt.computation().scorePercent() > best.computation().scorePercent()) {
                    best = attempt;
                }
            }
            Map<String, Object> melhor = new LinkedHashMap<>();
            melhor.put("sessionId", best.sessionId());
            melhor.put("scorePercent", best.computation().scorePercent());
            melhor.put("passing", best.computation().passing());
            entry.put("melhor", melhor);
        }

        entry.put("sections", aggregateSections(attempts));
        entry.put("topics", aggregateTopics(attempts));
        return entry;
    }

    private static Map<String, Object> attemptMap(Attempt attempt) {
        SessionComputation computation = attempt.computation();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionId", attempt.sessionId());
        map.put("examVersion", attempt.examVersion());
        map.put("status", attempt.status().name());
        map.put("gradedAt", attempt.gradedAt());
        if (attempt.analyzedAt() != null) {
            map.put("analyzedAt", attempt.analyzedAt());
        }
        map.put("correct", computation.correct());
        map.put("wrong", computation.wrong());
        map.put("unanswered", computation.unanswered());
        map.put("scorePercent", computation.scorePercent());
        map.put("passing", computation.passing());
        map.put("sections", sectionMap(computation.bySection()));
        map.put("topics", computation.byTopic());
        return map;
    }

    private static Map<Integer, Map<String, Object>> sectionMap(Map<Integer, Map<String, Object>> raw) {
        return reorderBySectionKey(raw);
    }

    private static <T> Map<Integer, T> reorderBySectionKey(Map<Integer, T> raw) {
        List<Integer> keys = new ArrayList<>(raw.keySet());
        keys.sort(Comparator.naturalOrder());
        Map<Integer, T> result = new LinkedHashMap<>();
        for (Integer key : keys) {
            result.put(key, raw.get(key));
        }
        return result;
    }

    private static Map<Integer, Map<String, Object>> aggregateSections(List<Attempt> attempts) {
        Map<Integer, int[]> buckets = new LinkedHashMap<>();
        for (Attempt attempt : attempts) {
            for (Map.Entry<Integer, Map<String, Object>> entry : attempt.computation().bySection().entrySet()) {
                int[] bucket = buckets.computeIfAbsent(entry.getKey(), k -> new int[2]);
                bucket[0] += num(entry.getValue(), "correct");
                bucket[1] += num(entry.getValue(), "total");
            }
        }
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, int[]> entry : reorderBySectionKey(buckets).entrySet()) {
            result.put(entry.getKey(), countMap(entry.getValue()));
        }
        return result;
    }

    private static Map<String, Map<String, Object>> aggregateTopics(List<Attempt> attempts) {
        Map<String, int[]> buckets = new LinkedHashMap<>();
        for (Attempt attempt : attempts) {
            for (Map.Entry<String, Map<String, Object>> entry : attempt.computation().byTopic().entrySet()) {
                int[] bucket = buckets.computeIfAbsent(entry.getKey(), k -> new int[2]);
                bucket[0] += num(entry.getValue(), "correct");
                bucket[1] += num(entry.getValue(), "total");
            }
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> entry : buckets.entrySet()) {
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

    private static int num(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }

    private static String humanString(Map<String, Object> human, String key, String fallback) {
        Object value = human == null ? null : human.get(key);
        return value instanceof String s && !s.isBlank() ? s : fallback;
    }

    private Path artifactFile(String sessionId) {
        return examsRoot.resolve("docs").resolve("study-log").resolve(sessionId + ".analysis.yaml");
    }

    private String readProgress(Path progressFile) {
        try {
            return Files.readString(progressFile);
        } catch (IOException e) {
            throw unreadable("Não foi possível ler " + progressFile + ": " + e.getMessage());
        }
    }

    private SplittedDocument splitDocument(String text, Path progressFile) {
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\\n", -1)));
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        int headerIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("exames:")) {
                headerIdx = i;
                break;
            }
        }
        if (headerIdx < 0) {
            throw new ExamSessionException(PROGRESS_INVALID,
                    "docs/progress.yaml sem bloco 'exames:' — não é possível derivar progresso: " + progressFile);
        }

        List<String> separator = new ArrayList<>();
        int i = headerIdx + 1;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line.isBlank()) {
                separator.add(line);
                i++;
            } else if (!line.isEmpty() && Character.isWhitespace(line.charAt(0))) {
                separator.clear();
                i++;
            } else {
                break;
            }
        }
        List<String> tail = new ArrayList<>(lines.subList(i, lines.size()));

        Map<String, Object> parsed;
        try {
            parsed = SessionYaml.parse(progressFile);
        } catch (ExamSessionException e) {
            if (e.kind() == SESSION_STORE_INVALID) {
                throw new ExamSessionException(PROGRESS_INVALID,
                        "docs/progress.yaml inválido: " + e.getMessage());
            }
            throw unreadable("docs/progress.yaml ilegível: " + e.getMessage());
        }
        Map<String, Object> exames = SessionYaml.asStringMap(parsed.get("exames"));
        if (exames == null) {
            throw new ExamSessionException(PROGRESS_INVALID,
                    "docs/progress.yaml sem bloco 'exames:' válido: " + progressFile);
        }
        return new SplittedDocument(lines.subList(0, headerIdx + 1), separator, tail, exames);
    }

    private static String indentByTwo(String yaml) {
        String trimmed = yaml.endsWith("\n") ? yaml.substring(0, yaml.length() - "\n".length()) : yaml;
        StringBuilder result = new StringBuilder(trimmed.length() + 64);
        for (String line : trimmed.split("\\n", -1)) {
            result.append("  ").append(line).append('\n');
        }
        return result.toString();
    }

    private static String merge(SplittedDocument split, String examesBody) {
        StringBuilder out = new StringBuilder();
        for (String line : split.head()) {
            out.append(line).append('\n');
        }
        out.append(examesBody);
        for (String line : split.separator()) {
            out.append(line).append('\n');
        }
        for (String line : split.tail()) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private void writeAtomically(Path progressFile, String output) {
        Path temp = progressFile.resolveSibling(progressFile.getFileName() + ".tmp");
        try {
            Files.writeString(temp, output);
            try {
                Files.move(temp, progressFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException fallback) {
                Files.move(temp, progressFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(temp);
            throw unreadable("Não foi possível escrever " + progressFile + ": " + e.getMessage());
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup do arquivo temporário
        }
    }

    private static ExamSessionException unreadable(String detail) {
        return new ExamSessionException(PROGRESS_UNREADABLE, detail);
    }

    private record SplittedDocument(List<String> head, List<String> separator, List<String> tail,
            Map<String, Object> humanExams) {
        List<String> humanExamIds() {
            List<String> ids = new ArrayList<>(humanExams.keySet());
            return ids;
        }
    }

    private record Attempt(String sessionId, String examId, int examVersion, ExamSessionStatus status,
            String gradedAt, String analyzedAt, SessionComputation computation) {
    }
}