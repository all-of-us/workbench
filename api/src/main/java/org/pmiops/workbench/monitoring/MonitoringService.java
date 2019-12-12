package org.pmiops.workbench.monitoring;

import java.util.Map;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;

public interface MonitoringService {

  /**
   * Record an occurrence of a counted event.
   *
   * @param viewProperties
   */
  default void recordIncrement(OpenCensusStatsViewInfo viewProperties) {
    recordValues(viewProperties, 1);
  }

  void recordValues(OpenCensusStatsViewInfo viewProperties, Number value);

  void recordValues(Map<OpenCensusStatsViewInfo, Number> enumToValue);
}
