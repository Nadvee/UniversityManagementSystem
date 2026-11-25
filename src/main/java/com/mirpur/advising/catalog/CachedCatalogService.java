package com.mirpur.advising.catalog;

import java.util.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

@Service public class CachedCatalogService {
    private final CatalogService source;
    public CachedCatalogService(CatalogService s) {
        source = s;
    }
    @Cacheable(cacheNames = "advisingCourses", key = "#window", sync = true) public List < Map < String, Object >> window(long window) {
        return source.window(window);
    }
    @Cacheable(cacheNames = "routine", key = "#student + ':' + #semester", sync = true) public List < Map < String, Object >> routine(long student, long semester) {
        return source.routine(student, semester);
    }
    @Caching(evict = {
        @CacheEvict(cacheNames = "advisingCourses", allEntries = true),
        @CacheEvict(cacheNames = "routine", allEntries = true)
    }) public void clear() {
    }
}
