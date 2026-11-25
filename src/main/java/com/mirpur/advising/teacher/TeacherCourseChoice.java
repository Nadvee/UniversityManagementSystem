package com.mirpur.advising.teacher;

import com.mirpur.advising.shared.exception.ValidationException;

public final class TeacherCourseChoice {
    public enum Status {
        SELECTED, CONFIRMED, ASSIGNED, REJECTED
    }
    private Status status;
    private final String source;
    public TeacherCourseChoice(Status status, String source) {
        this.status = status;
        this.source = source;
    }
    public void confirm() {
        if (status != Status.SELECTED) throw new ValidationException("Save a selection first.");
        status = Status.CONFIRMED;
    }
    public void assign() {
        if (status != Status.CONFIRMED) throw new ValidationException("Only confirmed choices can be assigned.");
        status = Status.ASSIGNED;
    }
    public void reject() {
        if (status != Status.CONFIRMED) throw new ValidationException("Only confirmed requests can be rejected.");
        status = Status.REJECTED;
    }
    public Status status() {
        return status;
    }
    public String source() {
        return source;
    }
}
