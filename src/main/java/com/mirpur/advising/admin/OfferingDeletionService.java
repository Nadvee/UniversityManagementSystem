package com.mirpur.advising.admin;

import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.seat.SeatService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class OfferingDeletionService {
    private final OfferingDeletionRepository db;
    private final CachedCatalogService cache;
    private final SeatService seats;
    public OfferingDeletionService(OfferingDeletionRepository db, CachedCatalogService cache, SeatService seats) {
        this.db = db;
        this.cache = cache;
        this.seats = seats;
    }
    @Transactional public Object delete(long actor, long section) {
        db.lockStudents(section);
        var offering = db.lockSection(section);
        if (db.deleted(section)) return Map.of("message", "Course offering already deleted.");
        db.cancelEnrollment(section);
        db.removeOffering(actor, section, str(offering, "section_no"));
        seats.invalidate(section);
        cache.clear();
        db.audit(actor, "DELETE_OFFERING", "section", section, str(offering, "code") + " section " + str(offering, "section_no") + " deleted. Enrollments cancelled and held seats released.");
        return Map.of("message", "Course offering deleted.");
    }
}
