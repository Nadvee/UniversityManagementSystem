package com.mirpur.advising.catalog;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class CatalogRepository extends BaseRepository {
    public CatalogRepository(JdbcDatabase database) {
        super(database);
    }
    public List < Map < String, Object >> windowCatalogueList(Object ... args) {
        return database.list(CatalogSql.SELECT + " JOIN advising_window_sections ws ON ws.section_id=s.id WHERE ws.window_id=? AND s.is_offered=TRUE ORDER BY c.code,s.section_no", args);
    }
    public List < Map < String, Object >> enrolledCatalogueList(Object ... args) {
        return database.list(CatalogSql.SELECT + " JOIN enrollments e ON e.section_id=s.id WHERE e.student_id=? AND e.status='CONFIRMED' AND e.semester_id=? ORDER BY c.code", args);
    }
    public List < Map < String, Object >> routineEnrollmentsList(Object ... args) {
        return database.list("SELECT c.code,c.name,c.credit,u.full_name AS faculty,r.name AS room,x.day,x.start_time,x.end_time FROM enrollments e JOIN sections s ON s.id=e.section_id JOIN courses c ON c.id=s.course_id JOIN section_schedules x ON x.section_id=s.id LEFT JOIN users u ON u.id=s.teacher_id LEFT JOIN rooms r ON r.id=s.room_id WHERE e.student_id=? AND e.semester_id=? AND e.status='CONFIRMED' ORDER BY x.start_time", args);
    }
}
