package com.mirpur.advising.account;

import com.mirpur.advising.shared.value.LoginId;

public final class Admin extends User {
    public Admin(LoginId loginId, String name, boolean active) {
        super(loginId, name, active);
    }
    @Override public String role() {
        return "ADMIN";
    }
}
