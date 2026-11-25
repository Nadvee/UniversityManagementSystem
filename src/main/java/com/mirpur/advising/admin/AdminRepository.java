package com.mirpur.advising.admin;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.catalog.CatalogSql;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class AdminRepository extends BaseRepository {
    public AdminRepository(JdbcDatabase database) {
        super(database);
    }
    public List < Map < String, Object >> accountDepartmentsList(Object ... args) {
        return database.list("SELECT id FROM departments WHERE code=?", args);
    }
    public long accountUsersInsert(Object ... args) {
        return database.insert("INSERT INTO users(login_id,password_hash,role,full_name) VALUES(?,?,?,?)", args);
    }
    public void requirePasswordSetup(long userId) {
        database.exec("INSERT INTO account_password_setup(user_id) VALUES(?)", userId);
    }
    public int accountStudentsUpdate(Object ... args) {
        return database.exec("INSERT INTO students(user_id,student_id,dept_id,subject,current_semester_id) VALUES(?,?,?,?,?)", args);
    }
    public int accountTeachersUpdate(Object ... args) {
        return database.exec("INSERT INTO teachers(user_id,teacher_id,dept_id) VALUES(?,?,?)", args);
    }
    public Map < String, Object > offeringRoomsRecord(Object ... args) {
        return database.one("SELECT capacity FROM rooms WHERE id=?", args);
    }
    public Map < String, Object > offeringCoursesRecord(Object ... args) {
        return database.one("SELECT id FROM courses WHERE id=?", args);
    }
    public Map < String, Object > offeringSectionsRecord(Object ... args) {
        return database.one("SELECT * FROM sections s WHERE id=? AND NOT EXISTS (SELECT 1 FROM deleted_offerings x WHERE x.section_id=s.id) FOR UPDATE", args);
    }
    public Map < String, Object > offeringRoomsRecord2(Object ... args) {
        return database.one("SELECT id FROM rooms WHERE id=? FOR UPDATE", args);
    }
    public Map < String, Object > offeringTeachersRecord(Object ... args) {
        return database.one("SELECT user_id FROM teachers WHERE user_id=? FOR UPDATE", args);
    }
    public long offeringSectionsInsert(Object ... args) {
        return database.insert("INSERT INTO sections(course_id,semester_id,section_no,room_id,capacity,is_offered) VALUES(?,?,?,?,?,?)", args);
    }
    public int offeringSectionsUpdate(Object ... args) {
        return database.exec("UPDATE sections SET course_id=?,section_no=?,room_id=?,capacity=?,is_offered=? WHERE id=?", args);
    }
    public int offeringStatusUpdate(boolean offered, long id) {
        return database.exec("UPDATE sections SET is_offered=? WHERE id=?", offered, id);
    }
    public int offeringSectionSchedulesUpdate(Object ... args) {
        return database.exec("DELETE FROM section_schedules WHERE section_id=?", args);
    }
    public int offeringSectionSchedulesUpdate2(Object ... args) {
        return database.exec("INSERT INTO section_schedules(section_id,day,start_time,end_time) VALUES(?,?,?,?)", args);
    }
    public long checkScheduleSectionSchedulesCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM section_schedules x JOIN sections s ON s.id=x.section_id WHERE s.semester_id=? AND NOT EXISTS (SELECT 1 FROM deleted_offerings d WHERE d.section_id=s.id) AND s.id<>? AND (s.room_id=? OR s.teacher_id=?) AND x.day=? AND x.start_time<? AND x.end_time>?", args);
    }
    public Map < String, Object > assignTeacherCourseChoicesRecord(Object ... args) {
        return database.one("SELECT * FROM teacher_course_choices WHERE id=? FOR UPDATE", args);
    }
    public Map < String, Object > assignSectionsRecord(Object ... args) {
        return database.one("SELECT * FROM sections s WHERE id=? AND NOT EXISTS (SELECT 1 FROM deleted_offerings x WHERE x.section_id=s.id) FOR UPDATE", args);
    }
    public Map < String, Object > assignTeachersRecord(Object ... args) {
        return database.one("SELECT user_id FROM teachers WHERE user_id=? FOR UPDATE", args);
    }
    public List < Map < String, Object >> assignSectionSchedulesList(Object ... args) {
        return database.list("SELECT day,start_time AS `start`,end_time AS `end` FROM section_schedules WHERE section_id=?", args);
    }
    public int assignSectionsUpdate(Object ... args) {
        return database.exec("UPDATE sections SET teacher_id=? WHERE id=?", args);
    }
    public int assignTeacherCourseChoicesUpdate(Object ... args) {
        return database.exec("UPDATE teacher_course_choices SET status='REJECTED',assigned_section_id=NULL,decided_by=?,decided_at=CURRENT_TIMESTAMP WHERE assigned_section_id=? AND teacher_id<>? AND status='ASSIGNED'", args);
    }
    public int assignTeacherCourseChoicesUpdate2(Object ... args) {
        return database.exec("UPDATE teacher_course_choices SET status='ASSIGNED',assigned_section_id=?,decided_by=?,decided_at=CURRENT_TIMESTAMP WHERE id=?", args);
    }
    public Map < String, Object > windowSemestersRecord(Object ... args) {
        return database.one("SELECT id FROM semesters WHERE id=? FOR UPDATE", args);
    }
    public long windowAdvisingWindowsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM advising_windows WHERE semester_id=? AND status IN ('SCHEDULED','OPEN') AND start_at<? AND end_at>?", args);
    }
    public Map < String, Object > windowSectionsRecord(Object ... args) {
        return database.one("SELECT * FROM sections s WHERE id=? AND NOT EXISTS (SELECT 1 FROM deleted_offerings x WHERE x.section_id=s.id) FOR UPDATE", args);
    }
    public long windowAdvisingWindowsInsert(Object ... args) {
        return database.insert("INSERT INTO advising_windows(semester_id,name,start_at,end_at,duration_minutes,status,created_by) VALUES(?,?,?,?,?,'SCHEDULED',?)", args);
    }
    public int windowAdvisingWindowSectionsUpdate(Object ... args) {
        return database.exec("INSERT INTO advising_window_sections(window_id,section_id) VALUES(?,?)", args);
    }
    public int windowNoticesUpdate(Object ... args) {
        return database.exec("INSERT INTO notices(title,body,category,related_window_id) VALUES(?,?,'ADVISING',?)", args);
    }
    public Map < String, Object > setWindowSemestersRecord(Object ... args) {
        return database.one("SELECT id FROM semesters WHERE id=? FOR UPDATE", args);
    }
    public Map < String, Object > setWindowAdvisingWindowsRecord(Object ... args) {
        return database.one("SELECT * FROM advising_windows WHERE id=? FOR UPDATE", args);
    }
    public long setWindowAdvisingWindowSectionsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM advising_window_sections ws JOIN sections s ON s.id=ws.section_id WHERE ws.window_id=? AND s.teacher_id IS NOT NULL AND s.is_offered=TRUE", args);
    }
    public long setWindowAdvisingWindowsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM advising_windows WHERE id<>? AND status='OPEN' AND end_at>CURRENT_TIMESTAMP", args);
    }
    public int setWindowAdvisingWindowsUpdate(Object ... args) {
        return database.exec("UPDATE advising_windows SET status='OPEN',start_at=CURRENT_TIMESTAMP,end_at=TIMESTAMPADD(MINUTE,?,CURRENT_TIMESTAMP) WHERE id=?", args);
    }
    public int setWindowAdvisingWindowsUpdate2(Object ... args) {
        return database.exec("UPDATE advising_windows SET status='CLOSED' WHERE id=?", args);
    }
    public Map < String, Object > deleteWindowSemestersRecord(Object ... args) {
        return database.one("SELECT id FROM semesters WHERE id=? FOR UPDATE", args);
    }
    public Map < String, Object > deleteWindowAdvisingWindowsRecord(Object ... args) {
        return database.one("SELECT * FROM advising_windows WHERE id=? FOR UPDATE", args);
    }
    public int deleteWindowAdvisingWindowsUpdate(Object ... args) {
        return database.exec("UPDATE advising_windows SET status='CANCELLED' WHERE id=?", args);
    }
    public int deleteWindowNoticesUpdate(Object ... args) {
        return database.exec("DELETE FROM notices WHERE related_window_id=?", args);
    }
    public List < Map < String, Object >> accountsUsersList(Object ... args) {
        return database.list("SELECT u.id,u.login_id,u.full_name,u.active,s.subject,s.cgpa,s.status,d.name AS department FROM users u LEFT JOIN students s ON s.user_id=u.id LEFT JOIN teachers t ON t.user_id=u.id LEFT JOIN departments d ON d.id=t.dept_id WHERE u.role=? AND (LOWER(u.full_name) LIKE LOWER(?) OR LOWER(u.login_id) LIKE LOWER(?)) ORDER BY u.id DESC LIMIT 20 OFFSET ?", args);
    }
    public long accountsUsersCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM users WHERE role=? AND (LOWER(full_name) LIKE LOWER(?) OR LOWER(login_id) LIKE LOWER(?))", args);
    }
    public Map < String, Object > studentDetailStudentsRecord(Object ... args) {
        return database.one("SELECT u.full_name,u.login_id,s.*,d.name AS department FROM students s JOIN users u ON u.id=s.user_id JOIN departments d ON d.id=s.dept_id WHERE s.user_id=?", args);
    }
    public Map < String, Object > teacherDetailTeachersRecord(Object ... args) {
        return database.one("SELECT u.full_name,u.login_id,t.*,d.name AS department FROM teachers t JOIN users u ON u.id=t.user_id LEFT JOIN departments d ON d.id=t.dept_id WHERE t.user_id=?", args);
    }
    public List < Map < String, Object >> teacherDetailCatalogueList(Object ... args) {
        return database.list(CatalogSql.SELECT + " WHERE s.teacher_id=? AND s.semester_id=?", args);
    }
    public int activeUsersUpdate(Object ... args) {
        return database.exec("UPDATE users SET active=? WHERE id=?", args);
    }
    public List < Map < String, Object >> choicesTeacherCourseChoicesList(boolean allTeachers, Object ... args) {
        return database.list("SELECT ch.*,u.full_name,u.login_id,c.code,c.name,c.credit,(SELECT COUNT(*) FROM teacher_course_choices x WHERE x.course_id=ch.course_id AND x.semester_id=ch.semester_id AND x.teacher_id<>ch.teacher_id AND x.status='CONFIRMED') AS competing FROM teacher_course_choices ch JOIN users u ON u.id=ch.teacher_id JOIN courses c ON c.id=ch.course_id WHERE ch.semester_id=? AND ch.status<>'SELECTED'" +(allTeachers ? "" : " AND ch.teacher_id=?") + " ORDER BY ch.id DESC", args);
    }
    public int rejectTeacherCourseChoicesUpdate(Object ... args) {
        return database.exec("UPDATE teacher_course_choices SET status='REJECTED',decided_by=?,decided_at=CURRENT_TIMESTAMP WHERE id=? AND status='CONFIRMED'", args);
    }
    public List < Map < String, Object >> offeringsCatalogueList(Object ... args) {
        return database.list(CatalogSql.SELECT + " WHERE s.semester_id=? ORDER BY c.code,s.section_no", args);
    }
    public List < Map < String, Object >> schedulesSectionSchedulesList(Object ... args) {
        return database.list("SELECT day,start_time AS `start`,end_time AS `end` FROM section_schedules WHERE section_id=?", args);
    }
    public List < Map < String, Object >> rosterEnrollmentsList(Object ... args) {
        return database.list("SELECT u.login_id,u.full_name,e.status FROM enrollments e JOIN users u ON u.id=e.student_id WHERE e.section_id=? AND e.status='CONFIRMED'", args);
    }
    public List < Map < String, Object >> deleteWindowAdvisingHoldsList(Object ... args) {
        return database.list("SELECT id,student_id FROM advising_holds WHERE window_id=? AND status='HELD' ORDER BY student_id,id", args);
    }
    public List < Map < String, Object >> windowActionAdvisingHoldsList(Object ... args) {
        return database.list("SELECT id,student_id FROM advising_holds WHERE window_id=? AND status='HELD'", args);
    }
}
