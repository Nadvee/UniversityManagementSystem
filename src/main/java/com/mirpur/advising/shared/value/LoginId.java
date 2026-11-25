package com.mirpur.advising.shared.value;

import com.mirpur.advising.shared.exception.ValidationException;

public record LoginId(String value) {
    public LoginId {
        if (value == null || value.isBlank()) throw new ValidationException("Enter an ID.");
    }
    public static LoginId student(String raw) {
        if ( !raw.matches("S[0-9]+")) throw new ValidationException("Use S followed by digits.");
        return new LoginId(raw);
    }
    public static LoginId teacher(String raw) {
        if ( !raw.matches("T[0-9]+")) throw new ValidationException("Use T followed by digits.");
        return new LoginId(raw);
    }
}
