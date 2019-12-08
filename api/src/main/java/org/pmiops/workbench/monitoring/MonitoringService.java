package org.pmiops.workbench.monitoring;

import org.pmiops.workbench.monitoring.signals.StatsViewProperties;

public interface MonitoringService {
  void record(StatsViewProperties viewProperties, Number value);
}
