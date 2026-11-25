package com.mirpur.advising.seat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.transaction.support.*;

public final class RedisSeatAllocator implements SeatAllocator {
    private final SeatCounterStore counters;
    private final SeatAllocator database;
    public RedisSeatAllocator(SeatCounterStore counters, SeatAllocator database) {
        this.counters = counters;
        this.database = database;
    }
    public boolean reserve(long section) {
        var reservation = counters.tryReserve(section);
        if (reservation == SeatCounterStore.Reservation.FULL) return false;
        boolean held = reservation == SeatCounterStore.Reservation.RESERVED;
        var compensated = new AtomicBoolean();
        if (held) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED && compensated.compareAndSet(false, true)) counters.compensate(section);
            }
        });
        boolean success = database.reserve(section);
        if ( !success && held && compensated.compareAndSet(false, true)) counters.compensate(section);
        return success;
    }
}
