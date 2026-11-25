package com.mirpur.advising.account;

import com.mirpur.advising.advising.AdvisingService;
import com.mirpur.advising.advising.StudentRepository;
import java.util.*;
import org.springframework.stereotype.Component;

@Component public final class AdminDashboardAssembler implements DashboardAssembler {
    private final StudentRepository db;
    private final AdvisingService advising;
    public AdminDashboardAssembler(StudentRepository db, AdvisingService advising) {
        this.db = db;
        this.advising = advising;
    }
    public String role() {
        return "ADMIN";
    }
    public Object assemble(Map < String, Object > user) {
        var result = new LinkedHashMap < String, Object >();
        result.put("user", user);
        result.put("students", db.dashboardStudentsCount());
        result.put("teachers", db.dashboardTeachersCount());
        result.put("sections", db.dashboardSectionsCount2());
        result.put("windows", advising.windows());
        result.put("audit", db.dashboardAuditLogsList());
        return result;
    }
}
