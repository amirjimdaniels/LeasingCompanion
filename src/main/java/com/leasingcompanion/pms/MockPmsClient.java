package com.leasingcompanion.pms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic fake property for development. Lease ends are spread over the next 15 months and
 * deliberately cluster on month-end, which is the problem expiration leveling exists to fix.
 */
public final class MockPmsClient implements PmsClient {

    private record FloorPlan(String name, int bedrooms, int marketRent) {
    }

    private static final FloorPlan[] FLOOR_PLANS = {
            new FloorPlan("S1 Studio", 0, 1250),
            new FloorPlan("A1 1x1", 1, 1450),
            new FloorPlan("A2 1x1 Den", 1, 1575),
            new FloorPlan("B1 2x2", 2, 1850),
            new FloorPlan("C1 3x2", 3, 2300),
    };
    private static final int[] TERM_MONTHS = {12, 12, 12, 12, 12, 6, 9, 13, 15};
    private static final String[] FIRST_NAMES = {
            "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Jamie", "Avery", "Quinn", "Drew",
            "Sam", "Reese", "Skyler", "Rowan", "Emerson", "Hayden"};
    private static final String[] LAST_NAMES = {
            "Nguyen", "Garcia", "Smith", "Patel", "Kim", "Johnson", "Okafor", "Silva", "Cohen", "Brown",
            "Martinez", "Lee", "Walker", "Haddad", "Novak", "Rossi"};

    private final List<Lease> leases;

    public MockPmsClient(LocalDate today) {
        this(today, 180, 42L);
    }

    public MockPmsClient(LocalDate today, int unitCount, long seed) {
        this.leases = List.copyOf(generate(today, unitCount, new Random(seed)));
    }

    @Override
    public List<Lease> fetchActiveLeases() {
        return leases;
    }

    private static List<Lease> generate(LocalDate today, int unitCount, Random rnd) {
        List<Lease> result = new ArrayList<>(unitCount);
        for (int i = 0; i < unitCount; i++) {
            FloorPlan plan = FLOOR_PLANS[rnd.nextInt(FLOOR_PLANS.length)];
            int building = i / 60 + 1;
            int floor = (i % 60) / 15 + 1;
            String number = "%d%d%02d".formatted(building, floor, i % 15 + 1);
            Unit unit = new Unit("U" + number, number, plan.name(), plan.bedrooms(), BigDecimal.valueOf(plan.marketRent()));

            YearMonth endMonth = YearMonth.from(today).plusMonths(rnd.nextInt(15));
            LocalDate end = rnd.nextDouble() < 0.7
                    ? endMonth.atEndOfMonth()
                    : endMonth.atDay(1 + rnd.nextInt(endMonth.lengthOfMonth()));
            if (end.isBefore(today)) {
                end = endMonth.atEndOfMonth();
            }
            int term = TERM_MONTHS[rnd.nextInt(TERM_MONTHS.length)];
            LocalDate start = end.plusDays(1).minusMonths(term);

            BigDecimal rent = BigDecimal.valueOf(plan.marketRent() * (0.94 + rnd.nextDouble() * 0.08))
                    .setScale(0, RoundingMode.HALF_UP);
            String resident = FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[rnd.nextInt(LAST_NAMES.length)];

            result.add(new Lease("L" + (1000 + i), unit, resident, start, end, rent));
        }
        return result;
    }
}
