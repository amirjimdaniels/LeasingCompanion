package com.leasingcompanion.renewal.expiration;

import com.leasingcompanion.pms.Lease;
import com.leasingcompanion.pms.MockPmsClient;
import com.leasingcompanion.pms.Unit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenewalPlannerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    @Test
    void candidateAvoidsDayAlreadyTakenByFixedLease() {
        Lease candidate = lease("L1", LocalDate.of(2026, 11, 30));
        Lease fixed = lease("L2", LocalDate.of(2027, 11, 30));
        var planner = new RenewalPlanner(() -> List.of(candidate, fixed));

        RenewalPlan plan = planner.plan(TODAY, 60, 12, new LevelingPolicy(1, 3, 0, true, Set.of()));

        assertEquals(1, plan.suggestions().size());
        RenewalSuggestion s = plan.suggestions().get(0);
        assertEquals(candidate, s.lease());
        assertEquals(LocalDate.of(2027, 11, 30), s.defaultEndDate());
        assertEquals(LocalDate.of(2027, 11, 29), s.suggestedEndDate());
        assertEquals(Period.of(0, 11, 29), s.suggestedTerm());
        assertEquals(2, plan.loadBefore().get(LocalDate.of(2027, 11, 30)));
        assertEquals(1, plan.loadAfter().get(LocalDate.of(2027, 11, 30)));
    }

    @Test
    void levelingReducesOverloadedDaysOnMockProperty() {
        var planner = new RenewalPlanner(new MockPmsClient(TODAY));

        RenewalPlan plan = planner.plan(TODAY, 120, 12, LevelingPolicy.defaults());

        assertTrue(plan.suggestions().size() > 20, "mock property should have a realistic batch of renewals");
        assertTrue(plan.overloadedDaysBefore() > 0, "month-end clustering should overload some days");
        assertTrue(plan.overloadedDaysAfter() < plan.overloadedDaysBefore());
    }

    private static Lease lease(String id, LocalDate end) {
        Unit unit = new Unit("U-" + id, id, "A1 1x1", 1, BigDecimal.valueOf(1450));
        return new Lease(id, unit, "Resident " + id, end.plusDays(1).minusMonths(12), end, BigDecimal.valueOf(1400));
    }
}
