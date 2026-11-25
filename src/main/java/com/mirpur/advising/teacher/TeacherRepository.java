package com.mirpur.advising.teacher;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.catalog.CatalogSql;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class TeacherRepository extends BaseRepository {
    public TeacherRepository(JdbcDatabase database) {
        super(database);
    }
    public Map < String, Object > chooseTeachersRecord(Object ... args) {
        return database.one("SELECT user_id FROM teachers WHERE user_id=? FOR UPDATE", args);
    }
    public long chooseTeacherCourseChoicesCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=? AND semester_id=? AND status='SELECTED'", args);
    }
    public int chooseTeacherCourseChoicesUpdate(Object ... args) {
        return database.exec("UPDATE teacher_course_choices SET status='CONFIRMED',confirmed_at=CURRENT_TIMESTAMP WHERE teacher_id=? AND semester_id=? AND status='SELECTED'", args);
    }
    public long chooseTeacherCourseChoicesCount2(Object ... args) {
        return database.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=? AND semester_id=? AND status IN ('CONFIRMED','ASSIGNED')", args);
    }
    public long chooseSectionsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM sections WHERE course_id=? AND semester_id=? AND is_offered=TRUE", args);
    }
    public long chooseTeacherCourseChoicesCount3(Object ... args) {
        return database.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=? AND course_id=? AND semester_id=? AND status IN ('CONFIRMED','ASSIGNED')", args);
    }
    public int chooseTeacherCourseChoicesUpdate2(Object ... args) {
        return database.exec("DELETE FROM teacher_course_choices WHERE teacher_id=? AND semester_id=? AND status='SELECTED'", args);
    }
    public int chooseTeacherCourseChoicesUpdate3(Object ... args) {
        return database.exec("UPDATE teacher_course_choices SET status='SELECTED',source='TEACHER',confirmed_at=NULL,assigned_section_id=NULL,decided_by=NULL,decided_at=NULL WHERE teacher_id=? AND course_id=? AND semester_id=? AND status='REJECTED'", args);
    }
    public int chooseTeacherCourseChoicesUpdate4(Object ... args) {
        return database.exec("INSERT INTO teacher_course_choices(teacher_id,course_id,semester_id,source,status) VALUES(?,?,?,'TEACHER','SELECTED')", args);
    }
    public List < Map < String, Object >> courseChoices(long teacher, long course, long semester) {
        return database.list("SELECT * FROM teacher_course_choices WHERE teacher_id=? AND course_id=? AND semester_id=?", teacher, course, semester);
    }
    public long activeChoices(long teacher, long semester) {
        return database.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=? AND semester_id=? AND status IN ('SELECTED','CONFIRMED','ASSIGNED')", teacher, semester);
    }
    public int confirmCourseChoice(long teacher, long course, long semester) {
        return database.exec("UPDATE teacher_course_choices SET status='CONFIRMED',confirmed_at=CURRENT_TIMESTAMP WHERE teacher_id=? AND course_id=? AND semester_id=? AND status='SELECTED'", teacher, course, semester);
    }
    public long assignedCourseSections(long teacher, long course, long semester) {
        return database.count("SELECT COUNT(*) FROM sections WHERE teacher_id=? AND course_id=? AND semester_id=?", teacher, course, semester);
    }
    public int removeCourseChoice(long teacher, long course, long semester) {
        return database.exec("DELETE FROM teacher_course_choices WHERE teacher_id=? AND course_id=? AND semester_id=? AND status IN ('SELECTED','CONFIRMED','REJECTED')", teacher, course, semester);
    }
    public void requireCourse(long course) { database.one("SELECT id FROM courses WHERE id=?", course); }
    public List < Map < String, Object >> coursesCoursesList(Object ... args) {
        return database.list("SELECT c.*,d.name AS department,ch.id AS choice_id,ch.status,ch.source,ch.assigned_section_id FROM courses c JOIN departments d ON d.id=c.dept_id LEFT JOIN teacher_course_choices ch ON ch.course_id=c.id AND ch.teacher_id=? AND ch.semester_id=? WHERE EXISTS(SELECT 1 FROM sections s WHERE s.course_id=c.id AND s.semester_id=? AND s.is_offered=TRUE) OR ch.status IN ('SELECTED','CONFIRMED','ASSIGNED') ORDER BY c.code", args);
    }
    public List < Map < String, Object >> sectionsCatalogueList(Object ... args) {
        return database.list(CatalogSql.SELECT + " WHERE s.teacher_id=? AND s.semester_id=?", args);
    }
    public long sectionsAdvisingWindowSectionsCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM advising_window_sections WHERE section_id=?", args);
    }
    public Map < String, Object > studentsSectionsRecord(Object ... args) {
        return database.one("SELECT id FROM sections WHERE id=? AND teacher_id=?", args);
    }
    public List < Map < String, Object >> studentsEnrollmentsList(Object ... args) {
        return database.list("SELECT u.id AS student_id,u.login_id,u.full_name FROM enrollments e JOIN users u ON u.id=e.student_id WHERE e.section_id=? AND e.status='CONFIRMED' ORDER BY u.login_id", args);
    }
}
