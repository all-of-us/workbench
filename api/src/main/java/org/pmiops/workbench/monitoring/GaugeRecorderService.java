package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;
import org.springframework.stereotype.Service;

@Service
public class GaugeRecorderService {
  private static final Logger logger = Logger.getLogger(GaugeRecorderService.class.getName());

  private final List<GaugeDataCollector> gaugeDataCollectors;
  private final MonitoringService monitoringService;

  public GaugeRecorderService(
      List<GaugeDataCollector> gaugeDataCollectors, MonitoringService monitoringService) {
    this.gaugeDataCollectors = gaugeDataCollectors;
    this.monitoringService = monitoringService;
  }

  public Map<OpenCensusStatsViewInfo, Number> record() {
    ImmutableMap<OpenCensusStatsViewInfo, Number> result =
        gaugeDataCollectors.stream()
            .map(GaugeDataCollector::getGaugeData)
            .flatMap(m -> m.entrySet().stream())
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    monitoringService.recordValues(result);
    logValues(result); // temporary until dashboards are up and robust
    return result;
  }

  private void logValues(Map<OpenCensusStatsViewInfo, Number> viewToValue) {
    StringBuilder textBuilder = new StringBuilder();
    viewToValue.forEach(
        (view, value) ->
            textBuilder.append(view.getName()).append(" = ").append(value.toString()).append("\n"));
    logger.info(textBuilder.toString());
  }
}
