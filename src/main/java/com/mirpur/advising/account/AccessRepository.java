package com.mirpur.advising.account;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class AccessRepository extends BaseRepository {
    public AccessRepository(JdbcDatabase database) {
        super(database);
    }
    public Map < String, Object > userRoleRecord(Object ... args) {
        return database.one("SELECT role FROM users WHERE id=?", args);
    }
}
