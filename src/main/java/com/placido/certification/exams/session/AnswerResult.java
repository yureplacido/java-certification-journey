package com.placido.certification.exams.session;

import java.nio.file.Path;

public record AnswerResult(Outcome outcome, String sessionId, Path sessionFile, String questionId) {

    public enum Outcome {
        ANSWERED,
        UPDATED,
        UNCHANGED
    }
}
