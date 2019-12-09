package org.pmiops.workbench.monitoring;

import java.util.Map;
import org.pmiops.workbench.monitoring.views.StatsViewProperties;

public interface MonitoringService {

  /**
   * Record an occurrence of a counted event.
   *
   * @param viewProperties
   */
  default void recordIncrement(StatsViewProperties viewProperties) {
    recordValue(viewProperties, 1);
  }

  void recordValue(StatsViewProperties viewProperties, Number value);

  void recordValue(Map<StatsViewProperties, Number> enumToValue);
}
