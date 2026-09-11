package com.placido.certification.exams.session;

import java.nio.file.Path;

public record GradeResult(Outcome outcome, String sessionId, Path sessionFile) {

    public enum Outcome {
        GRADED,
        ALREADY_GRADED,
    }
}