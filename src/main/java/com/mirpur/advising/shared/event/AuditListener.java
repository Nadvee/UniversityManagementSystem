package com.mirpur.advising.shared.event;

import com.mirpur.advising.shared.db.JdbcDatabase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component public final class AuditListener {
    private final JdbcDatabase db;
    public AuditListener(JdbcDatabase db) {
        this.db = db;
    }
    @EventListener public void record(AuditEvent event) {
        db.exec("INSERT INTO audit_logs(actor_id,action,entity,entity_id,detail) VALUES(?,?,?,?,?)", event.actor(), event.action(), event.entity(), event.id(), event.detail());
    }
}
