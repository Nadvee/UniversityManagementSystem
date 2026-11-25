package com.mirpur.advising;

import com.fasterxml.jackson.databind.*;
import com.mirpur.advising.admin.AdminService;
import com.mirpur.advising.shared.db.JdbcDatabase;
import com.mirpur.advising.seed.DataSeeder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:password_setup;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","app.cache-enabled=false"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class PasswordSetupTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AdminService admin;
    @Autowired JdbcDatabase db;
    @Autowired PasswordEncoder encoder;
    @Autowired DataSeeder seeder;
    private static final AtomicInteger sequence=new AtomicInteger(7000);

    String create(String role) {
        String id=(role.equals("STUDENT")?"S":"T")+sequence.incrementAndGet();
        long actor=db.count("SELECT id FROM users WHERE login_id='admin'");
        admin.account(actor,role,Map.of("loginId",id,"name","Password Setup Test","departmentCode","CSE","subject","CSE"));
        return id;
    }
    int setupStatus(String id,String password,String confirmation)throws Exception {
        return mvc.perform(post("/api/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id",id,"password",password,"confirmPassword",confirmation)))).andReturn().getResponse().getStatus();
    }
    JsonNode login(String id,String password,int status)throws Exception {
        var response=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("id",id,"password",password)))).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(status);
        return json.readTree(response.getContentAsString());
    }

    @Test void newlyCreatedStudentAndTeacherMustSetTheirOwnPassword()throws Exception {
        for(String role:List.of("STUDENT","TEACHER")){
            String id=create(role),password=role.equals("STUDENT")?"s123":"t123";
            login(id,"1234",401);login(id,password,401);
            assertThat(setupStatus(" "+id+" ",password,password)).isEqualTo(200);
            assertThat(login(id,password,200).get("role").asText()).isEqualTo(role);
            String hash=db.one("SELECT password_hash FROM users WHERE login_id=?",id).get("password_hash").toString();
            assertThat(hash).isNotEqualTo(password);assertThat(encoder.matches(password,hash)).isTrue();
            assertThat(setupStatus(id,"another-secret","another-secret")).isEqualTo(400);
            login(id,password,200);login(id,"another-secret",401);
        }
    }
    @Test void rejectsUnknownInactiveAdminAndAlreadyExistingAccounts()throws Exception {
        long count=db.count("SELECT COUNT(*) FROM users");
        assertThat(setupStatus("S99999999","my-secret-1","my-secret-1")).isEqualTo(400);
        assertThat(db.count("SELECT COUNT(*) FROM users")).isEqualTo(count);
        assertThat(setupStatus("admin","my-secret-1","my-secret-1")).isEqualTo(400);
        String disabled=create("STUDENT");db.exec("UPDATE users SET active=FALSE WHERE login_id=?",disabled);
        assertThat(setupStatus(disabled,"my-secret-1","my-secret-1")).isEqualTo(400);
        String legacy="S"+sequence.incrementAndGet();
        db.exec("INSERT INTO users(login_id,password_hash,role,full_name) VALUES(?,?,'STUDENT','Legacy account')",legacy,encoder.encode("existing-secret"));
        assertThat(setupStatus(legacy,"my-secret-1","my-secret-1")).isEqualTo(400);
        login(legacy,"existing-secret",200);
    }
    @Test void passwordValidationDoesNotConsumeFirstTimeSetup()throws Exception {
        String id=create("TEACHER");
        assertThat(setupStatus(id,"123","123")).isEqualTo(400);
        assertThat(setupStatus(id,"abcdefgh","different")).isEqualTo(400);
        assertThat(setupStatus(id," ".repeat(4)," ".repeat(4))).isEqualTo(400);
        assertThat(setupStatus(id,"a".repeat(73),"a".repeat(73))).isEqualTo(400);
        assertThat(setupStatus(id,"অ".repeat(25),"অ".repeat(25))).isEqualTo(400);
        assertThat(setupStatus(id,"1234","1234")).isEqualTo(200);
        login(id,"1234",200);
    }
    @Test void simultaneousRequestsCannotReplaceTheWinningPassword()throws Exception {
        String id=create("STUDENT");var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->{gate.await();return setupStatus(id,"first-password","first-password");});
            var second=pool.submit(()->{gate.await();return setupStatus(id,"second-password","second-password");});
            gate.countDown();int firstStatus=first.get(15,TimeUnit.SECONDS),secondStatus=second.get(15,TimeUnit.SECONDS);
            assertThat(List.of(firstStatus,secondStatus)).containsExactlyInAnyOrder(200,400);
            login(id,firstStatus==200?"first-password":"second-password",200);
            login(id,firstStatus==200?"second-password":"first-password",401);
        } finally {pool.shutdownNow();}
    }
    @Test void adminDefaultAndOneTimeUpgradeAreAppliedWithoutRestartResets()throws Exception {
        login("admin","admin",200);
        long id=db.count("SELECT id FROM users WHERE login_id='admin'");
        db.exec("DELETE FROM account_password_setup WHERE user_id=?",id);
        db.exec("UPDATE users SET password_hash=? WHERE id=?",encoder.encode("change-me-admin"),id);
        seeder.seed();login("admin","admin",200);login("admin","change-me-admin",401);
        String hash=db.one("SELECT password_hash FROM users WHERE id=?",id).get("password_hash").toString();
        seeder.seed();assertThat(db.one("SELECT password_hash FROM users WHERE id=?",id).get("password_hash")).isEqualTo(hash);
    }
}
