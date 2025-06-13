package org.pmiops.workbench.reporting;

import java.util.List;

public interface ReportingVerificationService {
  /** Verifies batched uploaded result for each table. Returns {@code true} if all are verified. */
  boolean verifyBatchesAndLog(long captureSnapshotTime);

  void verifyBatchesAndLog(List<String> tables, long captureSnapshotTime);

  boolean verifySnapshot(long captureSnapshotTime);
}
