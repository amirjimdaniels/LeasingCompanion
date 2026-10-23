package com.leasingcompanion.renewal.expiration;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpirationLevelerTest {

    private static final LocalDate NOV_17 = LocalDate.of(2027, 11, 17);
    private static final LocalDate NOV_30 = LocalDate.of(2027, 11, 30);

    @Test
    void neverExceedsDailyCap() {
        var leveler = new ExpirationLeveler(new LevelingPolicy(2, 7, 0, true, Set.of()));

        LevelingResult result = leveler.level(List.of(), requests(NOV_17, 6));

        assertTrue(result.unscheduled().isEmpty());
        assertEquals(2, result.loadOn(NOV_17));
        assertEquals(2, result.loadOn(NOV_17.minusDays(1)));
        assertEquals(2, result.loadOn(NOV_17.minusDays(2)));
    }

    @Test
    void picksClosestDayAndPrefersLaterOnTie() {
        var leveler = new ExpirationLeveler(new LevelingPolicy(1, 3, 3, true, Set.of()));

        List<LocalDate> ends = leveler.level(List.of(), requests(NOV_17, 3)).assignments().stream()
                .map(Assignment::assignedEndDate).toList();

        assertEquals(List.of(NOV_17, NOV_17.plusDays(1), NOV_17.minusDays(1)), ends);
    }

    @Test
    void skipsClosedDays() {
        LocalDate sunday = LocalDate.of(2027, 11, 10).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        var leveler = new ExpirationLeveler(new LevelingPolicy(1, 0, 2, true, Set.of(DayOfWeek.SUNDAY)));

        Assignment only = leveler.level(List.of(), requests(sunday, 1)).assignments().get(0);

        assertEquals(sunday.plusDays(1), only.assignedEndDate());
    }

    @Test
    void fixedExpirationsUseUpCapacity() {
        var leveler = new ExpirationLeveler(new LevelingPolicy(2, 1, 0, true, Set.of()));

        Assignment only = leveler.level(List.of(NOV_17, NOV_17), requests(NOV_17, 1)).assignments().get(0);

        assertEquals(NOV_17.minusDays(1), only.assignedEndDate());
        assertEquals(-1, only.shiftDays());
    }

    @Test
    void staysInMonthAndFlagsOverflowForReview() {
        var leveler = new ExpirationLeveler(new LevelingPolicy(1, 0, 5, true, Set.of()));

        LevelingResult result = leveler.level(List.of(), requests(NOV_30, 2));

        assertEquals(1, result.unscheduled().size());
        Assignment overflow = result.unscheduled().get(0);
        assertFalse(overflow.isScheduled());
        assertEquals(NOV_30, overflow.effectiveEndDate());
        assertEquals(2, result.loadOn(NOV_30), "unscheduled lease still counts on its default date");
    }

    @Test
    void mayCrossMonthWhenAllowed() {
        var leveler = new ExpirationLeveler(new LevelingPolicy(1, 0, 5, false, Set.of()));

        LevelingResult result = leveler.level(List.of(), requests(NOV_30, 2));

        assertTrue(result.unscheduled().isEmpty());
        assertEquals(1, result.loadOn(LocalDate.of(2027, 12, 1)));
    }

    @Test
    void resultDoesNotDependOnInputOrder() {
        var leveler = new ExpirationLeveler(LevelingPolicy.defaults());
        List<ScheduleRequest> input = new ArrayList<>();
        input.addAll(requests(NOV_30, 8));
        input.addAll(requests(NOV_17, 5));
        List<ScheduleRequest> shuffled = new ArrayList<>(input);
        Collections.shuffle(shuffled, new Random(7));

        assertEquals(leveler.level(List.of(), input), leveler.level(List.of(), shuffled));
    }

    private static List<ScheduleRequest> requests(LocalDate preferred, int count) {
        List<ScheduleRequest> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ScheduleRequest(preferred + "-L" + i, preferred));
        }
        return list;
    }
}
