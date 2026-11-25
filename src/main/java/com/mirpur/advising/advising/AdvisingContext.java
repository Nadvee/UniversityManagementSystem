package com.mirpur.advising.advising;

import java.util.List;

public record AdvisingContext(List < String > warnings, boolean enrolled, boolean held, boolean confirming, double selectedCredit, double courseCredit, double maximumCredit) {
    public AdvisingContext {
        warnings = List.copyOf(warnings);
    }
}
