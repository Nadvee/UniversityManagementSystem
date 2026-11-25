package com.mirpur.advising.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.mirpur.advising.shared.db.JdbcDatabase.*;

@Service
public class PasswordSetupService {
    private final PasswordSetupRepository accounts;
    private final PasswordEncoder encoder;
    private final InitialPasswordPolicy policy = new InitialPasswordPolicy();

    public PasswordSetupService(PasswordSetupRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @Transactional
    public Object setPassword(String loginId, String password, String confirmation) {
        policy.validate(password, confirmation);
        String id = Objects.toString(loginId, "").trim();
        var found = accounts.findForSetup(id);
        String unavailable = "This ID is not eligible for first-time password setup. Contact the administrator.";
        require(!found.isEmpty(), unavailable);
        var account = found.get(0);
        require(List.of("STUDENT", "TEACHER").contains(str(account, "role"))
                && Boolean.TRUE.equals(account.get("active"))
                && account.get("completed_at") == null, unavailable);
        long userId = number(account, "id");
        require(accounts.complete(userId, encoder.encode(password)) == 1, unavailable);
        accounts.audit(userId, "SET_INITIAL_PASSWORD", "user", userId, "First-time password setup completed.");
        return Map.of("message", "Password set successfully. You can now log in.");
    }
}
