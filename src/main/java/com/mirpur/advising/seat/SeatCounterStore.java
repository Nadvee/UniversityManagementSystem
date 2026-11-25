package com.mirpur.advising.seat;



public interface SeatCounterStore {
    enum Reservation {
        RESERVED, FULL, UNAVAILABLE
    }
    Reservation tryReserve(long section);
    void compensate(long section);
}
