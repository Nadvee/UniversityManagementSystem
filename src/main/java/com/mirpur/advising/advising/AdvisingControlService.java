package com.mirpur.advising.advising;

import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.catalog.SectionMapper;
import com.mirpur.advising.admin.AdminRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class AdvisingControlService {
    private final AdminRepository db;
    private final CachedCatalogService cache;
    public AdvisingControlService(AdminRepository db, CachedCatalogService cache) {
        this.db = db;
        this.cache = cache;
    }
    @Transactional public Object window(long actor, Map < String, Object > b) {
        var ids =((List < Number >) b.getOrDefault("sectionIds", List.of())).stream().map(Number::longValue).distinct().sorted().toList();
        require( !ids.isEmpty(), "Select sections with assigned teachers.");
        String name = str(b, "name").trim();
        require( !name.isBlank(), "Window name is required.");
        LocalDateTime start = LocalDateTime.parse(str(b, "startAt"));
        int duration = b.get("duration") == null ? 120 :(int) number(b, "duration");
        require(duration > 0 && duration <= 1440, "Duration must be between 1 and 1,440 minutes.");
        long semester = db.semester();
        db.windowSemestersRecord(semester);
        require(db.windowAdvisingWindowsCount(semester, start.plusMinutes(duration), start) == 0, "Advising windows cannot overlap.");
        var windowModel = new AdvisingWindow(AdvisingWindow.Status.SCHEDULED, start, start.plusMinutes(duration));
        for (long id : ids) {
            var s = db.windowSectionsRecord(id);
            require(s.get("teacher_id") != null && number(s, "semester_id") == semester && Boolean.TRUE.equals(s.get("is_offered")), "Every section must be offered and have an assigned teacher.");
            windowModel.addSection(new SectionMapper().domain(s));
        }
        long id = db.windowAdvisingWindowsInsert(semester, name, start, start.plusMinutes(duration), duration, actor);
        for (long s : ids) db.windowAdvisingWindowSectionsUpdate(id, s);
        db.windowNoticesUpdate(name, "Advising has been scheduled. Prepare your course selection.", id);
        db.audit(actor, "CREATE_WINDOW", "window", id, name);
        return Map.of("id", id, "message", "Advising window created.");
    }
    @Transactional public Object setWindow(long actor, long id, boolean open) {
        db.setWindowSemestersRecord(db.semester());
        var w = db.setWindowAdvisingWindowsRecord(id);
        require( !"CANCELLED".equals(w.get("status")), "This advising window has been deleted.");
        if (open) {
            require(db.setWindowAdvisingWindowSectionsCount(id) > 0, "Assign teachers and add sections before opening.");
            require(db.setWindowAdvisingWindowsCount(id) == 0, "Close the current window first.");
            db.setWindowAdvisingWindowsUpdate(number(w, "duration_minutes"), id);
            cache.window(id);
        } else db.setWindowAdvisingWindowsUpdate2(id);
        db.audit(actor, open ? "OPEN_WINDOW" : "CLOSE_WINDOW", "window", id, "Window status changed.");
        return Map.of("message", open ? "Advising is open." : "Window closed. Outstanding holds are being released.");
    }
    @Transactional public void deleteWindow(long actor, long id) {
        db.deleteWindowSemestersRecord(db.semester());
        var w = db.deleteWindowAdvisingWindowsRecord(id);
        if ("CANCELLED".equals(w.get("status"))) return;
        // Retain the row because historical holds reference it; cancelled windows are hidden from schedules.
        db.deleteWindowAdvisingWindowsUpdate(id);
        db.deleteWindowNoticesUpdate(id);
        cache.clear();
        db.audit(actor, "DELETE_WINDOW", "window", id, str(w, "name"));
    }
}
