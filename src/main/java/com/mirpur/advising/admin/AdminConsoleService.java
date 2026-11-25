package com.mirpur.advising.admin;
import com.mirpur.advising.advising.AdvisingService;
import com.mirpur.advising.shared.security.Access;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;
@Service public class AdminConsoleService {
    private final AdminRepository db;
    private final Access access;
    private final AdminService admin;
    private final AdvisingService advising;
    private final AccountDeletionService accounts;
    private final OfferingDeletionService offerings;
    public AdminConsoleService(AdminRepository db, Access access, AdminService admin, AdvisingService advising, AccountDeletionService accounts, OfferingDeletionService offerings) {
        this.db=db; this.access=access; this.admin=admin; this.advising=advising; this.accounts=accounts; this.offerings=offerings;
    }
    public Object accounts(String role,String q,int page) {
        page=Math.max(1,page); String query="%"+q.trim()+"%";
        return Map.of("items",db.accountsUsersList(role,query,query,(page-1)*20),"total",db.accountsUsersCount(role,query,query),"page",page);
    }
    public Object student(Map<String,Object> body) { return admin.account(access.id(),"STUDENT",body); }
    public Object teacher(Map<String,Object> body) { return admin.account(access.id(),"TEACHER",body); }
    public Object studentDetail(long id) { return Map.of("profile",db.studentDetailStudentsRecord(id),"state",advising.state(id)); }
    public Object teacherDetail(long id) { return Map.of("profile",db.teacherDetailTeachersRecord(id),"sections",db.teacherDetailCatalogueList(id,db.semester()),"choices",db.choicesTeacherCourseChoicesList(false,db.semester(),id)); }
    public Object active(long id,Map<String,Object> body) {
        require(id!=access.id(),"You cannot deactivate your own account.");
        db.activeUsersUpdate(Boolean.TRUE.equals(body.get("active")),id);
        db.audit(access.id(),"ACCOUNT_STATUS","user",id,str(body,"active")); return Map.of("message","Account updated.");
    }
    public Object deleteAccount(long id,String role) { accounts.delete(access.id(),id,role); return Map.of("message",("STUDENT".equals(role)?"Student":"Teacher")+" account deleted."); }
    public Object choices() { return db.choicesTeacherCourseChoicesList(true,db.semester()); }
    public Object assign(long id,Map<String,Object> body) { return admin.assign(access.id(),id,number(body,"sectionId")); }
    @Transactional public Object reject(long id) {
        require(db.rejectTeacherCourseChoicesUpdate(access.id(),id)==1,"Only confirmed requests can be rejected.");
        db.audit(access.id(),"REJECT_CHOICE","choice",id,"Rejected by administrator."); return Map.of("message","Request rejected.");
    }
    public Object offerings() { return db.offeringsCatalogueList(db.semester()); }
    public Object schedules(long id) { return db.schedulesSectionSchedulesList(id); }
    public Object offering(Map<String,Object> body) { return admin.offering(access.id(),null,body); }
    public Object edit(long id,Map<String,Object> body) { return admin.offering(access.id(),id,body); }
    public Object offeringStatus(long id,Map<String,Object> body) { require(body.get("offered") instanceof Boolean,"Offering status must be true or false."); return admin.offeringStatus(access.id(),id,Boolean.TRUE.equals(body.get("offered"))); }
    public Object deleteOffering(long id) { return offerings.delete(access.id(),id); }
    public Object roster(long id) { return db.rosterEnrollmentsList(id); }
    public Object windows() { return advising.windows(); }
    public Object window(Map<String,Object> body) { return admin.window(access.id(),body); }
    public Object deleteWindow(long id) {
        admin.deleteWindow(access.id(),id);
        for(var hold:db.deleteWindowAdvisingHoldsList(id)) advising.release(number(hold,"student_id"),number(hold,"id"),"EXPIRED");
        return Map.of("message","Advising window deleted. Outstanding holds released; confirmed courses retained.");
    }
    public Object windowAction(long id,String action) {
        require(List.of("open","close").contains(action),"Unknown action.");
        var result=admin.setWindow(access.id(),id,action.equals("open"));
        if(action.equals("close")) for(var hold:db.windowActionAdvisingHoldsList(id)) advising.release(number(hold,"student_id"),number(hold,"id"),"EXPIRED");
        return result;
    }
}
