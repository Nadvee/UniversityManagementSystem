package com.mirpur.advising.admin;

import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.teacher.TeacherCourseChoice;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class AssignmentService {
    private final AdminRepository db;
    private final CachedCatalogService cache;
    private final OfferingService schedules;
    public AssignmentService(AdminRepository db, CachedCatalogService cache, OfferingService schedules) {
        this.db = db;
        this.cache = cache;
        this.schedules = schedules;
    }
    @Transactional public Object assign(long actor, long choice, long section) {
        var c = db.assignTeacherCourseChoicesRecord(choice);
        new TeacherCourseChoice(TeacherCourseChoice.Status.valueOf(str(c, "status")), str(c, "source")).assign();
        var s = db.assignSectionsRecord(section);
        require(number(s, "course_id") == number(c, "course_id") && number(s, "semester_id") == number(c, "semester_id"), "Choose a section of this course and semester.");
        db.assignTeachersRecord(c.get("teacher_id"));
        var slots = db.assignSectionSchedulesList(section);
        schedules.checkSchedule(section, number(s, "room_id"), number(c, "teacher_id"), slots);
        db.assignSectionsUpdate(c.get("teacher_id"), section);
        db.assignTeacherCourseChoicesUpdate(actor, section, c.get("teacher_id"));
        db.assignTeacherCourseChoicesUpdate2(section, actor, choice);
        cache.clear();
        db.audit(actor, "ASSIGN_TEACHER", "section", section, "Choice " + choice);
        return Map.of("message", "Teacher assigned.");
    }
}
