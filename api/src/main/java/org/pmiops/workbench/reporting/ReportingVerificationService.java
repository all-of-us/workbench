package org.pmiops.workbench.reporting;

import java.util.Map;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUploadDetails;
import org.pmiops.workbench.reporting.ReportingServiceImpl.BatchSupportedTableEnum;

public interface ReportingVerificationService {

  ReportingUploadDetails getUploadDetails(ReportingSnapshot reportingSnapshot);

  ReportingUploadDetails verifyAndLog(ReportingSnapshot reportingSnapshot);

  void verifyBatchesAndLog(
      Map<BatchSupportedTableEnum, Integer> tableNameAndCount, long captureSnapshotTime);
}
