package org.pmiops.workbench.reporting;

import org.pmiops.workbench.model.ReportingSnapshot;

/**
 * @deprecated in favor of ReportingQueryService, because this implementation consumes too much
 *     memory. Please write a streaming query in ReportingQueryService for new payloads.
 *     <p>This service encapsulate the business of obtaining the current analytics snapshot from the
 *     application MySQL DB and third party sources (e.g. Terra).
 */
@Deprecated
public interface ReportingSnapshotService {
  ReportingSnapshot takeSnapshot();
}
