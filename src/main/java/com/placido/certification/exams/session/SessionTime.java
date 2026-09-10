package com.placido.certification.exams.session;

import static com.placido.certification.exams.session.ExamSessionException.ErrorKind.SESSION_STORE_INVALID;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

final class SessionTime {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private SessionTime() {
    }

    static String timestamp(OffsetDateTime instant) {
        return instant.format(TIMESTAMP);
    }

    static Accounting consumeFromLastActivity(String lastActivityAt, int elapsedSeconds,
            int remainingSeconds, OffsetDateTime now, Path file) {
        long gap = activeGapSeconds(lastActivityAt, now, file);
        long consumed = Math.min(gap, (long) remainingSeconds);
        int newRemaining = remainingSeconds - (int) consumed;
        return new Accounting(elapsedSeconds + (int) consumed, newRemaining);
    }

    static long activeGapSeconds(String lastActivityAt, OffsetDateTime now, Path file) {
        OffsetDateTime last = parse(lastActivityAt, file);
        return Math.max(0, ChronoUnit.SECONDS.between(last, now));
    }

    private static OffsetDateTime parse(String lastActivityAt, Path file) {
        try {
            return OffsetDateTime.parse(lastActivityAt, TIMESTAMP);
        } catch (DateTimeParseException primary) {
            try {
                return OffsetDateTime.parse(lastActivityAt);
            } catch (DateTimeParseException secondary) {
                throw new ExamSessionException(SESSION_STORE_INVALID,
                        "Sessão inválida em " + file + ": lastActivityAt inválido: " + lastActivityAt);
            }
        }
    }

    record Accounting(int elapsedSeconds, int remainingSeconds) {
    }
}