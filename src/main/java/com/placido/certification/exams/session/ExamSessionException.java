package com.placido.certification.exams.session;

public final class ExamSessionException extends RuntimeException {

    public enum ErrorKind {
        EXAM_DEFINITION_NOT_FOUND,
        EXAM_DEFINITION_INVALID,
        SESSION_STORE_UNREADABLE,
        SESSION_STORE_INVALID,
        SESSION_NOT_FOUND,
        SESSION_NOT_RESUMABLE,
        SESSION_NOT_PAUSABLE,
        SESSION_NOT_FINISHABLE,
    }

    private final ErrorKind kind;

    public ExamSessionException(ErrorKind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ErrorKind kind() {
        return kind;
    }
}