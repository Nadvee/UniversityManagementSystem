package com.mirpur.advising.teacher;

import com.mirpur.advising.shared.security.Access;

import com.mirpur.advising.shared.config.RuntimeSettings;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service public class TeacherService {
    private final TeacherRepository db;
    private final int max;
    private final Access access;
    public TeacherService(TeacherRepository db, RuntimeSettings settings, Access access) {
        this.access = access;
        this.max = settings.teacher().maxCourses();
        this.db = db;
    }
    @Transactional public Object choose(long teacher, List < Long > ids, boolean confirm) {
        db.chooseTeachersRecord(teacher);
        long semester = db.semester();
        if (confirm) {
            require(db.chooseTeacherCourseChoicesCount(teacher, semester) > 0, "Save a selection first.");
            db.chooseTeacherCourseChoicesUpdate(teacher, semester);
        } else {
            var unique = new HashSet < >(ids);
            require(unique.size() + db.chooseTeacherCourseChoicesCount2(teacher, semester) <= max, "The maximum is " + max + " courses per semester.");
            for (long id : unique) {
                require(db.chooseSectionsCount(id, semester) > 0, "Course is not offered this semester.");
                require(db.chooseTeacherCourseChoicesCount3(teacher, id, semester) == 0, "Confirmed choices cannot be edited.");
            }
            db.chooseTeacherCourseChoicesUpdate2(teacher, semester);
            for (long id : unique) {
                if (db.chooseTeacherCourseChoicesUpdate3(teacher, id, semester) == 0) db.chooseTeacherCourseChoicesUpdate4(teacher, id, semester);
            }
        }
        db.audit(teacher, confirm ? "CONFIRM_CHOICES" : "SAVE_CHOICES", "teacher", teacher, "Course selection updated.");
        return Map.of("message", confirm ? "Selection confirmed and sent to the administrator." : "Draft selection saved.");
    }
    public record Choices(List < Long > courseIds) {
    }
    public Object courses() {
        return db.coursesCoursesList(access.id(), db.semester(), db.semester());
    }
    public Object save(Choices b) {
        access.requireRole("TEACHER");
        return choose(access.id(), b.courseIds(), false);
    }
    public Object confirm() {
        access.requireRole("TEACHER");
        return choose(access.id(), List.of(), true);
    }
    @Transactional public Object selectCourse(long course) {
        access.requireRole("TEACHER");
        long teacher = access.id(), semester = db.semester();
        db.chooseTeachersRecord(teacher);
        var existing = db.courseChoices(teacher, course, semester);
        if (!existing.isEmpty() && !"REJECTED".equals(existing.get(0).get("status"))) return Map.of("message", "Course is already selected.");
        require(db.chooseSectionsCount(course, semester) > 0, "Course is not offered this semester.");
        require(db.activeChoices(teacher, semester) < max, "The maximum is " + max + " courses per semester.");
        if (db.chooseTeacherCourseChoicesUpdate3(teacher, course, semester) == 0) db.chooseTeacherCourseChoicesUpdate4(teacher, course, semester);
        db.audit(teacher, "SELECT_COURSE", "course", course, "Course selected and saved.");
        return Map.of("message", "Course selected.");
    }
    @Transactional public Object confirmCourse(long course) {
        access.requireRole("TEACHER");
        long teacher = access.id(), semester = db.semester();
        db.chooseTeachersRecord(teacher);
        var choices = db.courseChoices(teacher, course, semester);
        require(!choices.isEmpty(), "Select this course before confirming it.");
        var choice = choices.get(0);
        if (List.of("CONFIRMED", "ASSIGNED").contains(str(choice, "status"))) return Map.of("message", "Course is already confirmed.");
        require("SELECTED".equals(choice.get("status")), "Select this course before confirming it.");
        require(db.chooseSectionsCount(course, semester) > 0, "Course is not offered this semester.");
        new TeacherCourseChoice(TeacherCourseChoice.Status.valueOf(str(choice, "status")), str(choice, "source")).confirm();
        db.confirmCourseChoice(teacher, course, semester);
        db.audit(teacher, "CONFIRM_CHOICE", "choice", number(choice, "id"), "Course confirmed and sent to the administrator.");
        return Map.of("message", "Course confirmed and sent to the administrator.");
    }
    @Transactional public Object removeCourse(long course) {
        access.requireRole("TEACHER");
        long teacher = access.id(), semester = db.semester();
        db.chooseTeachersRecord(teacher);
        db.requireCourse(course);
        var choices = db.courseChoices(teacher, course, semester);
        if (choices.isEmpty()) return Map.of("message", "Course is already unselected.");
        var choice = choices.get(0);
        require(!"ASSIGNED".equals(choice.get("status")) && db.assignedCourseSections(teacher, course, semester) == 0, "Ask the administrator to unassign this course before removing it.");
        require(db.removeCourseChoice(teacher, course, semester) == 1, "The course changed. Reload the page before removing it.");
        db.audit(teacher, "REMOVE_CHOICE", "choice", number(choice, "id"), "Course removed from selection.");
        return Map.of("message", "Course removed from selection.");
    }
    public Object sections() {
        return db.sectionsCatalogueList(access.id(), db.semester()).stream().map(s -> {
            s.put("in_advising", db.sectionsAdvisingWindowSectionsCount(s.get("id")) > 0);
            return s;
        }).toList();
    }
    public Object students(long id) {
        db.studentsSectionsRecord(id, access.id());
        return db.studentsEnrollmentsList(id);
    }
}
