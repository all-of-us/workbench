package org.pmiops.workbench.reporting;

import org.pmiops.workbench.model.ReportingSnapshot;

public interface ReportingSnapshotService {
  ReportingSnapshot takeSnapshot();
}
