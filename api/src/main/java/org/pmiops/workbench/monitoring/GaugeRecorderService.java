package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
    logValues(bundlesToLogBuilder.build());
  }

  private void logValues(Collection<MeasurementBundle> bundles) {
    logger.info(
        bundles.stream()
            .map(this::measurementBundleToLogString)
            .sorted()
            .collect(Collectors.joining("\n")));
  }

  private String measurementBundleToLogString(MeasurementBundle bundle) {
    final StringBuilder bldr = new StringBuilder("MeasurementBundle\n").append("\tMeasurements:\n");
    bundle
        .getMeasurements()
        .forEach(
            (key, value) -> bldr.append(key.getName()).append(" = ").append(value).append("\n"));
    if (!bundle.getAttachments().isEmpty()) {
      bldr.append("\tAttachments:\n");
    }
    return bldr.toString();
  }
}
