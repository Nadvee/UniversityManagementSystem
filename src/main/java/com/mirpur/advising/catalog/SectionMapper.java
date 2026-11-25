package com.mirpur.advising.catalog;

import java.util.Map;

public final class SectionMapper {
    public Section domain(Map < String, Object > row) {
        return new Section(((Number) row.get("id")).longValue(),((Number) row.get("capacity")).intValue(),((Number) row.get("seats_taken")).intValue(), row.get("teacher_id") == null ? null :((Number) row.get("teacher_id")).longValue());
    }
}
