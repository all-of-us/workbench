package org.pmiops.workbench.reporting;

import java.util.Set;

public interface ReportingVerificationService {
  /** Verifies batched uploaded result for each table. Returns {@code true} if all are verified. */
  boolean verifyBatchesAndLog(Set<String> batchTables, long captureSnapshotTime);
}
