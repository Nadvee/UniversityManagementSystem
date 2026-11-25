package com.mirpur.advising.account;

import com.mirpur.advising.shared.value.LoginId;
import com.mirpur.advising.admin.AdminRepository;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component public final class TeacherAccountCreator extends AccountCreator {
    public TeacherAccountCreator(AdminRepository accounts, PasswordEncoder encoder) {
        super(accounts, encoder);
    }
    public String role() {
        return "TEACHER";
    }
    protected LoginId loginId(String raw) {
        return LoginId.teacher(raw);
    }
    protected String departmentField() {
        return "departmentCode";
    }
    protected User user(LoginId login, String name) {
        return new Teacher(login, name, true);
    }
    protected void saveProfile(long id, User user, long department, Map < String, Object > command) {
        accounts.accountTeachersUpdate(id, user.loginId().value(), department);
    }
}
