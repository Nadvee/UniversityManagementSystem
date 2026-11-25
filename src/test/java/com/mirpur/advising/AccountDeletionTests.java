package com.mirpur.advising;

import com.fasterxml.jackson.databind.*;
import com.mirpur.advising.admin.AdminService;
import com.mirpur.advising.shared.db.JdbcDatabase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:account_deletion;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "app.cache-enabled=false", "app.admin-password=delete-admin"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class AccountDeletionTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AdminService admin;
    @Autowired JdbcDatabase db;
    private static final AtomicInteger sequence = new AtomicInteger(80000);
    long actor;
    String token;
    record Account(long id, String login, String token) {}
    record Records(Account student, Account other, Account teacher, long confirmed, long held, long dropped, long prerequisite, long otherPrerequisite) {}

    @BeforeEach void signIn() throws Exception {
        actor = db.count("SELECT id FROM users WHERE login_id='admin'");
        token = call("POST", "/api/auth/login", null, Map.of("id", "admin", "password", "delete-admin"), 200).get("token").asText();
    }
    JsonNode call(String method, String path, String auth, Object body, int expected) throws Exception {
        var request = switch(method) { case "POST" -> post(path); case "DELETE" -> delete(path); default -> get(path); };
        if (auth != null) request.header("Authorization", "Bearer " + auth);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as(method + " " + path + " " + response.getContentAsString()).isEqualTo(expected);
        return response.getContentAsString().isBlank() ? json.nullNode() : json.readTree(response.getContentAsString());
    }
    Account account(String role) throws Exception {
        String login = (role.equals("STUDENT") ? "S" : "T") + sequence.incrementAndGet();
        admin.account(actor, role, Map.of("loginId", login, "name", "Delete Test " + login, "subject", "CSE", "departmentCode", "CSE"));
        call("POST", "/api/auth/setup-password", null, Map.of("id", login, "password", "delete-test", "confirmPassword", "delete-test"), 200);
        String auth = call("POST", "/api/auth/login", null, Map.of("id", login, "password", "delete-test"), 200).get("token").asText();
        return new Account(db.count("SELECT id FROM users WHERE login_id=?", login), login, auth);
    }
    long section(long teacher, int seats) {
        long course = db.count("SELECT id FROM courses WHERE code='CSE101'");
        return db.insert("INSERT INTO sections(course_id,semester_id,section_no,teacher_id,capacity,seats_taken) VALUES(?,?,?,?,40,?)", course, db.semester(), "DELETE-" + sequence.incrementAndGet(), teacher, seats);
    }
    long enrollment(long student, long section, String status) {
        return db.insert("INSERT INTO enrollments(student_id,section_id,semester_id,status) VALUES(?,?,?,?)", student, section, db.semester(), status);
    }
    long prerequisiteRecord(long student, long section) {
        long course = db.count("SELECT course_id FROM sections WHERE id=?", section);
        return db.insert("INSERT INTO prerequisite_records(student_id,course_id,semester_id,passed) VALUES(?,?,?,TRUE)", student, course, db.semester());
    }
    Records records() throws Exception {
        var student = account("STUDENT"); var other = account("STUDENT"); var teacher = account("TEACHER");
        long confirmed = section(teacher.id(), 2), held = section(teacher.id(), 1), dropped = section(teacher.id(), 1);
        enrollment(student.id(), confirmed, "CONFIRMED"); enrollment(student.id(), dropped, "DROPPED");
        enrollment(other.id(), confirmed, "CONFIRMED"); enrollment(other.id(), dropped, "CONFIRMED");
        for (var hold : List.of(Map.entry(confirmed, "CONFIRMED"), Map.entry(held, "HELD"), Map.entry(held, "EXPIRED")))
            db.exec("INSERT INTO advising_holds(student_id,section_id,source,expires_at,status) VALUES(?,?,'ADVISING','2099-01-01 00:00:00',?)", student.id(), hold.getKey(), hold.getValue());
        long prerequisite = prerequisiteRecord(student.id(), confirmed), otherPrerequisite = prerequisiteRecord(other.id(), confirmed);
        long course = db.count("SELECT course_id FROM sections WHERE id=?", confirmed);
        db.exec("INSERT INTO teacher_course_choices(teacher_id,course_id,semester_id,source,status,assigned_section_id,decided_by) VALUES(?,?,?,'SELF','ASSIGNED',?,?)", teacher.id(), course, db.semester(), confirmed, actor);
        db.audit(student.id(), "TEST_ACTIVITY", "user", student.id(), "Keep this audit history.");
        return new Records(student, other, teacher, confirmed, held, dropped, prerequisite, otherPrerequisite);
    }
    @Test void adminCanDeleteActiveAndInactiveAccountsAndReuseTheirLoginIds() throws Exception {
        for (String role : List.of("STUDENT", "TEACHER")) for (boolean active : List.of(true, false)) {
            var a = account(role); db.exec("UPDATE users SET active=? WHERE id=?", active, a.id());
            String path = "/api/admin/" + (role.equals("STUDENT") ? "students/" : "teachers/") + a.id();
            call("DELETE", path, token, null, 200);
            assertThat(db.count("SELECT COUNT(*) FROM users WHERE id=?", a.id())).isZero();
            assertThat(db.count("SELECT COUNT(*) FROM account_password_setup WHERE user_id=?", a.id())).isZero();
            assertThat(call("GET", "/api/admin/" + (role.equals("STUDENT") ? "students" : "teachers") + "?q=" + a.login(), token, null, 200).get("total").asInt()).isZero();
            call("GET", "/api/auth/me", a.token(), null, 401);
            call("POST", "/api/auth/login", null, Map.of("id", a.login(), "password", "delete-test"), 401);
            call("DELETE", path, token, null, 404);
            assertThat(db.count("SELECT COUNT(*) FROM audit_logs WHERE action='DELETE_ACCOUNT' AND entity_id=?", a.id())).isEqualTo(1);
            admin.account(actor, role, Map.of("loginId", a.login(), "name", "Replacement", "subject", "CSE", "departmentCode", "CSE"));
        }
    }
    @Test void deletionRejectsNonAdminsWrongAccountTypesAndMissingAccounts() throws Exception {
        var student = account("STUDENT"); var teacher = account("TEACHER");
        for (String path : List.of("/api/admin/students/" + student.id(), "/api/admin/teachers/" + teacher.id())) {
            call("DELETE", path, null, null, 401);
            call("DELETE", path, student.token(), null, 403);
            call("DELETE", path, teacher.token(), null, 403);
        }
        call("DELETE", "/api/admin/teachers/" + student.id(), token, null, 404);
        call("DELETE", "/api/admin/students/" + teacher.id(), token, null, 404);
        call("DELETE", "/api/admin/students/" + actor, token, null, 400);
        call("DELETE", "/api/admin/teachers/999999999", token, null, 404);
        assertThat(db.count("SELECT COUNT(*) FROM users WHERE id IN (?,?)", student.id(), teacher.id())).isEqualTo(2);
    }
    @Test void deletingStudentCleansRelatedRecordsAndOnlyReleasesOccupiedSeats() throws Exception {
        var f = records(); long id = f.student().id();
        call("DELETE", "/api/admin/students/" + id, token, null, 200);
        for (String table : List.of("enrollments", "advising_holds", "prerequisite_records"))
            assertThat(db.count("SELECT COUNT(*) FROM " + table + " WHERE student_id=?", id)).as(table).isZero();
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?", f.confirmed())).isEqualTo(1);
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?", f.held())).isZero();
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?", f.dropped())).isEqualTo(1);
        assertThat(db.count("SELECT COUNT(*) FROM prerequisite_records WHERE id=?", f.otherPrerequisite())).isEqualTo(1);
        assertThat(db.count("SELECT COUNT(*) FROM audit_logs WHERE entity_id=? AND action='TEST_ACTIVITY' AND actor_id IS NULL", id)).isEqualTo(1);
    }
    @Test void deletingTeacherUnassignsSectionsAndPreservesStudentAcademicRecords() throws Exception {
        var f = records(); long id = f.teacher().id();
        call("DELETE", "/api/admin/teachers/" + id, token, null, 200);
        assertThat(db.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=?", id)).isZero();
        assertThat(db.one("SELECT teacher_id FROM sections WHERE id=?", f.confirmed()).get("teacher_id")).isNull();
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?", f.confirmed())).isEqualTo(2);
        assertThat(db.count("SELECT COUNT(*) FROM prerequisite_records WHERE id IN (?,?)", f.prerequisite(), f.otherPrerequisite())).isEqualTo(2);
        assertThat(db.count("SELECT COUNT(*) FROM enrollments WHERE student_id=?", f.student().id())).isEqualTo(2);
        call("GET", "/api/auth/me", f.teacher().token(), null, 401);
    }
    @Test void failedDeletionRollsBackReleasedSeatsAndRecordChanges() throws Exception {
        var f = records(); long id = f.student().id();
        db.exec("UPDATE sections SET seats_taken=0 WHERE id=?", f.held());
        call("DELETE", "/api/admin/students/" + id, token, null, 400);
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?", f.confirmed())).isEqualTo(2);
        assertThat(db.count("SELECT COUNT(*) FROM enrollments WHERE student_id=?", id)).isEqualTo(2);
        assertThat(db.count("SELECT COUNT(*) FROM users WHERE id=?", id)).isEqualTo(1);
        assertThat(db.count("SELECT COUNT(*) FROM prerequisite_records WHERE id=?", f.prerequisite())).isEqualTo(1);
        assertThat(db.count("SELECT COUNT(*) FROM audit_logs WHERE entity_id=? AND action='DELETE_ACCOUNT'", id)).isZero();
    }
}
