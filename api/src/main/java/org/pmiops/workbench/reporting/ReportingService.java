package org.pmiops.workbench.reporting;

import com.google.common.annotations.VisibleForTesting;
import org.pmiops.workbench.model.ReportingSnapshot;

public interface ReportingService {
  @VisibleForTesting
  ReportingSnapshot getSnapshot();

  void uploadSnapshot();
}
