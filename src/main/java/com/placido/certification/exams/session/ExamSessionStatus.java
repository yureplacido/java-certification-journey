package com.placido.certification.exams.session;

public enum ExamSessionStatus {

    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    FINISHED,
    GRADED,
    ANALYZED,
    ABANDONED;

    public boolean isActive() {
        return this == IN_PROGRESS || this == PAUSED;
    }
}