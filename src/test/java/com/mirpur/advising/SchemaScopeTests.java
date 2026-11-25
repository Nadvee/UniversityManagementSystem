package com.mirpur.advising;

import com.mirpur.advising.admin.AdminService;
import com.mirpur.advising.advising.AdvisingService;
import com.mirpur.advising.shared.db.JdbcDatabase;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:core_schema;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","app.cache-enabled=false","app.admin-password=schema-admin"})
class SchemaScopeTests {
    @Autowired JdbcDatabase db;
    @Autowired AdminService admin;
    @Autowired AdvisingService advising;

    @Test void schemaContainsOnlyTablesSupportingTheThirteenPages() {
        var names = db.list("SELECT table_name FROM information_schema.tables WHERE table_schema='public'").stream().map(row -> row.get("table_name")).toList();
        assertThat(names).containsExactlyInAnyOrder("departments","semesters","users","account_password_setup","students","teachers","courses","course_prerequisites","rooms","sections","deleted_offerings","section_schedules","teacher_course_choices","enrollments","advising_windows","advising_holds","advising_rules","credit_limit_bands","advising_window_sections","notices","audit_logs","prerequisite_records");
        assertThat(db.list("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='advising_rules'").stream().map(row -> row.get("column_name"))).doesNotContain("late_fee","late_fee_after_date");
        assertThat(db.list("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='sections'").stream().map(row -> row.get("column_name"))).doesNotContain("is_test");
    }

    @Test void prerequisiteEligibilityUsesTheLatestAcademicRecord() {
        long actor=db.count("SELECT id FROM users WHERE login_id='admin'");
        admin.account(actor,"STUDENT",Map.of("loginId","S88001","name","Prerequisite Student","subject","CSE"));
        long student=db.count("SELECT id FROM users WHERE login_id='S88001'");
        long course=db.count("SELECT id FROM courses WHERE code='CSE101'");
        long section=db.count("SELECT MIN(id) FROM sections WHERE course_id=(SELECT id FROM courses WHERE code='CSE103')");
        assertThat(advising.warnings(student,section)).anyMatch(warning -> warning.contains("Prerequisite not met"));
        db.exec("INSERT INTO prerequisite_records(student_id,course_id,semester_id,passed,recorded_at) VALUES(?,?,?,TRUE,'2026-01-01 09:00:00')",student,course,db.semester());
        assertThat(advising.warnings(student,section)).isEmpty();
        db.exec("INSERT INTO prerequisite_records(student_id,course_id,semester_id,passed,recorded_at) VALUES(?,?,?,FALSE,'2026-02-01 09:00:00')",student,course,db.semester());
        assertThat(advising.warnings(student,section)).anyMatch(warning -> warning.contains("Prerequisite not met"));
    }
}
