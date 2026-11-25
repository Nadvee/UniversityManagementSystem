package com.mirpur.advising.seat;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class SeatRepository extends BaseRepository {
    public SeatRepository(JdbcDatabase database) {
        super(database);
    }
    public Map < String, Object > reserveSectionsRecord(Object ... args) {
        return database.one("SELECT capacity,seats_taken FROM sections WHERE id=?", args);
    }
    public int afterCompletionSectionsUpdate(Object ... args) {
        return database.exec("UPDATE sections SET seats_taken=seats_taken+1 WHERE id=? AND seats_taken<capacity AND is_offered=TRUE AND NOT EXISTS (SELECT 1 FROM deleted_offerings x WHERE x.section_id=sections.id)", args);
    }
    public int releaseSectionsUpdate(Object ... args) {
        return database.exec("UPDATE sections SET seats_taken=seats_taken-1 WHERE id=? AND seats_taken>0", args);
    }
    public Map < String, Object > mergeSectionsRecord(Object ... args) {
        return database.one("SELECT capacity,seats_taken FROM sections WHERE id=?", args);
    }
}
