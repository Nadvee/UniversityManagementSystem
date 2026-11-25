package com.mirpur.advising.account;

import com.mirpur.advising.shared.config.RuntimeSettings;
import com.mirpur.advising.shared.db.JdbcDatabase;
import com.mirpur.advising.shared.security.Access;
import io.jsonwebtoken.Jwts;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.stereotype.Service public class CommonService {
    private final java.time.Clock clock;
    private final CommonRepository db;
    private final Access access;
    private final PasswordEncoder encoder;
    private final SecretKey key;
    private final String dummyHash;
    private final int hours;
    public CommonService(CommonRepository db, Access access, PasswordEncoder encoder, SecretKey key, RuntimeSettings settings, java.time.Clock clock) {
        this.clock = clock;
        this.hours = settings.app().tokenHours();
        this.db = db;
        this.access = access;
        this.encoder = encoder;
        this.key = key;
        dummyHash = encoder.encode(UUID.randomUUID().toString());
    }
    public Map < String, Object > login(Map < String, String > body) {
        var rows = db.loginUsersList(Objects.toString(body.get("id"), ""));
        var u = rows.isEmpty() ? null : rows.get(0);
        boolean valid = encoder.matches(Objects.toString(body.get("password"), ""), u == null ? dummyHash : JdbcDatabase.str(u, "password_hash"));
        if ( !valid || u == null || !Boolean.TRUE.equals(u.get("active"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID or password.");
        String token = Jwts.builder().subject(u.get("id").toString()).claim("role", u.get("role")).issuedAt(Date.from(clock.instant())).expiration(Date.from(clock.instant().plusSeconds(hours * 3600L))).signWith(key).compact();
        return Map.of("token", token, "role", u.get("role"), "name", u.get("full_name"), "id", u.get("login_id"));
    }
    public Map < String, Object > me() {
        return db.meUsersRecord(access.id());
    }
    public Map < String, Object > reference() {
        return Map.of("departments", db.referenceDepartmentsList(), "courses", db.referenceCoursesList(), "rooms", db.referenceRoomsList(), "semesters", db.referenceSemestersList(), "semesterId", db.semester());
    }
    public Object notices() {
        return db.noticesNoticesList();
    }
}
