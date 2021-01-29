package org.pmiops.workbench.reporting;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.ReportingServiceImpl.BatchSupportedTableEnum;

/*
 * Service to upload a pre-compiled ReportingSnapshot to the appropriate Reporting dataset
 * and tables for this project. Each implementation of this service corresponds to a separate
 * BigQuery insertion path.
 */
public interface ReportingUploadService {
  void uploadSnapshot(ReportingSnapshot reportingSnapshot);

  void uploadBatchWorkspace(
      List<ReportingWorkspace> batch,
      long captureTimestamp,
      Map<BatchSupportedTableEnum, Integer> batchUploadedCount);
}
