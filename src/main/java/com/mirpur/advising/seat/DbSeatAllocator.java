package com.mirpur.advising.seat;


public final class DbSeatAllocator implements SeatAllocator {
    private final SeatRepository sections;
    public DbSeatAllocator(SeatRepository sections) {
        this.sections = sections;
    }
    public boolean reserve(long section) {
        return sections.afterCompletionSectionsUpdate(section) == 1;
    }
}
