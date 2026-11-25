package com.mirpur.advising.account;

import com.mirpur.advising.advising.AdvisingService;
import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.advising.StudentRepository;
import java.util.*;
import org.springframework.stereotype.Component;

@Component public final class StudentDashboardAssembler implements DashboardAssembler {
    private final StudentRepository db;
    private final CachedCatalogService cache;
    private final AdvisingService advising;
    public StudentDashboardAssembler(StudentRepository db, CachedCatalogService cache, AdvisingService advising) {
        this.db = db;
        this.cache = cache;
        this.advising = advising;
    }
    public String role() {
        return "STUDENT";
    }
    public Object assemble(Map < String, Object > user) {
        var result = new LinkedHashMap < String, Object >();
        result.put("user", user);
        result.put("profile", db.dashboardStudentsRecord(((Number) user.get("id")).longValue()));
        result.put("routine", cache.routine(((Number) user.get("id")).longValue(), db.semester()));
        result.put("windows", advising.windows());
        result.put("selectedCredit", advising.credits(((Number) user.get("id")).longValue()));
        return result;
    }
}
