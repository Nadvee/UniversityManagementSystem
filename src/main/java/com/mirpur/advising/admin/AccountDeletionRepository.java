package com.mirpur.advising.admin;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class AccountDeletionRepository extends BaseRepository {
    public AccountDeletionRepository(JdbcDatabase database) {
        super(database);
    }
    public Map < String, Object > lockAccount(long id, String role) {
        return database.one("SELECT id,login_id,full_name FROM users WHERE id=? AND role=? FOR UPDATE", id, role);
    }
    public void lockStudent(long id) {
        database.one("SELECT user_id FROM students WHERE user_id=? FOR UPDATE", id);
    }
    public List < Map < String, Object >> studentSections(long id) {
        return database.list("SELECT id FROM sections WHERE id IN (SELECT section_id FROM enrollments WHERE student_id=? UNION SELECT section_id FROM advising_holds WHERE student_id=?) ORDER BY id FOR UPDATE", id, id);
    }
    public long reservedSeats(long student, long section) {
        return database.count("SELECT (SELECT COUNT(*) FROM enrollments WHERE student_id=? AND section_id=? AND status='CONFIRMED') + (SELECT COUNT(*) FROM advising_holds WHERE student_id=? AND section_id=? AND status='HELD')", student, section, student, section);
    }
    public int releaseSeats(long section, long count) {
        return database.exec("UPDATE sections SET seats_taken=seats_taken-? WHERE id=? AND seats_taken>=?", count, section, count);
    }
    public void deleteStudentRecords(long id) {
        database.exec("DELETE FROM prerequisite_records WHERE student_id=?", id);
        database.exec("DELETE FROM advising_holds WHERE student_id=?", id);
        database.exec("DELETE FROM enrollments WHERE student_id=?", id);
        database.exec("DELETE FROM students WHERE user_id=?", id);
    }
    public void deleteTeacherRecords(long id) {
        database.one("SELECT user_id FROM teachers WHERE user_id=? FOR UPDATE", id);
        database.list("SELECT id FROM sections WHERE teacher_id=? ORDER BY id FOR UPDATE", id);
        database.exec("UPDATE sections SET teacher_id=NULL WHERE teacher_id=?", id);
        database.exec("DELETE FROM teacher_course_choices WHERE teacher_id=?", id);
        database.exec("DELETE FROM teachers WHERE user_id=?", id);
    }
    public void deleteAccount(long id) {
        // Retain shared records and audit history when their optional actor is removed.
        database.exec("UPDATE teacher_course_choices SET decided_by=NULL WHERE decided_by=?", id);
        database.exec("UPDATE advising_windows SET created_by=NULL WHERE created_by=?", id);
        database.exec("UPDATE audit_logs SET actor_id=NULL WHERE actor_id=?", id);
        database.exec("DELETE FROM account_password_setup WHERE user_id=?", id);
        database.exec("DELETE FROM users WHERE id=?", id);
    }
}
