package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

  /**
   * Record each map that has a non-empty attacment map separately, and just group the others.
   * @return
   */
  public void record() {
    ImmutableMap.Builder<OpenCensusStatsViewInfo, Number> noAttachmentsSignalMap = ImmutableMap.builder();
    for (GaugeDataCollector collector : gaugeDataCollectors) {
      Map<String, AttachmentValue> attachmentKeyToValue = collector.getMeasureMapAttachments();
      if (attachmentKeyToValue.isEmpty()) {
        // put all these values in the map with no attachments
        noAttachmentsSignalMap.putAll(collector.getGaugeData());
      } else {
        // record now
        monitoringService.recordValues(collector.getGaugeData(), collector.getMeasureMapAttachments());
      }
      monitoringService.recordValues(noAttachmentsSignalMap.build());
    }

    ImmutableMap<OpenCensusStatsViewInfo, Number> result =
        gaugeDataCollectors.stream()
            .map(GaugeDataCollector::getGaugeData)
            .flatMap(m -> m.entrySet().stream())
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    monitoringService.recordValues(result);
    logValues(result); // temporary until dashboards are up and robust
  }

  private void logValues(Map<OpenCensusStatsViewInfo, Number> viewToValue) {
    logger.info(
        viewToValue.entrySet().stream()
            .map(
                entry ->
                    String.format("%s = %s", entry.getKey().getName(), entry.getValue().toString()))
            .sorted()
            .collect(Collectors.joining("\n")));
  }
}
