package com.mirpur.advising.account;

import com.mirpur.advising.shared.value.LoginId;
import com.mirpur.advising.admin.AdminRepository;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component public final class StudentAccountCreator extends AccountCreator {
    public StudentAccountCreator(AdminRepository accounts, PasswordEncoder encoder) {
        super(accounts, encoder);
    }
    public String role() {
        return "STUDENT";
    }
    protected LoginId loginId(String raw) {
        return LoginId.student(raw);
    }
    protected String departmentField() {
        return "subject";
    }
    protected User user(LoginId login, String name) {
        return new Student(login, name, true);
    }
    protected void saveProfile(long id, User user, long department, Map < String, Object > command) {
        accounts.accountStudentsUpdate(id, user.loginId().value(), department, command.get("subject"), accounts.semester());
    }
}
