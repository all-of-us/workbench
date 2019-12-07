package org.pmiops.workbench.monitoring;

import org.pmiops.workbench.monitoring.signals.Signal;

public interface MonitoringService {
  void registerSignal(Signal signal);

  void send(Signal signal, Object value);
}
