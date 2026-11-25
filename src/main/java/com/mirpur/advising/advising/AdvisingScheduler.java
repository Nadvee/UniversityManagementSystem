package com.mirpur.advising.advising;

import com.mirpur.advising.catalog.CachedCatalogService;
import com.mirpur.advising.shared.db.JdbcDatabase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component public class AdvisingScheduler {
    private final WindowSchedulerRepository db;
    private final AdvisingService advising;
    private final CachedCatalogService cache;
    public AdvisingScheduler(WindowSchedulerRepository db, AdvisingService a, CachedCatalogService c) {
        this.db = db;
        advising = a;
        cache = c;
    }
    @Scheduled(fixedDelay = 15000, initialDelay = 15000) public void tick() {
        db.tickAdvisingWindowsUpdate();
        var due = db.tickAdvisingWindowsList();
        for (var w : due) {
            if (db.tickAdvisingWindowsUpdate2(w.get("id")) == 1) cache.window(JdbcDatabase.number(w, "id"));
        }
        for (var h : db.tickAdvisingHoldsList()) advising.release(JdbcDatabase.number(h, "student_id"), JdbcDatabase.number(h, "id"), "EXPIRED");
    }
}
