package com.placido.certification.exams.session;

import java.nio.file.Path;

public record FinishResult(Outcome outcome, String sessionId, Path sessionFile) {

    public enum Outcome {
        FINISHED,
        TIMED_OUT,
        ALREADY_FINISHED,
    }
}