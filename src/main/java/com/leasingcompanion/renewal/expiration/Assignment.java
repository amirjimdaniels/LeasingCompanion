package com.leasingcompanion.renewal.expiration;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Outcome for one request. {@code assignedEndDate} is null when no day within the policy had room;
 * such leases need a manager's decision.
 */
public record Assignment(ScheduleRequest request, LocalDate assignedEndDate) {

    public boolean isScheduled() {
        return assignedEndDate != null;
    }

    /** Where the lease will actually end if nobody intervenes. */
    public LocalDate effectiveEndDate() {
        return isScheduled() ? assignedEndDate : request.preferredEndDate();
    }

    /** Days moved from the default end date; negative means earlier. */
    public long shiftDays() {
        return ChronoUnit.DAYS.between(request.preferredEndDate(), effectiveEndDate());
    }
}
