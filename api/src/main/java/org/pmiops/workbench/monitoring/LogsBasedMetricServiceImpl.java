package org.pmiops.workbench.monitoring;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.monitoring.views.Metric;
import org.springframework.stereotype.Service;

@Service
public class LogsBasedMetricServiceImpl implements LogsBasedMetricService {

  // We don't need to make this name different for each environment, as the
  // MonitoredResource will take care of that.
  private static final String METRICS_LOG_NAME = "debug-logs-based-metrics";
  private Logging logging;
  private StackdriverStatsExporterService stackdriverStatsExporterService;

  /**
   * TODO(jaycarlton) The dependency on the OpenCensus-specific StackdriverStatsExporterService is
   * temporrary until we have a Provider of MonitoredResource.
   */
  public LogsBasedMetricServiceImpl(
      Logging logging, StackdriverStatsExporterService stackdriverStatsExporterService) {
    this.logging = logging;
    this.stackdriverStatsExporterService = stackdriverStatsExporterService;
  }

  @Override
  public void recordMeasurementBundle(MeasurementBundle measurementBundle) {
    final ImmutableSet<LogEntry> logEntries =
        measurementBundleToJsonPayloads(measurementBundle).stream()
            .map(this::payloadToLogEntry)
            .collect(ImmutableSet.toImmutableSet());
    // This list will never be empty because of the validation in the MeasurementBUndle builder
    logging.write(logEntries);
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

    final ImmutableSet.Builder<JsonPayload> resultBuilder = ImmutableSet.builder();
    // Make a JsonPayload for each measurement, and include the whole map.
    for (Map.Entry<Metric, Number> entry : measurementBundle.getMeasurements().entrySet()) {
      resultBuilder.add(
          JsonPayload.of(
              ImmutableMap.of(
                  LogsBasedMetricService.METRIC_VALUE_KEY, entry.getValue().doubleValue(),
                  LogsBasedMetricService.METRIC_NAME_KEY, entry.getKey().getName(),
                  LogsBasedMetricService.METRIC_LABELS_KEY, labelToValue)));
    }

    return resultBuilder.build();
  }

  private LogEntry payloadToLogEntry(Payload payload) {
    return LogEntry.newBuilder(payload)
        .setSeverity(Severity.INFO)
        .setLogName(METRICS_LOG_NAME)
        .setResource(stackdriverStatsExporterService.getLoggingMonitoredResource())
        .build();
  }
}
