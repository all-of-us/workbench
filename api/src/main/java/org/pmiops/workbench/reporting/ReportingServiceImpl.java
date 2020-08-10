package org.pmiops.workbench.reporting;

import org.pmiops.workbench.model.ReportingSnapshot;
import org.springframework.stereotype.Service;

@Service
public class ReportingServiceImpl implements ReportingService {

  private final ReportingUploadService reportingUploadService;
  private final ReportingSnapshotService reportingSnapshotService;

  public ReportingServiceImpl(
      ReportingUploadService reportingUploadService,
      ReportingSnapshotService reportingSnapshotService) {
    this.reportingUploadService = reportingUploadService;
    this.reportingSnapshotService = reportingSnapshotService;
  }

  @Override
  public ReportingJobResult takeAndUploadSnapshot() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    return reportingUploadService.uploadSnapshot(snapshot);
  }
}
