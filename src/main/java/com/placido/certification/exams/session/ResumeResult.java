package com.placido.certification.exams.session;

import java.nio.file.Path;

public record ResumeResult(Outcome outcome, String sessionId, Path sessionFile) {

    public enum Outcome {
        RESUMED,
        ALREADY_ACTIVE,
        EXPIRED,
    }
}