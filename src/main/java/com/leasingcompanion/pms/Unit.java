package com.leasingcompanion.pms;

import java.math.BigDecimal;

/** An apartment as reported by the property management system. */
public record Unit(String id, String number, String floorPlan, int bedrooms, BigDecimal marketRent) {
}
