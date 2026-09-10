package com.placido.certification.exams.session;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

final class SessionYaml {

    private SessionYaml() {
    }

    static Map<String, Object> parse(Path file) {
        String content;
        try {
            content = Files.readString(file, UTF_8);
        } catch (IOException e) {
            throw new ExamSessionException(ExamSessionException.ErrorKind.SESSION_STORE_UNREADABLE,
                    "Não foi possível ler o arquivo de sessão " + file + ": " + e.getMessage());
        }
        Object document;
        try {
            document = new Yaml(new SafeConstructor(new LoaderOptions())).load(content);
        } catch (RuntimeException e) {
            throw new ExamSessionException(ExamSessionException.ErrorKind.SESSION_STORE_INVALID,
                    "Erro de YAML no arquivo " + file + ": " + e.getMessage());
        }
        Map<String, Object> map = asStringMap(document);
        if (map == null) {
            throw new ExamSessionException(ExamSessionException.ErrorKind.SESSION_STORE_INVALID,
                    "Arquivo de sessão não é um mapa YAML: " + file);
        }
        return map;
    }

    static Map<String, Object> asStringMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    static String dump(Map<String, Object> data) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setSplitLines(false);
        return new Yaml(new QuotedStringsRepresenter(options), options).dump(data);
    }

    private static final class QuotedStringsRepresenter extends Representer {

        QuotedStringsRepresenter(DumperOptions options) {
            super(options);
        }

        @Override
        protected Node representScalar(Tag tag, String value, ScalarStyle style) {
            if (tag == Tag.STR) {
                style = ScalarStyle.DOUBLE_QUOTED;
            }
            return super.representScalar(tag, value, style);
        }
    }
}