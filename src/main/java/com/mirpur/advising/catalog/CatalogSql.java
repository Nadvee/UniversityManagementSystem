package com.mirpur.advising.catalog;



public final class CatalogSql {
    public static final String SELECT = """
 SELECT s.*,c.code,c.name,c.credit,d.code AS dept_code,d.name AS department,d.alias,
 u.full_name AS faculty,r.name AS room,
 (SELECT GROUP_CONCAT(CONCAT(x.day,' ',x.start_time,'-',x.end_time) SEPARATOR '; ') FROM section_schedules x WHERE x.section_id=s.id) AS schedule,
 (SELECT GROUP_CONCAT(CONCAT(p.code,' ',p.name) SEPARATOR ', ') FROM course_prerequisites cp JOIN courses p ON p.id=cp.prereq_course_id WHERE cp.course_id=c.id) AS prerequisites
 FROM (SELECT s.* FROM sections s WHERE NOT EXISTS (SELECT 1 FROM deleted_offerings x WHERE x.section_id=s.id)) s JOIN courses c ON c.id=s.course_id JOIN departments d ON d.id=c.dept_id
 LEFT JOIN users u ON u.id=s.teacher_id LEFT JOIN rooms r ON r.id=s.room_id
 """;
    private CatalogSql() {
    }
}
