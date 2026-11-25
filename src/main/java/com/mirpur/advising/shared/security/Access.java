package com.mirpur.advising.shared.security;

import com.mirpur.advising.account.AccessRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component public class Access {
    private final AccessRepository db;
    public Access(AccessRepository db) {
        this.db = db;
    }
    public long id() {
        return((Number) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).longValue();
    }
    public void requireRole(String role) {
        var user = db.userRoleRecord(id());
        if (!role.equals(user.get("role"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
    }
}
