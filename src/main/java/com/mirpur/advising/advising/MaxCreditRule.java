package com.mirpur.advising.advising;

import com.mirpur.advising.shared.exception.ValidationException;

public final class MaxCreditRule implements AdvisingRule {
    public void validate(AdvisingContext context) {
        if (context.selectedCredit() +(context.confirming() ? 0 : context.courseCredit()) > context.maximumCredit()) throw new ValidationException("The credit limit would be exceeded.");
    }
}
