package org.pmiops.workbench.reporting;

import java.util.Map;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUploadDetails;
import org.pmiops.workbench.reporting.ReportingServiceImpl.BatchSupportedTableEnum;

public interface ReportingVerificationService {
  ReportingUploadDetails verifyAndLog(ReportingSnapshot reportingSnapshot);

  /** Verifies batched uploaded result for each table. Returns {@code true} if all are verified. */
  boolean verifyBatchesAndLog(
      Map<BatchSupportedTableEnum, Integer> tableNameAndCount, long captureSnapshotTime);
}
