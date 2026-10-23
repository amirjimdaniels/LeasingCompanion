package com.leasingcompanion.renewal.expiration;

import com.leasingcompanion.pms.Lease;

import java.time.LocalDate;
import java.time.Period;

/** A renewal candidate and the end date the leveler suggests offering them. */
public record RenewalSuggestion(Lease lease, Assignment assignment) {

    public LocalDate renewalStartDate() {
        return lease.endDate().plusDays(1);
    }

    public LocalDate defaultEndDate() {
        return assignment.request().preferredEndDate();
    }

    public LocalDate suggestedEndDate() {
        return assignment.effectiveEndDate();
    }

    /** Length of the renewal term that ends on the suggested date. */
    public Period suggestedTerm() {
        return Period.between(renewalStartDate(), suggestedEndDate().plusDays(1));
    }
}
