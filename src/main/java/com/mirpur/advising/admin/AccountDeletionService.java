package com.mirpur.advising.admin;

import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.seat.SeatService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class AccountDeletionService {
    private final AccountDeletionRepository db;
    private final SeatService seats;
    private final CachedCatalogService cache;
    public AccountDeletionService(AccountDeletionRepository db, SeatService seats, CachedCatalogService cache) {
        this.db = db;
        this.seats = seats;
        this.cache = cache;
    }
    @Transactional public void delete(long actor, long id, String role) {
        require(List.of("STUDENT", "TEACHER").contains(role), "Choose a student or teacher account.");
        require(actor != id, "You cannot delete your own account.");
        var account = db.lockAccount(id, role);
        if ("STUDENT".equals(role)) {
            db.lockStudent(id);
            for (var section : db.studentSections(id)) {
                long sectionId = number(section, "id"), reserved = db.reservedSeats(id, sectionId);
                if (reserved > 0) {
                    require(db.releaseSeats(sectionId, reserved) == 1, "Seat counts are inconsistent. Please check the section before deleting this student.");
                    seats.invalidate(sectionId);
                }
            }
            db.deleteStudentRecords(id);
        } else {
            db.deleteTeacherRecords(id);
        }
        db.deleteAccount(id);
        db.audit(actor, "DELETE_ACCOUNT", "user", id, role + " " + str(account, "login_id") + " (" + str(account, "full_name") + ") permanently deleted.");
        cache.clear();
    }
}
