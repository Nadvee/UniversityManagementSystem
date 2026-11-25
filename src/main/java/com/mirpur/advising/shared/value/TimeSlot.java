package com.mirpur.advising.shared.value;

import com.mirpur.advising.shared.exception.ValidationException;
import java.time.LocalTime;
import java.util.List;

public record TimeSlot(String day, LocalTime start, LocalTime end) {
    public TimeSlot {
        if ( !List.of("SAT", "SUN", "MON", "TUE", "WED", "THU").contains(day) || start == null || end == null || !start.isBefore(end)) throw new ValidationException("Use Saturday–Thursday and a valid start/end time.");
    }
    public boolean overlaps(TimeSlot other) {
        return day.equals(other.day) && start.isBefore(other.end) && other.start.isBefore(end);
    }
}
