package com.mirpur.advising.advising;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.catalog.CatalogSql;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class AdvisingRepository extends BaseRepository {
    public AdvisingRepository(JdbcDatabase database) {
        super(database);
    }
    public Map < String, Object > bandStudentsRecord(Object ... args) {
        return database.one("SELECT * FROM students WHERE user_id=?", args);
    }
    public List < Map < String, Object >> bandCreditLimitBandsList(Object ... args) {
        return database.list("SELECT b.* FROM credit_limit_bands b JOIN advising_rules r ON r.id=b.rule_id WHERE r.semester_id=?", args);
    }
    public List < Map < String, Object >> windowsAdvisingWindowsList(Object ... args) {
        return database.list("SELECT * FROM advising_windows WHERE semester_id=? AND status<>'CANCELLED' ORDER BY CASE WHEN status='OPEN' THEN 0 ELSE 1 END, start_at DESC, id DESC", args);
    }
    public List < Map < String, Object >> openAdvisingWindowsList(Object ... args) {
        return database.list("SELECT * FROM advising_windows WHERE semester_id=? AND status='OPEN' AND start_at<=CURRENT_TIMESTAMP AND end_at>CURRENT_TIMESTAMP ORDER BY start_at", args);
    }
    public Map < String, Object > warningsSectionsRecord(Object ... args) {
        return database.one("SELECT course_id FROM sections WHERE id=?", args);
    }
    public List < Map < String, Object >> warningsCoursePrerequisitesList(Object ... args) {
        return database.list("SELECT c.id,c.code,c.name FROM course_prerequisites p JOIN courses c ON c.id=p.prereq_course_id WHERE p.course_id=?", args);
    }
    public List < Map < String, Object >> warningsPrerequisiteRecords(Object ... args) {
        return database.list("SELECT r.passed FROM prerequisite_records r JOIN semesters t ON t.id=r.semester_id WHERE r.student_id=? AND r.course_id=? ORDER BY t.start_date DESC,r.recorded_at DESC,r.id DESC", args);
    }
    public long warningsSectionSchedulesCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM section_schedules a JOIN section_schedules b ON a.day=b.day AND a.start_time<b.end_time AND b.start_time<a.end_time WHERE a.section_id=? AND b.section_id<>? AND b.section_id IN(SELECT section_id FROM enrollments WHERE student_id=? AND semester_id=? AND status='CONFIRMED' UNION SELECT section_id FROM advising_holds WHERE student_id=? AND status='HELD' AND expires_at>CURRENT_TIMESTAMP)", args);
    }
    public Double creditsSectionsValue(Object ... args) {
        return database.jdbc().queryForObject("SELECT COALESCE(SUM(c.credit),0) FROM sections s JOIN courses c ON c.id=s.course_id WHERE s.semester_id=? AND s.id IN(SELECT section_id FROM enrollments WHERE student_id=? AND status='CONFIRMED' UNION SELECT section_id FROM advising_holds WHERE student_id=? AND status='HELD' AND expires_at>CURRENT_TIMESTAMP)", Double.class, args);
    }
    public Map < String, Object > validateAdvisingWindowsRecord(Object ... args) {
        return database.one("SELECT w.* FROM advising_windows w JOIN advising_window_sections ws ON ws.window_id=w.id WHERE ws.section_id=? AND w.semester_id=? AND w.status='OPEN' AND w.start_at<=CURRENT_TIMESTAMP AND w.end_at>CURRENT_TIMESTAMP FOR UPDATE", args);
    }
    public Map < String, Object > validateSectionsRecord(Object ... args) {
        return database.one("SELECT s.*,c.credit FROM sections s JOIN courses c ON c.id=s.course_id WHERE s.id=? AND s.is_offered=TRUE AND s.teacher_id IS NOT NULL", args);
    }
    public long validateEnrollmentsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM enrollments e JOIN sections s ON s.id=e.section_id WHERE e.student_id=? AND s.course_id=? AND e.semester_id=? AND e.status='CONFIRMED'", args);
    }
    public long validateAdvisingHoldsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM advising_holds h JOIN sections s ON s.id=h.section_id WHERE h.student_id=? AND s.course_id=? AND h.status='HELD'", args);
    }
    public Map < String, Object > selectStudentsRecord(Object ... args) {
        return database.one("SELECT user_id FROM students WHERE user_id=? FOR UPDATE", args);
    }
    public List < Map < String, Object >> selectAdvisingHoldsList(Object ... args) {
        return database.list("SELECT * FROM advising_holds WHERE student_id=? AND section_id=? AND status='HELD' AND expires_at>CURRENT_TIMESTAMP", args);
    }
    public long selectAdvisingHoldsInsert(Object ... args) {
        return database.insert("INSERT INTO advising_holds(student_id,section_id,window_id,source,expires_at,status) VALUES(?,?,?,?,?,'HELD')", args);
    }
    public Map < String, Object > selectAdvisingHoldsRecord(Object ... args) {
        return database.one("SELECT * FROM advising_holds WHERE id=?", args);
    }
    public Map < String, Object > confirmStudentsRecord(Object ... args) {
        return database.one("SELECT user_id FROM students WHERE user_id=? FOR UPDATE", args);
    }
    public Map < String, Object > confirmAdvisingHoldsRecord(Object ... args) {
        return database.one("SELECT * FROM advising_holds WHERE id=? AND student_id=? FOR UPDATE", args);
    }
    public List < Map < String, Object >> confirmEnrollmentsList(Object ... args) {
        return database.list("SELECT id FROM enrollments WHERE student_id=? AND section_id=?", args);
    }
    public long confirmEnrollmentsInsert(Object ... args) {
        return database.insert("INSERT INTO enrollments(student_id,section_id,semester_id,status) VALUES(?,?,?,'CONFIRMED')", args);
    }
    public int confirmEnrollmentsUpdate(Object ... args) {
        return database.exec("UPDATE enrollments SET status='CONFIRMED',confirmed_at=CURRENT_TIMESTAMP,dropped_at=NULL,drop_reason=NULL WHERE id=?", args);
    }
    public int confirmAdvisingHoldsUpdate(Object ... args) {
        return database.exec("UPDATE advising_holds SET status='CONFIRMED' WHERE id=?", args);
    }
    public Map < String, Object > releaseStudentsRecord(Object ... args) {
        return database.one("SELECT user_id FROM students WHERE user_id=? FOR UPDATE", args);
    }
    public Map < String, Object > releaseAdvisingHoldsRecord(Object ... args) {
        return database.one("SELECT * FROM advising_holds WHERE id=? AND student_id=? FOR UPDATE", args);
    }
    public int releaseAdvisingHoldsUpdate(Object ... args) {
        return database.exec("UPDATE advising_holds SET status=? WHERE id=?", args);
    }
    public List < Map < String, Object >> stateCatalogueList(Object ... args) {
        return database.list(CatalogSql.SELECT + " JOIN advising_holds h ON h.section_id=s.id WHERE h.student_id=? AND h.status='HELD' ORDER BY h.id", args);
    }
    public Map < String, Object > stateAdvisingHoldsRecord(Object ... args) {
        return database.one("SELECT id,expires_at FROM advising_holds WHERE student_id=? AND section_id=? AND status='HELD'", args);
    }
    public Map < String, Object > dropStudentsRecord(Object ... args) {
        return database.one("SELECT user_id FROM students WHERE user_id=? FOR UPDATE", args);
    }
    public Map < String, Object > dropAdvisingRulesRecord(Object ... args) {
        return database.one("SELECT * FROM advising_rules WHERE semester_id=?", args);
    }
    public Map < String, Object > dropEnrollmentsRecord(Object ... args) {
        return database.one("SELECT id FROM enrollments WHERE student_id=? AND section_id=? AND status='CONFIRMED'", args);
    }
    public int dropEnrollmentsUpdate(Object ... args) {
        return database.exec("UPDATE enrollments SET status='DROPPED',dropped_at=CURRENT_TIMESTAMP,drop_reason=? WHERE id=?", args);
    }
}
