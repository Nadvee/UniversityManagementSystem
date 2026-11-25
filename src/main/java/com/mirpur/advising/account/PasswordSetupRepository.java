package com.mirpur.advising.account;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordSetupRepository extends BaseRepository {
    public PasswordSetupRepository(JdbcDatabase database) { super(database); }

    public List<Map<String,Object>> findForSetup(String loginId) {
        return database.list("SELECT u.id,u.role,u.active,s.completed_at FROM users u JOIN account_password_setup s ON s.user_id=u.id WHERE u.login_id=? FOR UPDATE", loginId);
    }
    public int complete(long userId, String hash) {
        int changed = database.exec("UPDATE account_password_setup SET completed_at=CURRENT_TIMESTAMP WHERE user_id=? AND completed_at IS NULL", userId);
        if (changed == 1) database.exec("UPDATE users SET password_hash=? WHERE id=?", hash, userId);
        return changed;
    }
    public long lockAdmin() {
        return JdbcDatabase.number(database.one("SELECT id FROM users WHERE login_id='admin' AND role='ADMIN' FOR UPDATE"), "id");
    }
    public boolean hasSetupRecord(long userId) {
        return database.count("SELECT COUNT(*) FROM account_password_setup WHERE user_id=?", userId) > 0;
    }
    public void configureAdminOnce(long userId, String hash) {
        database.exec("UPDATE users SET password_hash=? WHERE id=? AND role='ADMIN'", hash, userId);
        database.exec("INSERT INTO account_password_setup(user_id,completed_at) VALUES(?,CURRENT_TIMESTAMP)", userId);
    }
}
