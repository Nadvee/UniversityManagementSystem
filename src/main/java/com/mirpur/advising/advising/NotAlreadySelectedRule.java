package com.mirpur.advising.advising;

import com.mirpur.advising.shared.exception.ValidationException;

public final class NotAlreadySelectedRule implements AdvisingRule {
    public void validate(AdvisingContext context) {
        if (context.enrolled()) throw new ValidationException("This course is already confirmed.");
    }
}
