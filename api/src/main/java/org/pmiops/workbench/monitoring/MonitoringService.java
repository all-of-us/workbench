package org.pmiops.workbench.monitoring;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;

public interface MonitoringService {
  public void registerSignal(String signalName, String signalDescription, Measure measurementInformation, Aggregation aggregation);

  public void sendLongSignal(Measure.MeasureLong measurementInformation, Long longValue);

  public void sendDoubleSignal(Measure.MeasureDouble measurementInformation, Double doubleValue);
}
