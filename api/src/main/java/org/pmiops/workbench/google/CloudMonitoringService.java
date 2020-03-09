package org.pmiops.workbench.google;

import com.google.monitoring.v3.TimeSeries;
import java.time.Duration;

public interface CloudMonitoringService {

  Iterable<TimeSeries> getCloudStorageReceivedBytes(
      String workspaceNamespace, Duration trailingTimeToQuery);
}
