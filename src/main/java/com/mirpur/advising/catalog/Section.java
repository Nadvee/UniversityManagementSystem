package com.mirpur.advising.catalog;

import com.mirpur.advising.shared.exception.SeatFullException;
import com.mirpur.advising.shared.exception.ValidationException;
import com.mirpur.advising.shared.value.TimeSlot;
import java.util.ArrayList;
import java.util.List;

public final class Section {
    private final long id;
    private final int capacity;
    private int seatsTaken;
    private Long teacherId;
    private final List < TimeSlot > schedules = new ArrayList < >();
    public Section(long id, int capacity, int seatsTaken, Long teacherId) {
        if (capacity <= 0 || seatsTaken < 0 || seatsTaken > capacity) throw new ValidationException("Capacity cannot be below occupied seats.");
        this.id = id;
        this.capacity = capacity;
        this.seatsTaken = seatsTaken;
        this.teacherId = teacherId;
    }
    public long id() {
        return id;
    }
    public int availableSeats() {
        return capacity - seatsTaken;
    }
    public boolean hasTeacher() {
        return teacherId != null;
    }
    public void assignTeacher(long teacher) {
        teacherId = teacher;
    }
    public void reserveSeat() {
        if (availableSeats() == 0) throw new SeatFullException("Seat full.");
        seatsTaken ++;
    }
    public void releaseSeat() {
        if (seatsTaken > 0) seatsTaken --;
    }
    public void addSchedule(TimeSlot slot) {
        if (schedules.stream().anyMatch(slot::overlaps)) throw new ValidationException("Schedule slots overlap.");
        schedules.add(slot);
    }
    public boolean clashesWith(Section other) {
        return schedules.stream().anyMatch(slot -> other.schedules.stream().anyMatch(slot::overlaps));
    }
}
