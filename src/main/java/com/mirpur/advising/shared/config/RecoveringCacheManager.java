package com.mirpur.advising.shared.config;

import java.util.*;
import java.util.concurrent.*;
import org.springframework.cache.*;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.transaction.support.*;

/** Keeps a failed Redis invalidation dirty until it can be retried on a later read. */
public class RecoveringCacheManager implements CacheManager {
    private final RedisCacheManager delegate;
    private final Map < String, Cache > wrappers = new ConcurrentHashMap < >();
    private final Set < String > dirty = ConcurrentHashMap.newKeySet();
    public RecoveringCacheManager(RedisCacheManager delegate) {
        this.delegate = delegate;
        delegate.afterPropertiesSet();
    }
    public Collection < String > getCacheNames() {
        return delegate.getCacheNames();
    }
    public Cache getCache(String name) {
        return wrappers.computeIfAbsent(name, n -> new SafeCache(Objects.requireNonNull(delegate.getCache(n))));
    }
    private class SafeCache implements Cache {
        private final Cache cache;
        SafeCache(Cache cache) {
            this.cache = cache;
        }
        public String getName() {
            return cache.getName();
        }
        public Object getNativeCache() {
            return cache.getNativeCache();
        }
        private void recover() {
            if (dirty.contains(getName())) {
                cache.clear();
                dirty.remove(getName());
            }
        }
        public ValueWrapper get(Object key) {
            try {
                recover();
                return cache.get(key);
            } catch (RuntimeException unavailable) {
                return null;
            }
        }
        public < T > T get(Object key, Class < T > type) {
            ValueWrapper v = get(key);
            return v == null ? null : type.cast(v.get());
        }
        public < T > T get(Object key, Callable < T > loader) {
            var loaded = new java.util.concurrent.atomic.AtomicBoolean();
            var value = new java.util.concurrent.atomic.AtomicReference < T >();
            Callable < T > observed =() -> {
                T result = loader.call();
                value.set(result);
                loaded.set(true);
                return result;
            };
            try {
                recover();
                T result = cache.get(key, observed);
                return result;
            } catch (ValueRetrievalException queryFailure) {
                throw queryFailure;
            } catch (RuntimeException unavailable) {
                if (loaded.get()) return value.get();
                try {
                    return loader.call();
                } catch (Exception e) {
                    throw new ValueRetrievalException(key, loader, e);
                }
            }
        }
        private void mutate(Runnable operation) {
            Runnable safe =() -> {
                try {
                    operation.run();
                } catch (RuntimeException unavailable) {
                    dirty.add(getName());
                }
            };
            if (TransactionSynchronizationManager.isSynchronizationActive() && TransactionSynchronizationManager.isActualTransactionActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                public void afterCommit() {
                    safe.run();
                }
            });
            else safe.run();
        }
        public void put(Object key, Object value) {
            mutate(() -> {
                recover();
                cache.put(key, value);
            });
        }
        public void evict(Object key) {
            mutate(() -> cache.evict(key));
        }
        public void clear() {
            mutate(() -> {
                cache.clear();
                dirty.remove(getName());
            });
        }
    }
}
