package com.mirpur.advising;
import com.fasterxml.jackson.databind.*;
import com.mirpur.advising.shared.db.JdbcDatabase;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:workflow_core;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","app.cache-enabled=false","app.admin-password=test-admin"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkflowTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcDatabase db;
    String admin,teacher,student,other;long first,second,window;
    JsonNode call(String method,String path,String token,Object body,int expected) throws Exception {
        var request=switch(method){case "POST"->post(path);case "PUT"->put(path);case "DELETE"->delete(path);default->get(path);};
        if(token!=null)request.header("Authorization","Bearer "+token);if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response=mvc.perform(request).andReturn().getResponse();assertThat(response.getStatus()).as(method+" "+path+" "+response.getContentAsString()).isEqualTo(expected);
        return response.getContentAsString().isBlank()?json.nullNode():json.readTree(response.getContentAsString());
    }
    String login(String id,String pass) throws Exception {return call("POST","/api/auth/login",null,Map.of("id",id,"password",pass),200).get("token").asText();}
    @Test @Order(1) void accountsAndRoleAccessRemainAvailable() throws Exception {
        assertThat(db.count("SELECT COUNT(*) FROM users")).isEqualTo(1);assertThat(db.count("SELECT COUNT(*) FROM sections")).isEqualTo(100);
        admin=login("admin","test-admin");
        call("POST","/api/admin/teachers",admin,Map.of("name","Teacher","loginId","T001","departmentCode","BBA"),200);
        for(String id:List.of("S001","S002"))call("POST","/api/admin/students",admin,Map.of("name","Student","loginId",id,"subject","BBA"),200);
        for(String id:List.of("T001","S001","S002"))call("POST","/api/auth/setup-password",null,Map.of("id",id,"password",id+"-pass","confirmPassword",id+"-pass"),200);
        teacher=login("T001","T001-pass");student=login("S001","S001-pass");other=login("S002","S002-pass");
        call("GET","/api/admin/students",student,null,403);call("GET","/api/teacher/courses",student,null,403);
    }
    @Test @Order(2) void teachersAndAdminsPrepareAnAdvisingWindow() throws Exception {
        first=db.count("SELECT s.id FROM sections s JOIN courses c ON c.id=s.course_id WHERE c.code='BBA101' AND s.section_no='1'");
        second=db.count("SELECT s.id FROM sections s JOIN courses c ON c.id=s.course_id WHERE c.code='BBA102' AND s.section_no='1'");
        long course1=db.count("SELECT course_id FROM sections WHERE id=?",first),course2=db.count("SELECT course_id FROM sections WHERE id=?",second);
        call("PUT","/api/admin/offerings/"+second,admin,Map.of("courseId",course2,"sectionNo","1","roomId",2,"capacity",40,"offered",true,"schedules",List.of(Map.of("day","THU","start","20:00","end","21:30"))),200);
        for(long course:List.of(course1,course2)){
            call("POST","/api/teacher/choices/"+course,teacher,null,200);call("POST","/api/teacher/choices/"+course+"/confirm",teacher,null,200);
            long choice=db.count("SELECT id FROM teacher_course_choices WHERE teacher_id=(SELECT id FROM users WHERE login_id='T001') AND course_id=?",course);
            call("POST","/api/admin/teacher-choices/"+choice+"/assign",admin,Map.of("sectionId",course==course1?first:second),200);
        }
        window=call("POST","/api/admin/windows",admin,Map.of("name","Core Window","startAt",LocalDateTime.now().plusMinutes(10).toString(),"sectionIds",List.of(first,second)),200).get("id").asLong();
        call("POST","/api/admin/windows/"+window+"/open",admin,null,200);
        assertThat(call("GET","/api/advising/courses",student,null,200).size()).isEqualTo(2);
    }
    @Test @Order(3) void confirmationAndDropOnlyChangeEnrollmentRoutineAndSeats() throws Exception {
        long hold=call("POST","/api/advising/select",student,Map.of("sectionId",first),200).get("id").asLong();
        assertThat(call("POST","/api/advising/select",student,Map.of("sectionId",first),200).get("id").asLong()).isEqualTo(hold);
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?",first)).isEqualTo(1);
        call("POST","/api/advising/confirm",other,Map.of("holdId",hold),404);
        call("POST","/api/advising/confirm",student,Map.of("holdId",hold),200);call("POST","/api/advising/confirm",student,Map.of("holdId",hold),200);
        long secondHold=call("POST","/api/advising/select",student,Map.of("sectionId",second),200).get("id").asLong();
        call("POST","/api/advising/confirm",student,Map.of("holdId",secondHold),200);
        assertThat(call("GET","/api/routine/me",student,null,200).size()).isEqualTo(2);
        call("POST","/api/drop/confirm",student,Map.of("sectionIds",List.of(first),"reason","Schedule change"),200);
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?",first)).isZero();
        call("POST","/api/drop/confirm",student,Map.of("sectionIds",List.of(second),"reason","Too few credits"),400);
        long pending=call("POST","/api/advising/select",other,Map.of("sectionId",first),200).get("id").asLong();
        call("DELETE","/api/admin/windows/"+window,admin,null,200);
        assertThat(db.one("SELECT status FROM advising_holds WHERE id=?",pending).get("status")).isEqualTo("EXPIRED");
        assertThat(call("GET","/api/routine/me",student,null,200).size()).isEqualTo(1);
    }
    @Test @Order(4) void obsoleteBackendEndpointsAreRemoved() throws Exception {
        for(String route:List.of("/api/payment/invoice","/api/grades","/api/pre-advising/offered","/api/rules/current","/api/perf/db"))call("GET",route,student,null,404);
        for(String route:List.of("/api/admin/payments","/api/admin/permissions","/api/admin/audit","/api/admin/loadtest/tokens"))call("GET",route,admin,null,404);
        for(String route:List.of("/api/admin/cache/clear","/api/admin/seat/reconcile","/api/admin/seed"))call("POST",route,admin,Map.of(),404);
        call("POST","/api/teacher/grades/save",teacher,Map.of(),404);call("GET","/api/teacher/sections/"+first+"/gradebook",teacher,null,404);
    }
    @Test @Order(5) void theThirteenRolePagesAndLoginAreKept() throws Exception {
        for(String page:List.of("teacher/dashboard","teacher/course-selection","teacher/my-courses","admin/dashboard","admin/teachers","admin/students","admin/teacher-requests","admin/offerings","admin/advising-control","student/dashboard","student/advising","student/routine","student/drop","login"))assertThat(mvc.perform(get("/"+page+".html")).andReturn().getResponse().getStatus()).as(page).isEqualTo(200);
        for(String page:List.of("mock-gateway","teacher/grade-entry","admin/payments","admin/cache-performance","student/payment","student/grade-report","student/pre-advising"))assertThat(mvc.perform(get("/"+page+".html")).andReturn().getResponse().getStatus()).as(page).isEqualTo(404);
    }
}
