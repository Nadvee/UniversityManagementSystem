package com.mirpur.advising.advising;

import com.mirpur.advising.shared.exception.ValidationException;

public final class PrerequisiteAndClashRule implements AdvisingRule {
    public void validate(AdvisingContext context) {
        if ( !context.warnings().isEmpty()) throw new ValidationException(String.join(" ", context.warnings()));
    }
}
