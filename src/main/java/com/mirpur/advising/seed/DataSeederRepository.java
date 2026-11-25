package com.mirpur.advising.seed;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class DataSeederRepository extends BaseRepository {
    public DataSeederRepository(JdbcDatabase database) {
        super(database);
    }
    public long seedUsersCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM users WHERE login_id='admin'", args);
    }
    public int seedUsersUpdate(Object ... args) {
        return database.exec("INSERT INTO users(login_id,password_hash,role,full_name) VALUES(?,?,'ADMIN','Administrator')", args);
    }
    public long seedSemestersCount(Object ... args) {
        return database.count("SELECT COUNT(*) FROM semesters", args);
    }
    public long seedSemestersInsert(Object ... args) {
        return database.insert("INSERT INTO semesters(name,start_date,end_date,is_current) VALUES(?,?,?,TRUE)", args);
    }
    public int seedSemestersUpdate(Object ... args) {
        return database.exec("INSERT INTO semesters(name,start_date,end_date,is_current) VALUES(?,?,?,FALSE)", args);
    }
    public long seedDepartmentsInsert(Object ... args) {
        return database.insert("INSERT INTO departments(code,name,alias) VALUES(?,?,?)", args);
    }
    public int seedCoursesUpdate(Object ... args) {
        return database.exec("INSERT INTO courses(code,name,credit,dept_id) VALUES(?,?,3,?)", args);
    }
    public List < Map < String, Object >> seedCoursesList(Object ... args) {
        return database.list("SELECT id FROM courses WHERE code IN ('CSE101','CSE103') ORDER BY code", args);
    }
    public int seedCoursePrerequisitesUpdate(Object ... args) {
        return database.exec("INSERT INTO course_prerequisites(course_id,prereq_course_id) VALUES(?,?)", args);
    }
    public int seedRoomsUpdate(Object ... args) {
        return database.exec("INSERT INTO rooms(name,capacity) VALUES(?,80)", args);
    }
    public List < Map < String, Object >> seedCoursesList2(Object ... args) {
        return database.list("SELECT * FROM courses", args);
    }
    public long seedSectionsInsert(Object ... args) {
        return database.insert("INSERT INTO sections(course_id,semester_id,section_no,room_id,capacity) VALUES(?,?,?, ?,40)", args);
    }
    public int seedSectionSchedulesUpdate(Object ... args) {
        return database.exec("INSERT INTO section_schedules(section_id,day,start_time,end_time) VALUES(?,?,?,?)", args);
    }
    public long seedAdvisingRulesInsert(Object ... args) {
        return database.insert("INSERT INTO advising_rules(semester_id,min_credit,max_credit,drop_deadline,advising_deadline,drop_rules_text,prerequisite_rules_text,probation_cgpa_threshold) VALUES(?,3,18,?,?,?,?,2.0)", args);
    }
    public int seedCreditLimitBandsUpdate(Object ... args) {
        return database.exec("INSERT INTO credit_limit_bands(rule_id,min_cgpa,max_cgpa,status_label,min_credit,max_credit) VALUES(?,NULL,NULL,'New student (no CGPA)',3,15)", args);
    }
    public int seedCreditLimitBandsUpdate2(Object ... args) {
        return database.exec("INSERT INTO credit_limit_bands(rule_id,min_cgpa,max_cgpa,status_label,min_credit,max_credit) VALUES(?,0,1.99,'Probation',3,9)", args);
    }
    public int seedCreditLimitBandsUpdate3(Object ... args) {
        return database.exec("INSERT INTO credit_limit_bands(rule_id,min_cgpa,max_cgpa,status_label,min_credit,max_credit) VALUES(?,2,2.99,'Normal',3,15)", args);
    }
    public int seedCreditLimitBandsUpdate4(Object ... args) {
        return database.exec("INSERT INTO credit_limit_bands(rule_id,min_cgpa,max_cgpa,status_label,min_credit,max_credit) VALUES(?,3,4,'Good standing',3,18)", args);
    }
    public long seedUsersCount2(Object ... args) {
        return database.count("SELECT id FROM users WHERE login_id='admin'", args);
    }
    public long seedAdvisingWindowsInsert(Object ... args) {
        return database.insert("INSERT INTO advising_windows(semester_id,name,start_at,end_at,status,created_by) VALUES(?,?,?,?,'SCHEDULED',?)", args);
    }
    public int seedNoticesUpdate(Object ... args) {
        return database.exec("INSERT INTO notices(title,body,category,related_window_id) VALUES(?,?,'ADVISING',?)", args);
    }
    public int seedNoticesUpdate2(Object ... args) {
        return database.exec("INSERT INTO notices(title,body,category) VALUES('Welcome to University of Mirpur','Your academic journey starts here. Check this board for academic updates.','GENERAL')", args);
    }
}
