package com.leasingcompanion.pms;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A current lease as reported by the property management system. End date is the last day of occupancy. */
public record Lease(String id, Unit unit, String residentName, LocalDate startDate, LocalDate endDate,
                    BigDecimal monthlyRent) {
}
