package org.pmiops.workbench.reporting;

import org.pmiops.workbench.model.ReportingSnapshot;

/**
 * @deprecated This is memory-wasting implementation, for new payloads, write a query to return
 *     Steam of batches.
 *     <p>This service encapsulate the business of obtaining the current analytics snapshot from the
 *     application MySQL DB and third party sources (e.g. Terra).
 */
@Deprecated
public interface ReportingSnapshotService {
  ReportingSnapshot takeSnapshot();
}
