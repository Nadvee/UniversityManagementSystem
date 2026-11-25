package com.mirpur.advising;
import com.fasterxml.jackson.databind.*;
import com.mirpur.advising.admin.*;
import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.shared.db.JdbcDatabase;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:offering_deletion_core;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=DAY;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","app.cache-enabled=false","app.admin-password=delete-offering-admin"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class OfferingDeletionTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcDatabase db; @Autowired AdminService admin; @Autowired OfferingDeletionService service; @Autowired CachedCatalogService cache;
    @SpyBean OfferingDeletionRepository repository;
    static final AtomicInteger sequence=new AtomicInteger(960000); long actor; String token;
    @BeforeEach void login() throws Exception { actor=db.count("SELECT id FROM users WHERE login_id='admin'");token=call("POST","/api/auth/login",null,Map.of("id","admin","password","delete-offering-admin"),200).get("token").asText(); }
    JsonNode call(String method,String path,String auth,Object body,int expected) throws Exception {
        var request=switch(method){case "POST"->post(path);case "DELETE"->delete(path);case "PATCH"->patch(path);default->get(path);};
        if(auth!=null)request.header("Authorization","Bearer "+auth);if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response=mvc.perform(request).andReturn().getResponse();assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);return json.readTree(response.getContentAsString());
    }
    long account(String role) { String id=(role.equals("STUDENT")?"S":"T")+sequence.incrementAndGet();admin.account(actor,role,Map.of("loginId",id,"name","Offering Test","subject","CSE","departmentCode","CSE"));return db.count("SELECT id FROM users WHERE login_id=?",id); }
    long section(String no,Long teacher,int seats) {return db.insert("INSERT INTO sections(course_id,semester_id,section_no,teacher_id,room_id,capacity,seats_taken) VALUES((SELECT id FROM courses WHERE code='CSE101'),?,?,?,1,40,?)",db.semester(),no,teacher,seats);}
    @Test void occupiedSectionsCanBeRemovedWithoutDeletingAccountsOrOtherSections() throws Exception {
        long teacher=account("TEACHER"),student=account("STUDENT"),holder=account("STUDENT");String no="DELETE-"+sequence.incrementAndGet();long section=section(no,teacher,2),other=section("OTHER-"+sequence.incrementAndGet(),teacher,0);
        db.exec("INSERT INTO enrollments(student_id,section_id,semester_id,status) VALUES(?,?,?,'CONFIRMED')",student,section,db.semester());
        db.exec("INSERT INTO advising_holds(student_id,section_id,source,expires_at,status) VALUES(?,?,'ADVISING','2099-01-01 00:00:00','HELD')",holder,section);
        long window=db.insert("INSERT INTO advising_windows(semester_id,name,start_at,end_at,status) VALUES(?,'Delete Test','2000-01-01 00:00:00','2099-01-01 00:00:00','OPEN')",db.semester());
        db.exec("INSERT INTO advising_window_sections(window_id,section_id) VALUES(?,?)",window,section);
        call("DELETE","/api/admin/offerings/"+section,token,null,200);
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?",section)).isZero();
        assertThat(db.count("SELECT COUNT(*) FROM enrollments WHERE section_id=? AND status='CONFIRMED'",section)).isZero();
        assertThat(db.count("SELECT COUNT(*) FROM advising_holds WHERE section_id=? AND status='HELD'",section)).isZero();
        assertThat(db.count("SELECT COUNT(*) FROM users WHERE id IN (?,?)",student,holder)).isEqualTo(2);
        assertThat(db.count("SELECT teacher_id FROM sections WHERE id=?",other)).isEqualTo(teacher);
        assertThat(cache.window(window)).isEmpty();
        call("PATCH","/api/admin/offerings/"+section+"/status",token,Map.of("offered",true),404);
        call("DELETE","/api/admin/offerings/"+section,token,null,200);
        assertThat(db.count("SELECT COUNT(*) FROM audit_logs WHERE action='DELETE_OFFERING' AND entity_id=?",section)).isEqualTo(1);
    }
    @Test void originalSectionNumberAndRoomScheduleCanBeReused() throws Exception {
        String no="REUSE-"+sequence.incrementAndGet();long section=section(no,null,0);
        db.exec("INSERT INTO section_schedules(section_id,day,start_time,end_time) VALUES(?,'THU','21:00','22:00')",section);
        call("DELETE","/api/admin/offerings/"+section,token,null,200);
        var replacement=call("POST","/api/admin/offerings",token,Map.of("courseId",db.count("SELECT id FROM courses WHERE code='CSE101'"),"sectionNo",no,"roomId",1,"capacity",40,"offered",true,"schedules",List.of(Map.of("day","THU","start","21:00","end","22:00"))),200);
        assertThat(replacement.get("id").asLong()).isNotEqualTo(section);
    }
    @Test void deletionRequiresAdminAndRejectsMissingIds() throws Exception { long section=section("AUTH-"+sequence.incrementAndGet(),null,0);call("DELETE","/api/admin/offerings/"+section,null,null,401);call("DELETE","/api/admin/offerings/999999999",token,null,404); }
    @Test void errorsRollBackEnrollmentChanges() {
        long teacher=account("TEACHER"),student=account("STUDENT");String no="ROLLBACK-"+sequence.incrementAndGet();long section=section(no,teacher,1);
        db.exec("INSERT INTO enrollments(student_id,section_id,semester_id,status) VALUES(?,?,?,'CONFIRMED')",student,section,db.semester());
        doThrow(new IllegalStateException("Database failure")).when(repository).removeOffering(actor,section,no);
        assertThatThrownBy(()->service.delete(actor,section)).isInstanceOf(IllegalStateException.class);
        assertThat(db.count("SELECT COUNT(*) FROM enrollments WHERE section_id=? AND status='CONFIRMED'",section)).isEqualTo(1);
        assertThat(db.count("SELECT seats_taken FROM sections WHERE id=?",section)).isEqualTo(1);
        assertThat(db.count("SELECT COUNT(*) FROM deleted_offerings WHERE section_id=?",section)).isZero();
    }
}
