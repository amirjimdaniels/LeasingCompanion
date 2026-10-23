package com.leasingcompanion.renewal.expiration;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

/**
 * Rules for spreading lease end dates.
 *
 * @param maxMoveOutsPerDay most lease ends staff can handle on one day
 * @param maxDaysEarlier    how far before the default end date a lease may be pulled
 * @param maxDaysLater      how far after the default end date a lease may be pushed
 * @param keepWithinMonth   never move an end date into a different month
 * @param closedDays        days the office doesn't do move-outs
 */
public record LevelingPolicy(int maxMoveOutsPerDay, int maxDaysEarlier, int maxDaysLater,
                             boolean keepWithinMonth, Set<DayOfWeek> closedDays) {

    public LevelingPolicy {
        if (maxMoveOutsPerDay < 1) {
            throw new IllegalArgumentException("maxMoveOutsPerDay must be at least 1");
        }
        if (maxDaysEarlier < 0 || maxDaysLater < 0) {
            throw new IllegalArgumentException("shift limits cannot be negative");
        }
        closedDays = closedDays.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(closedDays));
    }

    public static LevelingPolicy defaults() {
        return new LevelingPolicy(3, 14, 14, true, Set.of(DayOfWeek.SUNDAY));
    }
}
