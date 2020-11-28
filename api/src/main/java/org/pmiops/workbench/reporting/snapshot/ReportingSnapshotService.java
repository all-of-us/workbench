package org.pmiops.workbench.reporting.snapshot;

import org.pmiops.workbench.model.ReportingSnapshot;

/*
 * This service encapsulate the business of obtaining the current analytics snapshot
 * from the application MySQL DB and third party sources (e.g. Terra).
 */
public interface ReportingSnapshotService {
  ReportingSnapshot takeSnapshot();
}
