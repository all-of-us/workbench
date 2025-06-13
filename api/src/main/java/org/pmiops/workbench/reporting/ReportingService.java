package org.pmiops.workbench.reporting;

import java.util.List;

/*
 * Captures a snapshot of analyst-facing data and uploads it to a dataset in BigQuery for this
 * GCP project. The tables are associated with a timestamp to distinguish periodic (most likely
 * daily) uploads, and aggregate and time-series metrics are computed in BigQuery.
 */
public interface ReportingService {
  void collectRecordsAndUpload();

  void splitUploadIntoTasksAndQueue();

  void collectRecordsAndUpload(List<String> tables, long captureTimestamp);
}
