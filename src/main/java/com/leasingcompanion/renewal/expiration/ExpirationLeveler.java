package com.leasingcompanion.renewal.expiration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Spreads lease end dates so no day exceeds the policy's move-out cap.
 *
 * <p>Fixed expirations (leases whose end date is already locked in) consume capacity first. Each request
 * then takes the open day closest to its default end date; on a tie the later day wins, since that keeps
 * the term (and rent) slightly longer. Requests are handled in date order so results are repeatable.
 * A request with no open day stays on its default date and is reported as unscheduled.
 */
public final class ExpirationLeveler {

    private final LevelingPolicy policy;

    public ExpirationLeveler(LevelingPolicy policy) {
        this.policy = Objects.requireNonNull(policy);
    }

    public LevelingResult level(Collection<LocalDate> fixedExpirations, List<ScheduleRequest> requests) {
        Map<LocalDate, Integer> load = new TreeMap<>();
        for (LocalDate day : fixedExpirations) {
            load.merge(day, 1, Integer::sum);
        }

        List<ScheduleRequest> ordered = new ArrayList<>(requests);
        ordered.sort(Comparator.comparing(ScheduleRequest::preferredEndDate).thenComparing(ScheduleRequest::leaseId));

        List<Assignment> assignments = new ArrayList<>(ordered.size());
        for (ScheduleRequest request : ordered) {
            LocalDate chosen = null;
            for (LocalDate candidate : candidateDates(request.preferredEndDate())) {
                if (load.getOrDefault(candidate, 0) < policy.maxMoveOutsPerDay()) {
                    chosen = candidate;
                    break;
                }
            }
            Assignment assignment = new Assignment(request, chosen);
            load.merge(assignment.effectiveEndDate(), 1, Integer::sum);
            assignments.add(assignment);
        }
        return new LevelingResult(assignments, new TreeMap<>(load));
    }

    /** Allowed end dates for a request, nearest to the default first (later before earlier on ties). */
    List<LocalDate> candidateDates(LocalDate preferred) {
        List<LocalDate> dates = new ArrayList<>();
        int widest = Math.max(policy.maxDaysEarlier(), policy.maxDaysLater());
        for (int offset = 0; offset <= widest; offset++) {
            if (offset <= policy.maxDaysLater()) {
                addIfAllowed(dates, preferred, preferred.plusDays(offset));
            }
            if (offset > 0 && offset <= policy.maxDaysEarlier()) {
                addIfAllowed(dates, preferred, preferred.minusDays(offset));
            }
        }
        return dates;
    }

    private void addIfAllowed(List<LocalDate> dates, LocalDate preferred, LocalDate candidate) {
        if (policy.closedDays().contains(candidate.getDayOfWeek())) {
            return;
        }
        if (policy.keepWithinMonth() && !YearMonth.from(candidate).equals(YearMonth.from(preferred))) {
            return;
        }
        dates.add(candidate);
    }
}
