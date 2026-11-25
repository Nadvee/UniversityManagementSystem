package com.mirpur.advising;

import com.fasterxml.jackson.databind.*;
import com.mirpur.advising.admin.AdminService;
import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.shared.db.JdbcDatabase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:offering_status;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "app.cache-enabled=false", "app.admin-password=status-admin"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class OfferingStatusTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcDatabase db;
    @Autowired AdminService admin;
    @Autowired CachedCatalogService cache;
    String token;
    long actor, section, window;

    @BeforeEach void setUp() throws Exception {
        actor = db.count("SELECT id FROM users WHERE login_id='admin'");
        token = login("admin", "status-admin");
        section = db.count("SELECT s.id FROM sections s JOIN courses c ON c.id=s.course_id WHERE c.code='CSE101' AND s.section_no='1'");
        window=db.insert("INSERT INTO advising_windows(semester_id,name,start_at,end_at,status) VALUES(?,'Status Test','2000-01-01 00:00:00','2099-01-01 00:00:00','OPEN')",db.semester());
        db.exec("INSERT INTO advising_window_sections(window_id,section_id) VALUES(?,?)",window,section);
    }
    String login(String id, String password) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id", id, "password", password)))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        return json.readTree(response.getContentAsString()).get("token").asText();
    }
    JsonNode status(long id, String auth, Object body, int expected) throws Exception {
        var request = patch("/api/admin/offerings/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (auth != null) request.header("Authorization", "Bearer " + auth);
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);
        return json.readTree(response.getContentAsString());
    }
    boolean offered(long id) {
        return Boolean.TRUE.equals(db.one("SELECT is_offered FROM sections WHERE id=?", id).get("is_offered"));
    }
    JsonNode listedSection() throws Exception {
        var response = mvc.perform(get("/api/admin/offerings").header("Authorization", "Bearer " + token)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        for (var row : json.readTree(response.getContentAsString())) if (row.get("id").asLong() == section) return row;
        throw new AssertionError("Section missing from admin directory");
    }
    @Test void enabledAndDisabledPersistWithoutChangingSectionSchedulesOrEnrollments() throws Exception {
        admin.account(actor, "STUDENT", Map.of("loginId", "S980001", "name", "Status Student", "subject", "CSE"));
        long student = db.count("SELECT id FROM users WHERE login_id='S980001'");
        db.exec("UPDATE sections SET is_offered=TRUE,seats_taken=1 WHERE id=?", section);
        db.exec("INSERT INTO enrollments(student_id,section_id,semester_id,status) VALUES(?,?,?,'CONFIRMED')", student, section, db.semester());
        var originalSection = db.one("SELECT * FROM sections WHERE id=?", section);
        originalSection.remove("is_offered");
        var schedules = db.list("SELECT * FROM section_schedules WHERE section_id=? ORDER BY id", section);
        var enrollments = db.list("SELECT * FROM enrollments WHERE section_id=? ORDER BY id", section);
        var routine = cache.routine(student, db.semester());
        assertThat(cache.window(window)).anyMatch(row -> JdbcDatabase.number(row, "id") == section);
        long auditBefore = db.count("SELECT COUNT(*) FROM audit_logs WHERE action='OFFERING_STATUS' AND entity_id=?", section);

        for (boolean value : List.of(false, true)) {
            var result = status(section, token, Map.of("offered", value), 200);
            assertThat(result.get("offered").asBoolean()).isEqualTo(value);
            assertThat(offered(section)).isEqualTo(value);
            assertThat(listedSection().get("is_offered").asBoolean()).isEqualTo(value);
            assertThat(cache.window(window).stream().anyMatch(row -> JdbcDatabase.number(row, "id") == section)).isEqualTo(value);
            var after = db.one("SELECT * FROM sections WHERE id=?", section); after.remove("is_offered");
            assertThat(after).isEqualTo(originalSection);
            assertThat(db.list("SELECT * FROM section_schedules WHERE section_id=? ORDER BY id", section)).isEqualTo(schedules);
            assertThat(db.list("SELECT * FROM enrollments WHERE section_id=? ORDER BY id", section)).isEqualTo(enrollments);
            assertThat(cache.routine(student, db.semester())).isEqualTo(routine);
            long audit = db.count("SELECT COUNT(*) FROM audit_logs WHERE action='OFFERING_STATUS' AND entity_id=?", section);
            status(section, token, Map.of("offered", value), 200);
            assertThat(offered(section)).isEqualTo(value);
            assertThat(db.count("SELECT COUNT(*) FROM audit_logs WHERE action='OFFERING_STATUS' AND entity_id=?", section)).isEqualTo(audit);
        }
        assertThat(db.count("SELECT COUNT(*) FROM audit_logs WHERE action='OFFERING_STATUS' AND entity_id=?", section)).isEqualTo(auditBefore + 2);
    }
    @Test void offeringStatusRequiresAnAdminAndABooleanValue() throws Exception {
        db.exec("UPDATE sections SET is_offered=TRUE WHERE id=?", section);
        status(section, null, Map.of("offered", false), 401);
        for (String role : List.of("STUDENT", "TEACHER")) {
            String id = role.equals("STUDENT") ? "S980002" : "T980002";
            admin.account(actor, role, Map.of("loginId", id, "name", "Status Role Test", "subject", "CSE", "departmentCode", "CSE"));
            var setup = mvc.perform(post("/api/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id", id, "password", "status-test", "confirmPassword", "status-test")))).andReturn().getResponse();
            assertThat(setup.getStatus()).isEqualTo(200);
            status(section, login(id, "status-test"), Map.of("offered", false), 403);
        }
        for (Object invalid : List.of(Map.of(), Map.of("offered", "false"), Map.of("offered", 0), Collections.singletonMap("offered", null))) status(section, token, invalid, 400);
        status(999999999, token, Map.of("offered", false), 404);
        assertThat(offered(section)).isTrue();
    }
}
