package org.pmiops.workbench.reporting;

import org.pmiops.workbench.model.ReportingSnapshot;

/*
 * Service to upload a pre-compiled ReportingSnapshot to the appropriate Reporting dataset
 * and tables for this project. Each implementation of this service corresponds to a separate
 * BigQuery insertion path.
 */
public interface ReportingUploadService {
  ReportingJobResult uploadSnapshot(ReportingSnapshot reportingSnapshot);
}
