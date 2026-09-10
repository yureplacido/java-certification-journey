package com.placido.certification.exams.session;

import java.nio.file.Path;

public record StartResult(Outcome outcome, String sessionId, Path sessionFile) {

    public enum Outcome {
        CREATED,
        ALREADY_ACTIVE,
    }

    public boolean created() {
        return outcome == Outcome.CREATED;
    }
}