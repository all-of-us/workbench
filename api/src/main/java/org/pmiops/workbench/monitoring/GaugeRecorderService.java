package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.monitoring.views.OpenCensusView;
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

  public void record() {
    ImmutableList.Builder<MeasurementBundle> bundlesToLogBuilder = ImmutableList.builder();
    for (GaugeDataCollector collector : gaugeDataCollectors) {
      Collection<MeasurementBundle> bundles = collector.getGaugeData();
      bundles.forEach(monitoringService::recordBundle);
      bundlesToLogBuilder.addAll(bundles);
    }
    logValues();
  }
  private void logValues(Collection<MeasurementBundle> bundles) {
    logger.info(bundles.stream()
        .map(MeasurementBundle::toString)
        .sorted()
        .collect(Collectors.joining("\n")));
  }
}
