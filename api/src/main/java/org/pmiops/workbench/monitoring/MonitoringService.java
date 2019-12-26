package org.pmiops.workbench.monitoring;

import java.util.Map;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;

public interface MonitoringService {

  /**
   * Record an occurrence of a counted event.
   *
   * @param viewInfo
   */
  default void recordIncrement(OpenCensusStatsViewInfo viewInfo) {
    recordValue(viewInfo, 1);
  }

  void recordValue(OpenCensusStatsViewInfo viewInfo, Number value);

  void recordValues(Map<OpenCensusStatsViewInfo, Number> enumToValue);
}
