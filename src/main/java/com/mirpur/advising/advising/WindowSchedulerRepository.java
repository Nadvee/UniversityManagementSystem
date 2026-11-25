package com.mirpur.advising.advising;

import com.mirpur.advising.shared.db.BaseRepository;
import com.mirpur.advising.shared.db.JdbcDatabase;

import java.util.*;
import org.springframework.stereotype.Repository;

@Repository public class WindowSchedulerRepository extends BaseRepository {
    public WindowSchedulerRepository(JdbcDatabase database) {
        super(database);
    }
    public int tickAdvisingWindowsUpdate() {
        return database.exec("UPDATE advising_windows SET status='CLOSED' WHERE end_at<=CURRENT_TIMESTAMP AND status IN ('SCHEDULED','OPEN')");
    }
    public List < Map < String, Object >> tickAdvisingWindowsList() {
        return database.list("SELECT id FROM advising_windows WHERE start_at<=CURRENT_TIMESTAMP AND end_at>CURRENT_TIMESTAMP AND status='SCHEDULED'");
    }
    public int tickAdvisingWindowsUpdate2(Object id) {
        return database.exec("UPDATE advising_windows SET status='OPEN' WHERE id=? AND status='SCHEDULED'", id);
    }
    public List < Map < String, Object >> tickAdvisingHoldsList() {
        return database.list("SELECT h.id,h.student_id FROM advising_holds h LEFT JOIN advising_windows w ON w.id=h.window_id WHERE h.status='HELD' AND (h.expires_at<=CURRENT_TIMESTAMP OR w.status IN ('CLOSED','CANCELLED') OR w.end_at<=CURRENT_TIMESTAMP) ORDER BY h.student_id,h.id");
    }
}
