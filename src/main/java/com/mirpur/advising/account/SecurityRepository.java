package com.mirpur.advising.account;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class SecurityRepository extends BaseRepository {
    public SecurityRepository(JdbcDatabase database) {
        super(database);
    }
    public Map < String, Object > queryUsersRecord(Object ... args) {
        return database.one("SELECT u.id,u.role,u.active FROM users u WHERE u.id=? AND NOT EXISTS (SELECT 1 FROM account_password_setup s WHERE s.user_id=u.id AND s.completed_at IS NULL)", args);
    }
}
