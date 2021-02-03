package org.pmiops.workbench.reporting;

import java.util.Set;
import org.pmiops.workbench.model.ReportingSnapshot;

public interface ReportingVerificationService {
  boolean verifyAndLog(ReportingSnapshot reportingSnapshot);

  /** Verifies batched uploaded result for each table. Returns {@code true} if all are verified. */
  boolean verifyBatchesAndLog(Set<String> batchTables, long captureSnapshotTime);
}
