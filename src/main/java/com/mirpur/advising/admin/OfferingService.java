package com.mirpur.advising.admin;

import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.shared.value.TimeSlot;
import com.mirpur.advising.seat.SeatService;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class OfferingService {
    private final AdminRepository db;
    private final CachedCatalogService cache;
    private final SeatService seats;
    public OfferingService(AdminRepository db, CachedCatalogService cache, SeatService seats) {
        this.db = db;
        this.cache = cache;
        this.seats = seats;
    }
    @Transactional public Object offeringStatus(long actor, long id, boolean offered) {
        var section = db.offeringSectionsRecord(id);
        if (Boolean.TRUE.equals(section.get("is_offered")) != offered) {
            db.offeringStatusUpdate(offered, id);
            cache.clear();
            db.audit(actor, "OFFERING_STATUS", "section", id, offered ? "Enabled" : "Disabled");
        }
        return Map.of("id", id, "offered", offered, "message", offered ? "Section enabled." : "Section disabled.");
    }
    @Transactional public Object offering(long actor, Long id, Map < String, Object > b) {
        long course = number(b, "courseId"), room = number(b, "roomId");
        int capacity =(int) number(b, "capacity");
        String no = str(b, "sectionNo").trim();
        require( !no.isBlank(), "Section number is required.");
        require(capacity > 0 && capacity <= number(db.offeringRoomsRecord(room), "capacity"), "Capacity must fit the selected room.");
        db.offeringCoursesRecord(course);
        Long teacher = null;
        if (id != null) {
            var old = db.offeringSectionsRecord(id);
            require(number(old, "seats_taken") <= capacity, "Capacity cannot be below occupied seats.");
            require(number(old, "course_id") == course || number(old, "seats_taken") == 0, "Cannot change an occupied section's course.");
            teacher = old.get("teacher_id") == null ? null : number(old, "teacher_id");
        }
        List < Map < String, Object >> slots =(List < Map < String, Object >>) b.get("schedules");
        require(slots != null && !slots.isEmpty(), "Add at least one schedule.");
        db.offeringRoomsRecord2(room);
        if (teacher != null) db.offeringTeachersRecord(teacher);
        checkSchedule(id, room, teacher, slots);
        if (id == null) id = db.offeringSectionsInsert(course, db.semester(), no, room, capacity, !Boolean.FALSE.equals(b.get("offered")));
        else db.offeringSectionsUpdate(course, no, room, capacity, !Boolean.FALSE.equals(b.get("offered")), id);
        db.offeringSectionSchedulesUpdate(id);
        for (var slot : slots) db.offeringSectionSchedulesUpdate2(id, str(slot, "day"), LocalTime.parse(str(slot, "start")), LocalTime.parse(str(slot, "end")));
        cache.clear();
        seats.invalidate(id);
        db.audit(actor, "SAVE_SECTION", "section", id, no);
        return Map.of("id", id, "message", "Section and routine saved.");
    }
    public void checkSchedule(Long section, long room, Long teacher, List < Map < String, Object >> slots) {
        for (int i = 0; i < slots.size(); i ++) {
            var s = slots.get(i);
            String day = str(s, "day");
            LocalTime start = LocalTime.parse(str(s, "start")), end = LocalTime.parse(str(s, "end"));
            var timeSlot = new TimeSlot(day, start, end);
            require(db.checkScheduleSectionSchedulesCount(db.semester(), section == null ? - 1 : section, room, teacher, day, end, start) == 0, "Room or teacher schedule clash.");
            for (int j = 0; j < i; j ++) {
                var p = slots.get(j);
                require( !timeSlot.overlaps(new TimeSlot(str(p, "day"), LocalTime.parse(str(p, "start")), LocalTime.parse(str(p, "end")))), "Schedule slots overlap.");
            }
        }
    }
}
