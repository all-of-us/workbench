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
    recordValue(viewProperties, 1);
  }

  void recordValue(OpenCensusStatsViewInfo viewProperties, Number value);

  void recordValue(Map<OpenCensusStatsViewInfo, Number> enumToValue);
}
