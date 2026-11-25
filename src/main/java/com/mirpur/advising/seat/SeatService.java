package com.mirpur.advising.seat;

import com.mirpur.advising.shared.config.RuntimeSettings;
import com.mirpur.advising.shared.db.JdbcDatabase;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;

@Service public class SeatService implements SeatCounterStore {
    private final Map < Boolean, SeatAllocator > allocators;
    private final SeatRepository db;
    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final String namespace;
    private final Set < Long > dirty = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final DefaultRedisScript < Long > reserve = new DefaultRedisScript < >(), release = new DefaultRedisScript < >();
    public SeatService(SeatRepository db, StringRedisTemplate redis, RuntimeSettings settings) {
        this.allocators = Map.of(false, new DbSeatAllocator(db), true, new RedisSeatAllocator(this, new DbSeatAllocator(db)));
        this.namespace = settings.app().namespace();
        this.db = db;
        this.redis = redis;
        this.enabled = settings.app().cacheEnabled();
        reserve.setLocation(new ClassPathResource("lua/reserve_seat.lua"));
        reserve.setResultType(Long.class);
        release.setLocation(new ClassPathResource("lua/release_seat.lua"));
        release.setResultType(Long.class);
    }
    private String key(long id) {
        return namespace + ":seat:" + id;
    }
    public boolean reserve(long section, boolean fast) {
        return allocators.get(fast).reserve(section);
    }
    public void compensate(long section) {
        increment(section);
    }
    public Reservation tryReserve(long section) {
        if (enabled) try {
            if (dirty.contains(section)) {
                redis.delete(key(section));
                dirty.remove(section);
            }
            if (Boolean.FALSE.equals(redis.hasKey(key(section)))) {
                var s = db.reserveSectionsRecord(section);
                redis.opsForValue().setIfAbsent(key(section), "" +(JdbcDatabase.number(s, "capacity") - JdbcDatabase.number(s, "seats_taken")));
            }
            Long result = redis.execute(reserve, List.of(key(section)));
            if (Long.valueOf(0).equals(result)) return Reservation.FULL;
            if (Long.valueOf(1).equals(result)) return Reservation.RESERVED;
        } catch (RuntimeException ignored) {
            dirty.add(section);
        }
        return Reservation.UNAVAILABLE;
    }
    public void lock(long student, long section) {
        if ( !enabled) return;
        String lock = namespace + ":lock:" + student + ":" + section, token = UUID.randomUUID().toString();
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(lock, token, java.time.Duration.ofSeconds(15));
        } catch (RuntimeException ignored) {
            return;
        }
        JdbcDatabase.require( !Boolean.FALSE.equals(acquired), "This course selection is already being processed.");
        if (Boolean.TRUE.equals(acquired)) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCompletion(int status) {
                try {
                    redis.execute(new DefaultRedisScript < Long >("if redis.call('GET',KEYS[1]) == ARGV[1] then return redis.call('DEL',KEYS[1]) end return 0", Long.class), List.of(lock), token);
                } catch (RuntimeException ignored) {
                }
            }
        });
    }
    public void release(long section) {
        db.releaseSectionsUpdate(section);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() {
                increment(section);
            }
        });
    }
    public void invalidate(long section) {
        if ( !enabled) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCommit() {
                try {
                    redis.delete(key(section));
                    dirty.remove(section);
                } catch (RuntimeException unavailable) {
                    dirty.add(section);
                }
            }
        });
    }
    private void increment(long section) {
        if (enabled) try {
            redis.execute(release, List.of(key(section)));
        } catch (RuntimeException ignored) {
            dirty.add(section);
        }
    }
    public List < Map < String, Object >> merge(List < Map < String, Object >> source) {
        List < String > values = null;
        if (enabled && !source.isEmpty()) try {
            values = redis.opsForValue().multiGet(source.stream().map(s -> key(JdbcDatabase.number(s, "id"))).toList());
        } catch (RuntimeException ignored) {
        }
        var result = new ArrayList < Map < String, Object >>();
        int i = 0;
        for (var row : source) {
            var copy = new LinkedHashMap < >(row);
            String v = values == null ? null : values.get(i ++);
            if (dirty.contains(JdbcDatabase.number(row, "id"))) v = null;
            if (v != null) {
                copy.put("available", Integer.parseInt(v));
                copy.put("seats_taken", JdbcDatabase.number(row, "capacity") - Integer.parseInt(v));
            } else {
                var live = db.mergeSectionsRecord(row.get("id"));
                copy.put("available", JdbcDatabase.number(live, "capacity") - JdbcDatabase.number(live, "seats_taken"));
                copy.put("seats_taken", live.get("seats_taken"));
            }
            result.add(copy);
        }
        return result;
    }
}
