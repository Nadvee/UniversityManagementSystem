package com.mirpur.advising.advising;

import com.mirpur.advising.catalog.CatalogService;
import com.mirpur.advising.shared.config.RuntimeSettings;
import com.mirpur.advising.seat.SeatService;
import java.time.*;
import java.util.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class AdvisingService {
    private final java.time.Clock clock;
    private final AdvisingRepository db;
    private final SeatService seats;
    private final CatalogService catalog;
    private final int holdSeconds;
    public AdvisingService(AdvisingRepository db, SeatService seats, CatalogService catalog, RuntimeSettings settings, java.time.Clock clock) {
        this.clock = clock;
        this.holdSeconds = settings.app().holdSeconds();
        this.db = db;
        this.seats = seats;
        this.catalog = catalog;
    }
    public Map < String, Object > band(long student) {
        var s = db.bandStudentsRecord(student);
        var bands = db.bandCreditLimitBandsList(db.semester());
        return bands.stream().filter(b -> s.get("cgpa") == null ? b.get("min_cgpa") == null : b.get("min_cgpa") != null && decimal(s, "cgpa") >= decimal(b, "min_cgpa") && decimal(s, "cgpa") <= decimal(b, "max_cgpa")).findFirst().orElseThrow();
    }
    public List < Map < String, Object >> windows() {
        return db.windowsAdvisingWindowsList(db.semester());
    }
    public Map < String, Object > open() {
        var list = db.openAdvisingWindowsList(db.semester());
        return list.isEmpty() ? null : list.get(0);
    }
    public List < String > warnings(long student, long section) {
        var s = db.warningsSectionsRecord(section);
        var warnings = new ArrayList < String >();
        for (var p : db.warningsCoursePrerequisitesList(s.get("course_id"))) {
            var latest = db.warningsPrerequisiteRecords(student, p.get("id"));
            if (latest.isEmpty() || !Boolean.TRUE.equals(latest.get(0).get("passed"))) warnings.add("Prerequisite not met: " + p.get("code") + " " + p.get("name"));
        }
        if (db.warningsSectionSchedulesCount(section, section, student, db.semester(), student) > 0) warnings.add("This section conflicts with your current timetable.");
        return warnings;
    }
    public double credits(long student) {
        return db.creditsSectionsValue(db.semester(), student, student);
    }
    private Map < String, Object > validate(long student, long section, boolean confirming) {
        var window = db.validateAdvisingWindowsRecord(section, db.semester());
        var s = db.validateSectionsRecord(section);
        var context = new AdvisingContext(warnings(student, section), db.validateEnrollmentsCount(student, s.get("course_id"), db.semester()) > 0, db.validateAdvisingHoldsCount(student, s.get("course_id")) > 0, confirming, credits(student), decimal(s, "credit"), decimal(band(student), "max_credit"));
        new AdvisingValidator(List.of(new PrerequisiteAndClashRule(), new NotAlreadySelectedRule(), new NotAlreadyHeldRule(), new MaxCreditRule())).validate(context);
        return window;
    }
    @Transactional public Object select(long student, long section) {
        db.selectStudentsRecord(student);
        var existing = db.selectAdvisingHoldsList(student, section);
        if ( !existing.isEmpty()) return existing.get(0);
        seats.lock(student, section);
        var w = validate(student, section, false);
        require(seats.reserve(section, true), "Seat full.");
        long id = db.selectAdvisingHoldsInsert(student, section, w.get("id"), "ADVISING", AdvisingHold.start(clock, holdSeconds).expiresAt());
        return db.selectAdvisingHoldsRecord(id);
    }
    @Transactional
    @CacheEvict(cacheNames = "routine", key = "#student + ':' + @db.semester()") public Object confirm(long student, long hold) {
        db.confirmStudentsRecord(student);
        var h = db.confirmAdvisingHoldsRecord(hold, student);
        if ("CONFIRMED".equals(h.get("status"))) return Map.of("message", "Course already confirmed.");
        new AdvisingHold(AdvisingHold.Status.valueOf(str(h, "status")), LocalDateTime.parse(str(h, "expires_at"))).confirm(clock);
        long section = number(h, "section_id");
        validate(student, section, true);
        long semester = db.semester();
        var old = db.confirmEnrollmentsList(student, section);
        long enrollment;
        if (old.isEmpty()) enrollment = db.confirmEnrollmentsInsert(student, section, semester);
        else {
            enrollment = number(old.get(0), "id");
            db.confirmEnrollmentsUpdate(enrollment);
        }
        db.confirmAdvisingHoldsUpdate(hold);
        db.audit(student, "ADVISING_CONFIRM", "enrollment", enrollment, "Course confirmed and routine updated.");
        return Map.of("message", "Course confirmed. Your routine has been updated.");
    }
    @Transactional public void release(long student, long hold, String status) {
        db.releaseStudentsRecord(student);
        var h = db.releaseAdvisingHoldsRecord(hold, student);
        if ( !"HELD".equals(h.get("status"))) return;
        db.releaseAdvisingHoldsUpdate(status, hold);
        seats.release(number(h, "section_id"));
    }
    public Object state(long student) {
        var state = new LinkedHashMap < String, Object >();
        state.put("window", open());
        state.put("windows", windows());
        state.put("serverTime", LocalDateTime.now(clock).toString());
        state.put("band", band(student));
        state.put("holds", db.stateCatalogueList(student).stream().map(s -> {
            var r = new LinkedHashMap < >(s);
            var h = db.stateAdvisingHoldsRecord(student, s.get("id"));
            r.put("hold_id", h.get("id"));
            r.put("expires_at", h.get("expires_at"));
            return r;
        }).toList());
        state.put("confirmed", catalog.enrolled(student));
        state.put("credit", credits(student));
        return state;
    }
}
