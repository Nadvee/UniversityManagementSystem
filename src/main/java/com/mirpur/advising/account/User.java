package com.mirpur.advising.account;

import com.mirpur.advising.shared.value.LoginId;

public abstract class User {
    private final LoginId loginId;
    private final String name;
    private boolean active;
    protected User(LoginId loginId, String name, boolean active) {
        this.loginId = loginId;
        this.name = name;
        this.active = active;
    }
    public LoginId loginId() {
        return loginId;
    }
    public String name() {
        return name;
    }
    public boolean isActive() {
        return active;
    }
    public void deactivate() {
        active = false;
    }
    public abstract String role();
    public String homePath() {
        return "/" + role().toLowerCase(java.util.Locale.ROOT) + "/dashboard.html";
    }
}
