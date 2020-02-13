package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.springframework.stereotype.Service;

@Service
public class GaugeRecorderService {
  private static final Logger logger = Logger.getLogger(GaugeRecorderService.class.getName());

  private final List<GaugeDataCollector> gaugeDataCollectors;
  private final MonitoringService monitoringService;
  private LogsBasedMetricService logsBasedMetricService;

  // For local debugging, change this to Level.INFO or higher
  private final Level logLevel = Level.FINE;

  public GaugeRecorderService(
      List<GaugeDataCollector> gaugeDataCollectors,
      MonitoringService monitoringService,
      LogsBasedMetricService logsBasedMetricService) {
    this.gaugeDataCollectors = gaugeDataCollectors;
    this.monitoringService = monitoringService;
    this.logsBasedMetricService = logsBasedMetricService;
  }

  public void record() {
    logsBasedMetricService.recordElapsedTime(
        MeasurementBundle.builder(),
        DistributionMetric.GAUGE_COLLECTION_TIME,
        () -> {
          ImmutableList.Builder<MeasurementBundle> bundlesToLogBuilder = ImmutableList.builder();
          for (GaugeDataCollector collector : gaugeDataCollectors) {
            Collection<MeasurementBundle> bundles = collector.getGaugeData();
            monitoringService.recordBundles(bundles);
            bundlesToLogBuilder.addAll(bundles);
          }
          logValues(bundlesToLogBuilder.build());
        });
  }

  private void logValues(Collection<MeasurementBundle> bundles) {
    logger.log(
        logLevel,
        bundles.stream()
            .map(b -> String.format("gauge: %s", b.toString()))
            .sorted()
            .collect(Collectors.joining("\n")));
  }
}
