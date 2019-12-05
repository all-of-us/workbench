package org.pmiops.workbench.monitoring;

import org.pmiops.workbench.monitoring.signals.Signal;

public interface MonitoringService {
  public void registerSignal(Signal signal);

  public void sendSignal(Signal signal, Object value);
}
