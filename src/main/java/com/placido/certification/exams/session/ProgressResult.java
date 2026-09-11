package com.placido.certification.exams.session;

import java.nio.file.Path;

public record ProgressResult(Path progressFile, int sessionsScanned, int attemptsWritten,
        int sessionsIgnored) {
}