package com.mirpur.advising.account;

import com.mirpur.advising.shared.value.LoginId;
import com.mirpur.advising.admin.AdminRepository;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

public abstract class AccountCreator {
    protected final AdminRepository accounts;
    private final PasswordEncoder encoder;
    protected AccountCreator(AdminRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }
    public abstract String role();
    protected abstract LoginId loginId(String raw);
    protected abstract String departmentField();
    protected abstract User user(LoginId login, String name);
    protected abstract void saveProfile(long id, User user, long department, Map < String, Object > command);
    public final Map < String, Object > create(long actor, Map < String, Object > command) {
        LoginId login = loginId(str(command, "loginId").trim());
        String name = str(command, "name").trim();
        require( !name.isBlank() && name.length() <= 120, "Enter a name (maximum 120 characters).");
        var departments = accounts.accountDepartmentsList(str(command, departmentField()).trim());
        require( !departments.isEmpty(), "Choose a valid department.");
        User user = user(login, name);
        // A random, undisclosed credential prevents login until the user claims this ID.
        long id = accounts.accountUsersInsert(login.value(), encoder.encode(UUID.randomUUID().toString()), user.role(), user.name());
        saveProfile(id, user, number(departments.get(0), "id"), command);
        accounts.requirePasswordSetup(id);
        accounts.audit(actor, "CREATE_" + user.role(), "user", id, name + " (" + login.value() + ")");
        return Map.of("id", id, "message", "Account created. The user can set their password from the login page.");
    }
}
