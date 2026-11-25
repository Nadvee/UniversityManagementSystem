package com.mirpur.advising.account;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class CommonRepository extends BaseRepository {
    public CommonRepository(JdbcDatabase database) {
        super(database);
    }
    public List < Map < String, Object >> loginUsersList(Object ... args) {
        return database.list("SELECT u.* FROM users u WHERE u.login_id=? AND NOT EXISTS (SELECT 1 FROM account_password_setup s WHERE s.user_id=u.id AND s.completed_at IS NULL)", args);
    }
    public Map < String, Object > meUsersRecord(Object ... args) {
        return database.one("SELECT id,login_id,full_name,role FROM users WHERE id=?", args);
    }
    public List < Map < String, Object >> referenceDepartmentsList(Object ... args) {
        return database.list("SELECT * FROM departments", args);
    }
    public List < Map < String, Object >> referenceCoursesList(Object ... args) {
        return database.list("SELECT * FROM courses ORDER BY code", args);
    }
    public List < Map < String, Object >> referenceRoomsList(Object ... args) {
        return database.list("SELECT * FROM rooms", args);
    }
    public List < Map < String, Object >> referenceSemestersList(Object ... args) {
        return database.list("SELECT * FROM semesters ORDER BY start_date DESC", args);
    }
    public List < Map < String, Object >> noticesNoticesList(Object ... args) {
        return database.list("SELECT n.*,w.start_at,w.end_at,w.status AS window_status FROM notices n LEFT JOIN advising_windows w ON w.id=n.related_window_id WHERE n.expires_at IS NULL OR n.expires_at>CURRENT_TIMESTAMP ORDER BY n.published_at DESC", args);
    }
}
