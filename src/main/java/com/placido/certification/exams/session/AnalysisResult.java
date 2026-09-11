package com.placido.certification.exams.session;

import java.nio.file.Path;

public record AnalysisResult(Outcome outcome, String sessionId, Path sessionFile, Path artifactFile) {

    public enum Outcome {
        ANALYZED,
        ALREADY_ANALYZED,
    }
}