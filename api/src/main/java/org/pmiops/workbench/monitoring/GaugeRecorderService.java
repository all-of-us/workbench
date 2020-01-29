package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.pmiops.workbench.monitoring.views.CumulativeMetric;
import org.springframework.stereotype.Service;

@Service
public class GaugeRecorderService {
  private static final Logger logger = Logger.getLogger(GaugeRecorderService.class.getName());

  private final List<GaugeDataCollector> gaugeDataCollectors;
  private final MonitoringService monitoringService;
  private final Random random = new Random();

  // For local debugging, change this to Level.INFO or higher
  private final Level logLevel = Level.FINE;

  public GaugeRecorderService(
      List<GaugeDataCollector> gaugeDataCollectors, MonitoringService monitoringService) {
    this.gaugeDataCollectors = gaugeDataCollectors;
    this.monitoringService = monitoringService;
  }

  public void record() {
    ImmutableList.Builder<MeasurementBundle> bundlesToLogBuilder = ImmutableList.builder();
    for (GaugeDataCollector collector : gaugeDataCollectors) {
      Collection<MeasurementBundle> bundles = collector.getGaugeData();
      monitoringService.recordBundles(bundles);
      bundlesToLogBuilder.addAll(bundles);
    }
    logValues(bundlesToLogBuilder.build());

    debugRecordCumulativeAndDistribution();
  }

  private void debugRecordCumulativeAndDistribution() {
    sendDistribution(100);
    sendEvents(100);
  }

  private void sendEvents(int eventCount) {
    int numEvents = (int) Math.round(random.nextDouble() * eventCount);
    IntStream.rangeClosed(0, numEvents - 1)
        .forEach(i ->  monitoringService.recordEvent(CumulativeMetric.DEBUG_COUNT));
  }

  private void sendDistribution(int sampleCount) {
    Stream.generate(random::nextDouble)
        .limit(sampleCount)
        .collect(Collectors.toList())
        .forEach(v -> monitoringService.recordValue(DistributionMetric.UNIFORM_RANDOM_SAMPLE, v));
  }

  private void logValues(Collection<MeasurementBundle> bundles) {
    logger.log(
        logLevel,
        bundles.stream()
            .map(MeasurementBundle::toString)
            .sorted()
            .collect(Collectors.joining("\n")));
  }
}
