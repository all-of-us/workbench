package org.pmiops.workbench.reporting;

public interface ReportingVerificationService {
  /** Verifies batched uploaded result for each table. Returns {@code true} if all are verified. */
  boolean verifyBatchesAndLog(long captureSnapshotTime);
}
