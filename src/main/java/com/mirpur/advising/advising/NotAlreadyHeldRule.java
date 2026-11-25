package com.mirpur.advising.advising;

import com.mirpur.advising.shared.exception.ValidationException;

public final class NotAlreadyHeldRule implements AdvisingRule {
    public void validate(AdvisingContext context) {
        if ( !context.confirming() && context.held()) throw new ValidationException("This course is already on hold.");
    }
}
