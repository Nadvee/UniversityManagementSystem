package com.mirpur.advising.advising;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.catalog.CatalogSql;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class StudentRepository extends BaseRepository {
    public StudentRepository(JdbcDatabase database) {
        super(database);
    }
    public Map < String, Object > dashboardUsersRecord(Object ... args) {
        return database.one("SELECT id,login_id,full_name,role FROM users WHERE id=?", args);
    }
    public Map < String, Object > dashboardStudentsRecord(Object ... args) {
        return database.one("SELECT s.*,d.name AS department,t.name AS semester FROM students s JOIN departments d ON d.id=s.dept_id JOIN semesters t ON t.id=s.current_semester_id WHERE s.user_id=?", args);
    }
    public Map < String, Object > dashboardTeachersRecord(Object ... args) {
        return database.one("SELECT * FROM teachers WHERE user_id=?", args);
    }
    public long dashboardSectionsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM sections WHERE teacher_id=? AND semester_id=?", args);
    }
    public long dashboardTeacherCourseChoicesCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=? AND status IN ('SELECTED','CONFIRMED')", args);
    }
    public long dashboardStudentsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM students", args);
    }
    public long dashboardTeachersCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM teachers", args);
    }
    public long dashboardSectionsCount2(Object ... args) {
        return database.count("SELECT COUNT(*) FROM sections s WHERE NOT EXISTS (SELECT 1 FROM deleted_offerings x WHERE x.section_id=s.id)", args);
    }
    public List < Map < String, Object >> dashboardAuditLogsList(Object ... args) {
        return database.list("SELECT a.*,u.full_name AS actor FROM audit_logs a LEFT JOIN users u ON u.id=a.actor_id ORDER BY a.id DESC LIMIT 8", args);
    }
    public Map < String, Object > eligibleAdvisingRulesRecord(Object ... args) {
        return database.one("SELECT * FROM advising_rules WHERE semester_id=?", args);
    }
}
