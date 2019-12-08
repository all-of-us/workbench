package org.pmiops.workbench.monitoring;

import org.pmiops.workbench.monitoring.signals.StatsViewProperties;

public interface MonitoringService {
  void send(StatsViewProperties signal, Object value);
}
