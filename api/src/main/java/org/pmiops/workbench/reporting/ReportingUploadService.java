package org.pmiops.workbench.reporting;

import java.util.List;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;

/*
 * Service to upload a pre-compiled ReportingSnapshot to the appropriate Reporting dataset
 * and tables for this project. Each implementation of this service corresponds to a separate
 * BigQuery insertion path.
 */
public interface ReportingUploadService {
  void uploadSnapshot(ReportingSnapshot reportingSnapshot);

  void uploadBatch(List<ReportingWorkspace> batch, long captureTimestamp);
}
