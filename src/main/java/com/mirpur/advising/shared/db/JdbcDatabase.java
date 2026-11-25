package com.mirpur.advising.shared.db;

import com.mirpur.advising.shared.event.AuditEvent;
import com.mirpur.advising.shared.event.EventPublisher;
import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component("db") public class JdbcDatabase {
    private final EventPublisher events;
    private final JdbcTemplate jdbc;
    public JdbcTemplate jdbc() {
        return jdbc;
    }
    public JdbcDatabase(JdbcTemplate jdbc, EventPublisher events) {
        this.events = events;
        this.jdbc = jdbc;
    }
    public List < Map < String, Object >> list(String sql, Object ... args) {
        return jdbc.query(sql,(rs, n) -> {
            Map < String, Object > row = new LinkedHashMap < >();
            var meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i ++) {
                Object v = rs.getObject(i);
                if (v instanceof Timestamp t) v = t.toLocalDateTime().toString();
                else if (v instanceof java.sql.Date || v instanceof Time) v = v.toString();
                row.put(meta.getColumnLabel(i).toLowerCase(Locale.ROOT), v);
            }
            return row;
        }, args);
    }
    public Map < String, Object > one(String sql, Object ... args) {
        var rows = list(sql, args);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found.");
        return rows.get(0);
    }
    public long count(String sql, Object ... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
    public int exec(String sql, Object ... args) {
        return jdbc.update(sql, args);
    }
    public long insert(String sql, Object ... args) {
        var key = new GeneratedKeyHolder();
        jdbc.update(c -> {
            var s = c.prepareStatement(sql, new String[] {
                "id"
            });
            for (int i = 0; i < args.length; i ++) s.setObject(i + 1, args[i]);
            return s;
        }, key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    public long semester() {
        return number(one("SELECT id FROM semesters WHERE is_current=TRUE"), "id");
    }
    public static long number(Map < String, ? > m, String key) {
        if (m == null || !(m.get(key) instanceof Number n)) throw new IllegalArgumentException("Missing numeric field: " + key);
        return n.longValue();
    }
    public static double decimal(Map < String, ? > m, String key) {
        return m.get(key) == null ? 0 :((Number) m.get(key)).doubleValue();
    }
    public static String str(Map < String, ? > m, String key) {
        return Objects.toString(m.get(key), "");
    }
    public static void require(boolean ok, String message) {
        if ( !ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    public void audit(Long actor, String action, String entity, Long id, String detail) {
        events.publish(new AuditEvent(actor, action, entity, id, detail));
    }
}
