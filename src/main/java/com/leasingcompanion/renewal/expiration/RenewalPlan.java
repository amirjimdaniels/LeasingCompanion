package com.leasingcompanion.renewal.expiration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @param loadBefore lease ends per day if every candidate renewed on the default term
 * @param loadAfter  lease ends per day with the suggested end dates
 */
public record RenewalPlan(List<RenewalSuggestion> suggestions,
                          SortedMap<LocalDate, Integer> loadBefore,
                          SortedMap<LocalDate, Integer> loadAfter,
                          int maxMoveOutsPerDay) {

    public RenewalPlan {
        suggestions = List.copyOf(suggestions);
        loadBefore = Collections.unmodifiableSortedMap(new TreeMap<>(loadBefore));
        loadAfter = Collections.unmodifiableSortedMap(new TreeMap<>(loadAfter));
    }

    /** Months that renewal candidates would end in, before or after leveling. */
    public SortedSet<YearMonth> affectedMonths() {
        SortedSet<YearMonth> months = new TreeSet<>();
        for (RenewalSuggestion s : suggestions) {
            months.add(YearMonth.from(s.defaultEndDate()));
            months.add(YearMonth.from(s.suggestedEndDate()));
        }
        return months;
    }

    public long movedCount() {
        return suggestions.stream().filter(s -> s.assignment().shiftDays() != 0).count();
    }

    public long needsReviewCount() {
        return suggestions.stream().filter(s -> !s.assignment().isScheduled()).count();
    }

    public long overloadedDaysBefore() {
        return overloadedDays(loadBefore);
    }

    public long overloadedDaysAfter() {
        return overloadedDays(loadAfter);
    }

    private long overloadedDays(Map<LocalDate, Integer> load) {
        SortedSet<YearMonth> months = affectedMonths();
        return load.entrySet().stream()
                .filter(e -> months.contains(YearMonth.from(e.getKey())))
                .filter(e -> e.getValue() > maxMoveOutsPerDay)
                .count();
    }
}
