package com.leasingcompanion.renewal.expiration;

import com.leasingcompanion.pms.Lease;
import com.leasingcompanion.pms.PmsClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Builds renewal offers whose end dates respect the move-out cap.
 *
 * <p>Leases ending within the renewal window are candidates: their next end date is still open. Every
 * other lease's current end date is treated as fixed load.
 */
public final class RenewalPlanner {

    private final PmsClient pms;

    public RenewalPlanner(PmsClient pms) {
        this.pms = Objects.requireNonNull(pms);
    }

    public RenewalPlan plan(LocalDate today, int renewalWindowDays, int renewalTermMonths, LevelingPolicy policy) {
        LocalDate windowEnd = today.plusDays(renewalWindowDays);
        Map<String, Lease> candidates = new LinkedHashMap<>();
        List<LocalDate> fixed = new ArrayList<>();
        for (Lease lease : pms.fetchActiveLeases()) {
            boolean inWindow = !lease.endDate().isBefore(today) && !lease.endDate().isAfter(windowEnd);
            if (inWindow) {
                candidates.put(lease.id(), lease);
            } else {
                fixed.add(lease.endDate());
            }
        }

        List<ScheduleRequest> requests = candidates.values().stream()
                .map(l -> new ScheduleRequest(l.id(), l.endDate().plusMonths(renewalTermMonths)))
                .toList();
        LevelingResult result = new ExpirationLeveler(policy).level(fixed, requests);

        SortedMap<LocalDate, Integer> before = new TreeMap<>();
        fixed.forEach(d -> before.merge(d, 1, Integer::sum));
        requests.forEach(r -> before.merge(r.preferredEndDate(), 1, Integer::sum));

        List<RenewalSuggestion> suggestions = result.assignments().stream()
                .map(a -> new RenewalSuggestion(candidates.get(a.request().leaseId()), a))
                .toList();
        return new RenewalPlan(suggestions, before, result.loadByDay(), policy.maxMoveOutsPerDay());
    }
}
