package org.pmiops.workbench.reporting;

import java.util.List;

public interface ReportingUploadService {
  <T> void uploadBatch(
      ReportingTableParams<T> uploadBatchParams, List<T> batch, long captureTimestamp);

  /** Uploads a record into VerifiedSnapshot table if upload result is verified. */
  void uploadVerifiedSnapshot(long captureTimestamp);
}
