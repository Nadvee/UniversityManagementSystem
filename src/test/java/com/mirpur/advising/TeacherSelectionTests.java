package com.mirpur.advising;

import com.fasterxml.jackson.databind.*;
import com.mirpur.advising.admin.AdminService;
import com.mirpur.advising.shared.db.JdbcDatabase;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:teacher_selection;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "app.cache-enabled=false", "app.admin-password=selection-admin"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class TeacherSelectionTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcDatabase db; @Autowired AdminService admin;
    private static final AtomicInteger sequence = new AtomicInteger(940000);
    long actor, teacher; String token, adminToken;
    record Account(long id, String token) {}
    @BeforeEach void setUp() throws Exception {
        actor = db.count("SELECT id FROM users WHERE login_id='admin'"); adminToken = login("admin", "selection-admin");
        var account = account("TEACHER"); teacher = account.id(); token = account.token();
    }
    JsonNode call(String method, String path, String auth, Object body, int expected) throws Exception {
        var request = switch(method) { case "POST" -> post(path); case "DELETE" -> delete(path); default -> get(path); };
        if (auth != null) request.header("Authorization", "Bearer " + auth);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as(method + " " + path + " " + response.getContentAsString()).isEqualTo(expected);
        return json.readTree(response.getContentAsString());
    }
    String login(String id, String pass) throws Exception { return call("POST", "/api/auth/login", null, Map.of("id", id, "password", pass), 200).get("token").asText(); }
    Account account(String role) throws Exception {
        String id = (role.equals("TEACHER") ? "T" : "S") + sequence.incrementAndGet();
        admin.account(actor, role, Map.of("name", "Selection Test", "loginId", id, "departmentCode", "CSE", "subject", "CSE"));
        call("POST", "/api/auth/setup-password", null, Map.of("id", id, "password", "selection-test", "confirmPassword", "selection-test"), 200);
        return new Account(db.count("SELECT id FROM users WHERE login_id=?", id), login(id, "selection-test"));
    }
    long course(int number) { return db.count("SELECT id FROM courses WHERE code=?", "CSE" + number); }
    String path(long course) { return "/api/teacher/choices/" + course; }
    String status(long user, long course) { return db.one("SELECT status FROM teacher_course_choices WHERE teacher_id=? AND course_id=? AND semester_id=?", user, course, db.semester()).get("status").toString(); }
    JsonNode listed(long course) throws Exception { for (var row : call("GET", "/api/teacher/courses", token, null, 200)) if (row.get("id").asLong() == course) return row; throw new AssertionError("Course not listed"); }
    @Test void selectionConfirmationAndRemovalAreSavedPerCourse() throws Exception {
        long first = course(101), second = course(102);
        call("POST", path(first), token, null, 200); call("POST", path(second), token, null, 200); call("POST", path(first), token, null, 200);
        assertThat(db.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=?", teacher)).isEqualTo(2);
        assertThat(listed(first).get("status").asText()).isEqualTo("SELECTED");
        call("POST", path(first) + "/confirm", token, null, 200); call("POST", path(first) + "/confirm", token, null, 200);
        assertThat(status(teacher, first)).isEqualTo("CONFIRMED"); assertThat(status(teacher, second)).isEqualTo("SELECTED");
        call("DELETE", path(second), token, null, 200); call("DELETE", path(first), token, null, 200); call("DELETE", path(first), token, null, 200);
        assertThat(db.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=?", teacher)).isZero();
        assertThat(listed(first).get("status").isNull()).isTrue(); call("POST", path(first), token, null, 200);
        assertThat(status(teacher, first)).isEqualTo("SELECTED");
    }
    @Test void otherTeachersAndAssignedSectionsAreProtected() throws Exception {
        long course = course(101); var other = account("TEACHER");
        call("POST", path(course), token, null, 200); call("POST", path(course) + "/confirm", other.token(), null, 400); call("DELETE", path(course), other.token(), null, 200);
        assertThat(status(teacher, course)).isEqualTo("SELECTED");
        call("POST", path(course), other.token(), null, 200); call("POST", path(course) + "/confirm", other.token(), null, 200);
        call("POST", path(course) + "/confirm", token, null, 200);
        long section = db.count("SELECT id FROM sections WHERE course_id=? AND section_no='1' AND semester_id=?", course, db.semester());
        db.exec("UPDATE sections SET teacher_id=? WHERE id=?", teacher, section);
        db.exec("UPDATE teacher_course_choices SET status='ASSIGNED',assigned_section_id=? WHERE teacher_id=? AND course_id=?", section, teacher, course);
        call("DELETE", path(course), token, null, 400);
        assertThat(db.count("SELECT teacher_id FROM sections WHERE id=?", section)).isEqualTo(teacher);
        assertThat(status(teacher, course)).isEqualTo("ASSIGNED"); assertThat(status(other.id(), course)).isEqualTo("CONFIRMED");
    }
    @Test void concurrentSelectionsRespectTheFourCourseLimit() throws Exception {
        for (int number : List.of(101,102,103)) call("POST", path(course(number)), token, null, 200);
        var pool = Executors.newFixedThreadPool(2); var gate = new CountDownLatch(1);
        try {
            var futures = new ArrayList<Future<Integer>>();
            for (int number : List.of(104,105)) { String requestPath = path(course(number)); futures.add(pool.submit(() -> { gate.await(); return mvc.perform(post(requestPath).header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus(); })); }
            gate.countDown(); assertThat(List.of(futures.get(0).get(10,TimeUnit.SECONDS),futures.get(1).get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,400);
        } finally { pool.shutdownNow(); }
        assertThat(db.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=?", teacher)).isEqualTo(4);
        call("POST", path(course(101)) + "/confirm", token, null, 200); call("DELETE", path(course(101)), token, null, 200); call("POST", path(course(106)), token, null, 200);
        assertThat(db.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=?", teacher)).isEqualTo(4);
    }
    @Test void rolesAndActiveAccountsAreEnforced() throws Exception {
        long course = course(101); var student = account("STUDENT");
        call("POST", path(course), null, null, 401); call("POST", path(course), adminToken, null, 403); call("POST", path(course), student.token(), null, 403);
        call("POST", path(course), token, null, 200);
        db.exec("UPDATE users SET active=FALSE WHERE id=?", teacher);
        call("POST", path(course), token, null, 401); call("POST", path(course) + "/confirm", token, null, 401); call("DELETE", path(course), token, null, 401);
        assertThat(status(teacher, course)).isEqualTo("SELECTED");
    }
    @Test void disabledSelectedCoursesRemainVisibleAndRemovable() throws Exception {
        long course = course(107); call("POST", path(course), token, null, 200);
        db.exec("UPDATE sections SET is_offered=FALSE WHERE course_id=? AND semester_id=?", course, db.semester());
        try { assertThat(listed(course).get("status").asText()).isEqualTo("SELECTED"); call("POST", path(course) + "/confirm", token, null, 400); call("DELETE", path(course), token, null, 200); assertThat(db.count("SELECT COUNT(*) FROM teacher_course_choices WHERE teacher_id=? AND course_id=?", teacher,course)).isZero(); }
        finally { db.exec("UPDATE sections SET is_offered=TRUE WHERE course_id=? AND semester_id=?", course, db.semester()); }
    }
}
