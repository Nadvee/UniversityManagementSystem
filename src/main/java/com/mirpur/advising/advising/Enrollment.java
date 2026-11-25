package com.mirpur.advising.advising;

import com.mirpur.advising.shared.exception.ValidationException;

public final class Enrollment {
    private String status;
    private String dropReason;
    public Enrollment(String status) {
        this.status = status;
    }
    public void drop(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 1000) throw new ValidationException("A reason is required (maximum 1,000 characters).");
        if ( !"CONFIRMED".equals(status)) throw new ValidationException("Only your confirmed courses can be dropped.");
        dropReason = reason;
        status = "DROPPED";
    }
    public String status() {
        return status;
    }
    public String dropReason() {
        return dropReason;
    }
}
