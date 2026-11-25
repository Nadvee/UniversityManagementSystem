package com.mirpur.advising.account;

import com.mirpur.advising.shared.exception.ValidationException;
import java.nio.charset.StandardCharsets;

public final class InitialPasswordPolicy {
    public void validate(String password, String confirmation) {
        if (password == null || password.isBlank() || password.length() < 4) {
            throw new ValidationException("Use at least 4 characters for your password.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("Password is too long. Use at most 72 UTF-8 bytes.");
        }
        if (!password.equals(confirmation)) {
            throw new ValidationException("Passwords do not match.");
        }
    }
}
