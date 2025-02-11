package org.pmiops.workbench.reporting;

import java.util.List;
import org.pmiops.workbench.model.ReportingBase;

public interface ReportingUploadService {
  <T extends ReportingBase> void uploadBatch(
      ReportingTableParams<T> uploadBatchParams, List<T> batch, long captureTimestamp);

  /** Uploads a record into VerifiedSnapshot table if upload result is verified. */
  void uploadVerifiedSnapshot(long captureTimestamp);
}
