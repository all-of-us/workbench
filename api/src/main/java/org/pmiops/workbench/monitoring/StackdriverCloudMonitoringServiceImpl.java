package org.pmiops.workbench.monitoring;

import com.google.api.Metric;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.monitoring.views.MetricBase;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * As an alternative to the OpenCensus library, we provide a way to write using the Google APIs
 * directly
 */
@Service
@Primary
public class StackdriverCloudMonitoringServiceImpl implements MonitoringService {

  private static final Logger logger =
      Logger.getLogger(StackdriverCloudMonitoringServiceImpl.class.getName());
  public static final Duration INTERVAL_SIZE = Duration.ofSeconds(1);

  private Clock clock;
  private MetricServiceClient metricServiceClient;
  private Provider<MonitoredResource> monitoredResourceProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  public StackdriverCloudMonitoringServiceImpl(
      Clock clock,
      MetricServiceClient metricServiceClient,
      Provider<MonitoredResource> monitoredResourceProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.clock = clock;
    this.metricServiceClient = metricServiceClient;
    this.monitoredResourceProvider = monitoredResourceProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void recordValues(Map<MetricBase, Number> metricToValue, Map<TagKey, TagValue> tags) {
    final CreateTimeSeriesRequest.Builder requestBuilder =
        CreateTimeSeriesRequest.newBuilder()
            .setName(ProjectName.of(workbenchConfigProvider.get().server.projectId).toString());

    metricToValue.forEach(
        (metricBase, value) -> {
          final TypedValue.Builder typedValueBuilder = TypedValue.newBuilder();
          if (metricBase.getMeasureClass().equals(MeasureLong.class)) {
            typedValueBuilder.setInt64Value(value.longValue());
          } else if (metricBase.getMeasureClass().equals(MeasureDouble.class)) {
            typedValueBuilder.setDoubleValue(value.doubleValue());
          } else {
            logger.log(
                Level.WARNING,
                String.format(
                    "Unrecognized measure class %s", metricBase.getMeasureClass().getName()));
          }
          final TimeInterval interval = makeCurrentInterval(metricBase.getMetricKind());
          final Point point =
              Point.newBuilder().setValue(typedValueBuilder.build()).setInterval(interval).build();
          final Metric metric = toStackdriverMetric(metricBase, tags);
          final TimeSeries timeSeries =
              TimeSeries.newBuilder()
                  .addPoints(point)
                  .setMetric(metric)
                  .setResource(monitoredResourceProvider.get())
                  .build();
          requestBuilder.addTimeSeries(timeSeries);
        });

    CreateTimeSeriesRequest createTimeSeriesRequest = requestBuilder.build();
    metricServiceClient.createTimeSeries(createTimeSeriesRequest);
    logger.info(
        String.format("Created TimeSeries with request: %s", createTimeSeriesRequest.toString()));
  }

  // Convert a MetricBase object, along with a map of label-value pairs (as OC TagKey/TagValue for
  // now)
  // into the Metric object we need to build a timeseries.
  private Metric toStackdriverMetric(MetricBase metricBase, Map<TagKey, TagValue> tagKeyToValue) {
    final Map<String, String> stringKeyToValue =
        tagKeyToValue.entrySet().stream()
            .map(e -> new SimpleImmutableEntry<>(e.getKey().getName(), e.getValue().asString()))
            .collect(
                Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));
    return Metric.newBuilder()
        .setType(metricBase.getMetricPathName())
        .putAllLabels(stringKeyToValue)
        .build();
  }

  @NotNull
  private TimeInterval makeCurrentInterval(MetricKind metricKind) {
    final Instant now = clock.instant();
    final Instant earlier = now.minus(INTERVAL_SIZE);
    final TimeInterval.Builder resultBuilder =
        TimeInterval.newBuilder().setEndTime(Timestamps.fromMillis(now.toEpochMilli()));
    if (metricKind != MetricKind.GAUGE) {
      resultBuilder.setStartTime(Timestamps.fromMillis(earlier.toEpochMilli()));
    }
    return resultBuilder.build();
  }
}
