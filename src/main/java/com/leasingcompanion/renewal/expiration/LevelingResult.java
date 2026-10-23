package com.leasingcompanion.renewal.expiration;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @param assignments one per request, in scheduling order
 * @param loadByDay   lease ends per day after leveling, including fixed expirations and unscheduled
 *                    requests (counted on their default date)
 */
public record LevelingResult(List<Assignment> assignments, SortedMap<LocalDate, Integer> loadByDay) {

    public LevelingResult {
        assignments = List.copyOf(assignments);
        loadByDay = Collections.unmodifiableSortedMap(new TreeMap<>(loadByDay));
    }

    public List<Assignment> unscheduled() {
        return assignments.stream().filter(a -> !a.isScheduled()).toList();
    }

    public int loadOn(LocalDate day) {
        return loadByDay.getOrDefault(day, 0);
    }
}
