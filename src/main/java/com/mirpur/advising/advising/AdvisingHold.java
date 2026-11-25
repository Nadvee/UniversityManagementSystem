package com.mirpur.advising.advising;

import com.mirpur.advising.shared.exception.ValidationException;
import java.time.*;

public final class AdvisingHold {
    public enum Status {
        HELD, CONFIRMED, EXPIRED, REMOVED
    }
    private Status status;
    private final LocalDateTime expiresAt;
    public AdvisingHold(Status status, LocalDateTime expiresAt) {
        this.status = status;
        this.expiresAt = expiresAt;
    }
    public static AdvisingHold start(Clock clock, int holdSeconds) {
        return new AdvisingHold(Status.HELD, LocalDateTime.now(clock).plusSeconds(holdSeconds));
    }
    public LocalDateTime expiresAt() {
        return expiresAt;
    }
    public Status status() {
        return status;
    }
    public boolean isExpired(Clock clock) {
        return !expiresAt.isAfter(LocalDateTime.now(clock));
    }
    public void confirm(Clock clock) {
        if (status != Status.HELD || isExpired(clock)) throw new ValidationException("This hold has expired.");
        status = Status.CONFIRMED;
    }
    public void expire() {
        transition(Status.EXPIRED);
    }
    public void remove() {
        transition(Status.REMOVED);
    }
    private void transition(Status next) {
        if (status != Status.HELD) throw new ValidationException("Only a held course can change status.");
        status = next;
    }
}
