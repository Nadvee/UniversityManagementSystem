package com.mirpur.advising.drop;

import com.mirpur.advising.advising.AdvisingService;
import com.mirpur.advising.catalog.CatalogService;
import com.mirpur.advising.advising.Enrollment;
import com.mirpur.advising.advising.AdvisingRepository;
import com.mirpur.advising.seat.SeatService;
import java.time.*;
import java.util.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class DropService {
    private final AdvisingRepository db;
    private final AdvisingService advising;
    private final CatalogService catalog;
    private final SeatService seats;
    private final Clock clock;
    public DropService(AdvisingRepository db, AdvisingService advising, CatalogService catalog, SeatService seats, Clock clock) {
        this.db = db;
        this.advising = advising;
        this.catalog = catalog;
        this.seats = seats;
        this.clock = clock;
    }
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "advisingCourses", allEntries = true),
        @CacheEvict(cacheNames = "routine", key = "#student + ':' + @db.semester()")
    }) public Object confirm(long student, List < Long > sectionIds, String reason) {
        require(reason != null && !reason.isBlank() && reason.length() <= 1000, "A reason is required (maximum 1,000 characters).");
        require(sectionIds != null && !sectionIds.isEmpty(), "Select at least one course.");
        db.dropStudentsRecord(student);
        var rule = db.dropAdvisingRulesRecord(db.semester());
        require( !LocalDate.now(clock).isAfter(LocalDate.parse(str(rule, "drop_deadline"))), "The drop deadline has passed.");
        var confirmed = catalog.enrolled(student);
        var ids = new HashSet < >(sectionIds);
        require(confirmed.stream().filter(s -> ids.contains(number(s, "id"))).count() == ids.size(), "Only your confirmed courses can be dropped.");
        double remain = confirmed.stream().filter(s -> !ids.contains(number(s, "id"))).mapToDouble(s -> decimal(s, "credit")).sum();
        require(remain >= decimal(advising.band(student), "min_credit"), "At least " + advising.band(student).get("min_credit") + " credits must remain.");
        for (long id : ids.stream().sorted().toList()) {
            var e = db.dropEnrollmentsRecord(student, id);
            var enrollment = new Enrollment("CONFIRMED");
            enrollment.drop(reason);
            db.dropEnrollmentsUpdate(reason, e.get("id"));
            seats.release(id);
            db.audit(student, "SEMESTER_DROP", "enrollment", number(e, "id"), reason);
        }
        return Map.of("message", "Advising and Routine caches refreshed.");
    }
}
