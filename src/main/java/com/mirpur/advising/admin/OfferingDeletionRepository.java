package com.mirpur.advising.admin;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class OfferingDeletionRepository extends BaseRepository {
    public OfferingDeletionRepository(JdbcDatabase database) {
        super(database);
    }
    public List < Map < String, Object >> lockStudents(long section) {
        return database.list("SELECT user_id FROM students WHERE user_id IN (SELECT student_id FROM enrollments WHERE section_id=? UNION SELECT student_id FROM advising_holds WHERE section_id=?) ORDER BY user_id FOR UPDATE", section, section);
    }
    public Map < String, Object > lockSection(long id) {
        return database.one("SELECT s.*,c.code,c.name FROM sections s JOIN courses c ON c.id=s.course_id WHERE s.id=? FOR UPDATE", id);
    }
    public boolean deleted(long id) {
        return database.count("SELECT COUNT(*) FROM deleted_offerings WHERE section_id=?", id) > 0;
    }
    public void cancelEnrollment(long section) {
        database.exec("UPDATE enrollments SET status='DROPPED',dropped_at=CURRENT_TIMESTAMP,drop_reason='Course offering deleted by administrator.' WHERE section_id=? AND status='CONFIRMED'", section);
        database.exec("UPDATE advising_holds SET status='EXPIRED' WHERE section_id=? AND status='HELD'", section);
    }
    public void removeOffering(long actor, long section, String originalNo) {
        database.exec("INSERT INTO deleted_offerings(section_id,original_section_no,deleted_by) VALUES(?,?,?)", section, originalNo, actor);
        database.exec("DELETE FROM advising_window_sections WHERE section_id=?", section);
        database.exec("UPDATE teacher_course_choices SET status='REJECTED',assigned_section_id=NULL,decided_by=?,decided_at=CURRENT_TIMESTAMP WHERE assigned_section_id=?", actor, section);
        // Retain enrollment and hold history; free the original section number for reuse.
        database.exec("UPDATE sections SET is_offered=FALSE,teacher_id=NULL,seats_taken=0,section_no=? WHERE id=?", "~deleted~" + section, section);
    }
}
