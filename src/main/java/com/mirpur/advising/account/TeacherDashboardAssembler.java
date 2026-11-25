package com.mirpur.advising.account;

import com.mirpur.advising.advising.StudentRepository;
import java.util.*;
import org.springframework.stereotype.Component;

@Component public final class TeacherDashboardAssembler implements DashboardAssembler {
    private final StudentRepository db;
    public TeacherDashboardAssembler(StudentRepository db) {
        this.db = db;
    }
    public String role() {
        return "TEACHER";
    }
    public Object assemble(Map < String, Object > user) {
        var result = new LinkedHashMap < String, Object >();
        result.put("user", user);
        result.put("profile", db.dashboardTeachersRecord(((Number) user.get("id")).longValue()));
        result.put("assigned", db.dashboardSectionsCount(((Number) user.get("id")).longValue(), db.semester()));
        result.put("pending", db.dashboardTeacherCourseChoicesCount(((Number) user.get("id")).longValue()));
        return result;
    }
}
