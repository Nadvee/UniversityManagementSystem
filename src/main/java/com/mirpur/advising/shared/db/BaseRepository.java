package com.mirpur.advising.shared.db;


public abstract class BaseRepository {
    protected final JdbcDatabase database;
    protected BaseRepository(JdbcDatabase database) {
        this.database = database;
    }
    public long semester() {
        return database.semester();
    }
    public void audit(Long actor, String action, String entity, Long id, String detail) {
        database.audit(actor, action, entity, id, detail);
    }
}
