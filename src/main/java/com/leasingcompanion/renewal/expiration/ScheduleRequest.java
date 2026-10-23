package com.leasingcompanion.renewal.expiration;

import java.time.LocalDate;
import java.util.Objects;

/** A lease whose end date can still be chosen, and the end date it would get by default. */
public record ScheduleRequest(String leaseId, LocalDate preferredEndDate) {

    public ScheduleRequest {
        Objects.requireNonNull(leaseId);
        Objects.requireNonNull(preferredEndDate);
    }
}
