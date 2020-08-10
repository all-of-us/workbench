package org.pmiops.workbench.reporting;

import org.pmiops.workbench.model.ReportingSnapshot;

// Declaring a service for hte BigQuery upload process will allow easaier testing side-by-side
// of different upload strategies (e.g. batch vs chunked vs streaming).
public interface ReportingUploadService {
  ReportingJobResult uploadSnapshot(ReportingSnapshot reportingSnapshot);
}
