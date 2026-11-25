package com.mirpur.advising.advising;

import com.mirpur.advising.shared.exception.ValidationException;
import com.mirpur.advising.catalog.Section;
import java.time.*;
import java.util.*;

public final class AdvisingWindow {
    public enum Status {
        SCHEDULED, OPEN, CLOSED, CANCELLED
    }
    private Status status;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final Set < Long > sections = new HashSet < >();
    public AdvisingWindow(Status status, LocalDateTime start, LocalDateTime end) {
        if ( !start.isBefore(end)) throw new ValidationException("Invalid advising window.");
        this.status = status;
        this.start = start;
        this.end = end;
    }
    public boolean isOpen(Clock clock) {
        var now = LocalDateTime.now(clock);
        return status == Status.OPEN && !now.isBefore(start) && now.isBefore(end);
    }
    public void addSection(Section section) {
        if ( !section.hasTeacher()) throw new ValidationException("Every section must be offered and have an assigned teacher.");
        sections.add(section.id());
    }
    public void open() {
        if (status == Status.CANCELLED) throw new ValidationException("This advising window has been deleted.");
        status = Status.OPEN;
    }
    public void close() {
        if (status == Status.CANCELLED) throw new ValidationException("This advising window has been deleted.");
        status = Status.CLOSED;
    }
    public void cancel() {
        status = Status.CANCELLED;
    }
    public Status status() {
        return status;
    }
}
