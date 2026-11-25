package com.mirpur.advising;

import com.mirpur.advising.shared.value.LoginId;
import com.mirpur.advising.shared.value.TimeSlot;
import com.mirpur.advising.shared.exception.SeatFullException;
import com.mirpur.advising.shared.exception.ValidationException;
import com.mirpur.advising.catalog.Section;
import com.mirpur.advising.teacher.TeacherCourseChoice;
import com.mirpur.advising.account.Student;
import com.mirpur.advising.account.Teacher;
import com.mirpur.advising.account.User;
import com.mirpur.advising.advising.AdvisingHold;
import com.mirpur.advising.advising.Enrollment;
import com.mirpur.advising.advising.AdvisingWindow;
import com.mirpur.advising.advising.AdvisingContext;
import com.mirpur.advising.advising.AdvisingValidator;
import com.mirpur.advising.advising.MaxCreditRule;
import com.mirpur.advising.advising.NotAlreadySelectedRule;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Domain tests run without Spring, SQL, Redis or HTTP. */
class DomainModelTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-06T04:00:00Z"), ZoneId.of("Asia/Dhaka"));

    @Test void loginIdsAndUserInheritanceKeepRolesDistinct() {
        User student = new Student(LoginId.student("S001"), "Student", true);
        User teacher = new Teacher(LoginId.teacher("T001"), "Teacher", true);
        assertThat(student.homePath()).isEqualTo("/student/dashboard.html");
        assertThat(teacher.homePath()).isEqualTo("/teacher/dashboard.html");
        student.deactivate();assertThat(student.isActive()).isFalse();
        assertThatThrownBy(() -> LoginId.student("T001")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> LoginId.teacher("T-001")).isInstanceOf(ValidationException.class);
    }
    @Test void schedulesOverlapOnlyOnTheSameDayAndExcludeTouchingEdges() {
        var first = new TimeSlot("SAT", LocalTime.of(9,0), LocalTime.of(10,0));
        assertThat(first.overlaps(new TimeSlot("SAT", LocalTime.of(9,30), LocalTime.of(11,0)))).isTrue();
        assertThat(first.overlaps(new TimeSlot("SAT", LocalTime.of(10,0), LocalTime.of(11,0)))).isFalse();
        assertThat(first.overlaps(new TimeSlot("SUN", LocalTime.of(9,0), LocalTime.of(10,0)))).isFalse();
    }
    @Test void sectionCannotOversellOrReleaseBelowZero() {
        var section = new Section(1,1,0,2L);
        section.reserveSeat();assertThat(section.availableSeats()).isZero();
        assertThatThrownBy(section::reserveSeat).isInstanceOf(SeatFullException.class);
        section.releaseSeat();section.releaseSeat();assertThat(section.availableSeats()).isEqualTo(1);
    }
    @Test void holdsExpireAtTheExactBoundaryAndTransitionsAreGuarded() {
        var hold = AdvisingHold.start(clock,600);
        assertThat(hold.isExpired(Clock.offset(clock,Duration.ofSeconds(599)))).isFalse();
        assertThat(hold.isExpired(Clock.offset(clock,Duration.ofSeconds(600)))).isTrue();
        hold.confirm(clock);
        assertThatThrownBy(hold::expire).isInstanceOf(ValidationException.class);
        var expired = AdvisingHold.start(clock,600);
        assertThatThrownBy(() -> expired.confirm(Clock.offset(clock,Duration.ofMinutes(10)))).hasMessage("This hold has expired.");
    }
    @Test void windowsRequireTeachersAndRespectTheExclusiveEndTime() {
        var now = LocalDateTime.now(clock);
        var window = new AdvisingWindow(AdvisingWindow.Status.OPEN,now,now.plusHours(2));
        assertThatThrownBy(() -> window.addSection(new Section(1,40,0,null))).isInstanceOf(ValidationException.class);
        window.addSection(new Section(2,40,0,3L));
        assertThat(window.isOpen(clock)).isTrue();
        assertThat(window.isOpen(Clock.offset(clock,Duration.ofHours(2)))).isFalse();
        window.cancel();assertThatThrownBy(window::open).isInstanceOf(ValidationException.class);
    }
    @Test void choiceCanOnlyBeAssignedAfterConfirmation() {
        var choice = new TeacherCourseChoice(TeacherCourseChoice.Status.SELECTED,"TEACHER");
        assertThatThrownBy(choice::assign).isInstanceOf(ValidationException.class);
        choice.confirm();choice.assign();assertThat(choice.status()).isEqualTo(TeacherCourseChoice.Status.ASSIGNED);
        assertThatThrownBy(choice::reject).isInstanceOf(ValidationException.class);
    }
    @Test void droppingEnrollmentRequiresAReasonAndConfirmedState() {
        var enrollment = new Enrollment("CONFIRMED");
        assertThatThrownBy(() -> enrollment.drop(" ")).isInstanceOf(ValidationException.class);
        enrollment.drop("Schedule change");assertThat(enrollment.status()).isEqualTo("DROPPED");
        assertThatThrownBy(() -> enrollment.drop("Again")).isInstanceOf(ValidationException.class);
    }
    @Test void validationChainRejectsDuplicatesAndExcessCredits() {
        var validator = new AdvisingValidator(List.of(new NotAlreadySelectedRule(),new MaxCreditRule()));
        assertThatThrownBy(() -> validator.validate(new AdvisingContext(List.of(),true,false,false,3,3,15))).hasMessage("This course is already confirmed.");
        assertThatThrownBy(() -> validator.validate(new AdvisingContext(List.of(),false,false,false,15,3,15))).hasMessage("The credit limit would be exceeded.");
    }
}
