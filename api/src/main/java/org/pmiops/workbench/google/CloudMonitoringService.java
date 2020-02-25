package org.pmiops.workbench.google;

import com.google.monitoring.v3.TimeSeries;

public interface CloudMonitoringService {

  Iterable<TimeSeries> getCloudStorageReceivedBytes(String workspaceNamespace);
}
