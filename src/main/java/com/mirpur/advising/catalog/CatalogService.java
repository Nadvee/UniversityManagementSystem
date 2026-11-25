package com.mirpur.advising.catalog;

import java.util.*;
import org.springframework.stereotype.Service;

@Service public class CatalogService {
    private final CatalogRepository db;
    public CatalogService(CatalogRepository db) {
        this.db = db;
    }
    public List < Map < String, Object >> window(long window) {
        return db.windowCatalogueList(window);
    }
    public List < Map < String, Object >> enrolled(long student) {
        return db.enrolledCatalogueList(student, db.semester());
    }
    public List < Map < String, Object >> routine(long student, long semester) {
        return db.routineEnrollmentsList(student, semester);
    }
}
