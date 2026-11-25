package com.mirpur.advising.seed;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component public final class ReferenceDataSeeder implements Seeder {
    private final DataSeederRepository db;
    private final java.time.Clock clock;
    public ReferenceDataSeeder(DataSeederRepository db, java.time.Clock clock) {
        this.db = db;
        this.clock = clock;
    }
    public void seed() {
        if (db.seedSemestersCount() > 0) return;
        LocalDate today = LocalDate.now(clock);
        long semester = db.seedSemestersInsert("Fall " + today.getYear(), today.withDayOfYear(1), today.withMonth(12).withDayOfMonth(31));
        db.seedSemestersUpdate("Spring " + today.getYear(), today.withDayOfYear(1), today.withMonth(6).withDayOfMonth(30));
        String[][] departments = {
            {
                "CSE", "Computer Science & Engineering", "Computing"
            }, {
                "EEE", "Electrical & Electronic Engineering", "Electrical"
            }, {
                "BBA", "Business Administration", "Business"
            }, {
                "ENG", "English", "Humanities"
            }, {
                "ECO", "Economics", "Social Sciences"
            }, {
                "LAW", "Law", "Legal Studies"
            }, {
                "MAT", "Mathematics", "Math"
            }, {
                "PHY", "Physics", "Science"
            }
        };
        String[] names = {
            "Programming Fundamentals", "Discrete Mathematics", "Data Structures", "Database Systems", "Computer Networks", "Software Engineering", "Operating Systems", "Artificial Intelligence"
        };
        for (var d : departments) {
            long dept = db.seedDepartmentsInsert((Object[]) d);
            for (int i = 0; i < 8; i ++) db.seedCoursesUpdate(d[0] +(101 + i), "CSE".equals(d[0]) ? names[i] : d[1] + " " +(i + 1), dept);
        }
        var cse = db.seedCoursesList();
        db.seedCoursePrerequisitesUpdate(cse.get(1).get("id"), cse.get(0).get("id"));
        for (int i = 1; i <= 20; i ++) db.seedRoomsUpdate("Room " +(200 + i));
        int n = 0;
        String[] days = {
            "SAT", "SUN", "MON", "TUE", "WED", "THU"
        };
        for (var c : db.seedCoursesList2()) {
            for (int k = 0; k <(n < 36 ? 2 : 1); k ++) {
                long section = db.seedSectionsInsert(c.get("id"), semester, "" +(k + 1),(n % 20) + 1);
                int hour = 8 +(n / 20) * 2;
                db.seedSectionSchedulesUpdate(section, days[k], LocalTime.of(hour, 0), LocalTime.of(hour + 1, 30));
            }
            n ++;
        }
        long rule = db.seedAdvisingRulesInsert(semester, today.plusDays(60), today.plusDays(30), "At least 3 credits must remain after dropping a course.", "Recorded prerequisite completion is required before selecting a course.");
        db.seedCreditLimitBandsUpdate(rule);
        db.seedCreditLimitBandsUpdate2(rule);
        db.seedCreditLimitBandsUpdate3(rule);
        db.seedCreditLimitBandsUpdate4(rule);
        long admin = db.seedUsersCount2();
        for (int i = 1; i <= 2; i ++) {
            var start = LocalDateTime.now(clock).plusDays(i).withHour(9).withMinute(0).withSecond(0).withNano(0);
            long id = db.seedAdvisingWindowsInsert(semester, "Advising session " + i, start, start.plusHours(2), admin);
            db.seedNoticesUpdate("Upcoming advising session", "Review your courses before the advising session opens.", id);
        }
        db.seedNoticesUpdate2();
    }
}
