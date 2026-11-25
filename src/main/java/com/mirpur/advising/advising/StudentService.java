package com.mirpur.advising.advising;
import com.mirpur.advising.account.DashboardAssembler;
import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.catalog.CatalogService;
import com.mirpur.advising.drop.DropService;
import com.mirpur.advising.seat.SeatService;
import com.mirpur.advising.shared.security.Access;
import java.util.*;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;
@org.springframework.stereotype.Service public class StudentService {
    private final StudentRepository db;
    private final Access access;
    private final AdvisingService advising;
    private final CachedCatalogService cache;
    private final SeatService seats;
    private final CatalogService catalog;
    private final DropService dropService;
    private final Map<String,DashboardAssembler> dashboards;
    public StudentService(StudentRepository db,Access access,AdvisingService advising,CachedCatalogService cache,SeatService seats,CatalogService catalog,List<DashboardAssembler> dashboards,DropService dropService) {
        this.db=db; this.access=access; this.advising=advising; this.cache=cache; this.seats=seats; this.catalog=catalog; this.dropService=dropService;
        this.dashboards=dashboards.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(DashboardAssembler::role,x->x));
    }
    public Object dashboard() { var user=db.dashboardUsersRecord(access.id()); return dashboards.get(str(user,"role")).assemble(user); }
    public Object state() { access.requireRole("STUDENT"); return advising.state(access.id()); }
    public Object courses() { var window=advising.open(); return window==null?List.of():seats.merge(cache.window(number(window,"id"))); }
    public Object select(Map<String,Object> body) { access.requireRole("STUDENT"); return advising.select(access.id(),number(body,"sectionId")); }
    public Object confirm(Map<String,Object> body) { access.requireRole("STUDENT"); return advising.confirm(access.id(),number(body,"holdId")); }
    public void remove(long id) { access.requireRole("STUDENT"); advising.release(access.id(),id,"REMOVED"); }
    public Object routine() { return cache.routine(access.id(),db.semester()); }
    public Object eligible() { return Map.of("courses",catalog.enrolled(access.id()),"rules",db.eligibleAdvisingRulesRecord(db.semester()),"band",advising.band(access.id())); }
    public record DropRequest(List<Long> sectionIds,String reason) {}
    public Object drop(DropRequest body) { access.requireRole("STUDENT"); return dropService.confirm(access.id(),body.sectionIds(),body.reason()); }
}
