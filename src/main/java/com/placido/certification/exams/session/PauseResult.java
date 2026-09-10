package com.placido.certification.exams.session;

import java.nio.file.Path;

public record PauseResult(Outcome outcome, String sessionId, Path sessionFile) {

    public enum Outcome {
        PAUSED,
        ALREADY_PAUSED,
        EXPIRED,
    }
}