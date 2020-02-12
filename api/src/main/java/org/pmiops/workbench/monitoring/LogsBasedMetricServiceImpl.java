package org.pmiops.workbench.monitoring;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Provider;
import org.pmiops.workbench.monitoring.MeasurementBundle.Builder;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class LogsBasedMetricServiceImpl implements LogsBasedMetricService {

  // We don't need to make this name different for each environment, as the
  // MonitoredResource will take care of that.
  private static final String METRICS_LOG_NAME = "debug-logs-based-metrics";
  private Logging logging;
  private StackdriverStatsExporterService stackdriverStatsExporterService;
  private Provider<Stopwatch> stopwatchProvider;

  /**
   * TODO(jaycarlton) The dependency on the OpenCensus-specific StackdriverStatsExporterService is
   * temporrary until we have a Provider of MonitoredResource.
   */
  public LogsBasedMetricServiceImpl(
      Logging logging,
      StackdriverStatsExporterService stackdriverStatsExporterService,
      Provider<Stopwatch> stopwatchProvider) {
    this.logging = logging;
    this.stackdriverStatsExporterService = stackdriverStatsExporterService;
    this.stopwatchProvider = stopwatchProvider;
  }

  @Override
  public void record(MeasurementBundle measurementBundle) {
    final ImmutableSet<LogEntry> logEntries =
        measurementBundleToJsonPayloads(measurementBundle).stream()
            .map(this::payloadToLogEntry)
            .collect(ImmutableSet.toImmutableSet());
    // This list will never be empty because of the validation in the MeasurementBundle builder
    logging.write(logEntries);
  }

  @Override
  public void recordElapsedTime(
      Builder measurementBundleBuilder, DistributionMetric distributionMetric, Runnable operation) {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    operation.run();
    stopwatch.stop();

    record(
        measurementBundleBuilder
            .addMeasurement(distributionMetric, stopwatch.elapsed().toMillis())
            .build());
  }

  @Override
  public <T> T recordElapsedTime(
      Builder measurementBundleBuilder,
      DistributionMetric distributionMetric,
      Supplier<T> operation) {
    final Stopwatch stopwatch = stopwatchProvider.get().start();
    final T result = operation.get();
    stopwatch.stop();

    record(
        measurementBundleBuilder
            .addMeasurement(distributionMetric, stopwatch.elapsed().toMillis())
            .build());
    return result;
  }

  /**
   * Each measurement bundle can contain multiple key/value pairs and a constant set of tags. We
   * need to create a separate JsonPayload for each measurement.
   *
   * <p>We drop several important fields from the Metric interface that are useful when talking to
   * OpenCensus and/or the Monitoring API. It's fairly bare bones, and we have to set up aggregation
   * on the Logging console.
   */
  private Set<JsonPayload> measurementBundleToJsonPayloads(MeasurementBundle measurementBundle) {
    final ImmutableMap<String, String> labelToValue =
        measurementBundle.getTags().entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    e -> e.getKey().getName(), e -> e.getValue().asString()));

    // Make a JsonPayload for each measurement, and include all the metric labels.
    return measurementBundle.getMeasurements().entrySet().stream()
        .map(
            entry ->
                JsonPayload.of(
                    ImmutableMap.of(
                        PayloadKey.VALUE.getKeyName(), entry.getValue().doubleValue(),
                        PayloadKey.NAME.getKeyName(), entry.getKey().getName(),
                        PayloadKey.UNIT.getKeyName(), entry.getKey().getUnit(),
                        PayloadKey.LABELS.getKeyName(), labelToValue)))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * The LogEntry structure should only vary by payload within a single running of the application.
   * The MonitoredResource should not change during execution.
   *
   * @param jsonPayload - A JsonPayload structure with the three keys in LogsBasedMetricService.
   * @return
   */
  private LogEntry payloadToLogEntry(JsonPayload jsonPayload) {
    return LogEntry.newBuilder(jsonPayload)
        .setSeverity(Severity.INFO)
        .setLogName(METRICS_LOG_NAME)
        .setResource(stackdriverStatsExporterService.getLoggingMonitoredResource())
        .build();
  }

  /**
   * Allowed labels for the JsonPayload are here.
   *
   * <p>NAME: name of the metric, to show up in the Metric Explorer. Should be snake_case. Existing
   * EventMetric class's getName() method works.
   *
   * <p>VALUE: double value for the metric for this sample. Either 1.0 for count metrics, or some
   * number in the distribution for a distribution metric. For cumulative metrics, just use a value
   * to be summed, and choose the right aggregation on the Stackdriver side. That is, there's no
   * separate option for it.
   *
   * <p>LABELS: String-String map of label to value. Should only contain keys and discrete values
   * allowed by the EventMetric and MetricLabel classes, respectively. (Using a MeasurementBundle
   * ensures this).
   *
   * <p>UNIT: Official unit of measure. It looks like you still have to set this up manually when
   * making a Logs-based metric in the GUI. I.e. it won't honor this field. But it's still good to
   * have as a reminder if you're just surfing the log.
   */
  @VisibleForTesting
  public enum PayloadKey {
    VALUE("data_point_value"),
    NAME("metric_name"),
    LABELS("labels"),
    UNIT("unit");

    private String keyName;

    PayloadKey(String keyName) {
      this.keyName = keyName;
    }

    public String getKeyName() {
      return keyName;
    }
  }
}
