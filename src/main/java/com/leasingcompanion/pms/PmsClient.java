package com.leasingcompanion.pms;

import java.util.List;

/**
 * Read-only view of the property management system. A real integration (Yardi, RealPage, Entrata, ...)
 * implements this; the app only depends on this interface.
 */
public interface PmsClient {

    List<Lease> fetchActiveLeases();
}
