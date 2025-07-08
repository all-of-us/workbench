package org.pmiops.workbench.reporting;

import java.util.List;

public interface ReportingVerificationService {
  /** Verifies batched uploaded result for each table. Returns {@code true} if all are verified. */
  boolean verifyBatchesAndLog(long captureSnapshotTime);

  /** Verifies batched uploaded result specified tables. */
  void verifyBatchesAndLog(List<String> tables, long captureSnapshotTime);

  /** Verifies all tables associated with captureSnapshotTime uploaded successfully. Returns {@code true} if all are verified. */
  boolean verifySnapshot(long captureSnapshotTime);
}
