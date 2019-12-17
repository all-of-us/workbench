package org.pmiops.workbench.monitoring;

import java.util.Map;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;

public interface GaugeDataCollector {
  Map<OpenCensusStatsViewInfo, Number> getGaugeData();
}
