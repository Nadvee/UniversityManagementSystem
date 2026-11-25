package com.mirpur.advising.advising;

import java.util.List;

public final class AdvisingValidator {
    private final List < AdvisingRule > rules;
    public AdvisingValidator(List < AdvisingRule > rules) {
        this.rules = List.copyOf(rules);
    }
    public void validate(AdvisingContext context) {
        rules.forEach(rule -> rule.validate(context));
    }
}
