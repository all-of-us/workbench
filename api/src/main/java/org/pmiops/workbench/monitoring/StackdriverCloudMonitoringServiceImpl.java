package org.pmiops.workbench.monitoring;

import com.google.api.Distribution;
import com.google.api.Distribution.BucketOptions;
import com.google.api.Distribution.BucketOptions.Explicit;
import com.google.api.Distribution.BucketOptions.Linear;
import com.google.api.Metric;
import com.google.api.MetricDescriptor.MetricKind;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.monitoring.v3.TypedValue.Builder;
import com.google.protobuf.util.Timestamps;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
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
  private static final Duration INTERVAL_SIZE = Duration.ofMinutes(1);

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
    final ImmutableList<TimeSeries> timeSeriesList = metricToValue.entrySet().stream()
        .map(entry -> makeTimeSeries(tags, entry.getKey(), entry.getValue()))
        .collect(ImmutableList.toImmutableList());

    recordTimeSeriesList(timeSeriesList);
  }

  private void recordTimeSeriesList(Iterable<TimeSeries> timeSeriesList) {
    final CreateTimeSeriesRequest.Builder requestBuilder =
        CreateTimeSeriesRequest.newBuilder()
            .setName(ProjectName.of(workbenchConfigProvider.get().server.projectId).toString())
            .addAllTimeSeries(timeSeriesList);
    final CreateTimeSeriesRequest createTimeSeriesRequest = requestBuilder.build();
    metricServiceClient.createTimeSeries(createTimeSeriesRequest);
    logger.info(
        String.format("Created TimeSeries with request: %s", createTimeSeriesRequest.toString()));
  }

  private TimeSeries makeTimeSeries(Map<TagKey, TagValue> tags, MetricBase metricBase,
      Number value) {
    final TypedValue typedValue = toTypedValue(metricBase, value);
    return makeTimeSeries(tags, metricBase, typedValue);
  }

  @NotNull
  private TimeSeries makeTimeSeries(Map<TagKey, TagValue> tags, MetricBase metricBase,
      TypedValue typedValue) {
    final TimeInterval interval = makeCurrentInterval(metricBase.getMetricKind());
    final Point point = Point.newBuilder().setValue(typedValue).setInterval(interval).build();
    final Metric metric = toStackdriverMetric(metricBase, tags);
    return TimeSeries.newBuilder()
        .addPoints(point)
        .setMetric(metric)
        .setResource(monitoredResourceProvider.get())
        .build();
  }

  private TypedValue makeDistributionTypedValue(DistributionMetric distributionMetric, List<Double> values) {
    final List<Long> bucketCounts = distributionMetric.getHistogram(values);
    final ImmutableList<Double> boundaries = ImmutableList.copyOf(distributionMetric.getBoundaryValues());

    return TypedValue.newBuilder()
        .setDistributionValue(Distribution.newBuilder()
            .setBucketOptions(
                BucketOptions.newBuilder()
                    .setExplicitBuckets(Explicit.newBuilder().addAllBounds(boundaries).build()))
                .addAllBucketCounts(bucketCounts)
                .setCount(values.size())).build();
  }

  @Override
  public void recordDistribution(DistributionMetric metric, List<Double> values) {
    final TypedValue typedValue = makeDistributionTypedValue(metric, values);

    final TimeSeries timeSeries = makeTimeSeries(Collections.emptyMap(), metric, typedValue);
    recordTimeSeriesList(Collections.singleton(timeSeries));
  }

  @NotNull
  private TypedValue toTypedValue(MetricBase metricBase, Number value) {
    final Builder typedValueBuilder = TypedValue.newBuilder();

    if (metricBase instanceof DistributionMetric) {
      if (metricBase.getAggregation() instanceof Aggregation.Distribution) {
        final Aggregation.Distribution openCensusDistribution =
            (Aggregation.Distribution) metricBase.getAggregation();
        final ImmutableList<Double> boundaries =
            ImmutableList.copyOf(openCensusDistribution.getBucketBoundaries().getBoundaries());

        if (boundaries.isEmpty()) {
          throw new IllegalArgumentException(
              String.format(
                  "Empty boundaries array for distribution metric %s", metricBase.getName()));
        }

        typedValueBuilder.setDistributionValue(
            Distribution.newBuilder()
                .setBucketOptions(
                    BucketOptions.newBuilder()
//                        .setLinearBuckets(Linear.newBuilder()
//                            .setWidth(1.0)
//                            .setNumFiniteBuckets(10)
//                            .build()))
                        .setExplicitBuckets(Explicit.newBuilder().addAllBounds(boundaries).build()))
                .addAllBucketCounts(getBucketCounts(boundaries, value.doubleValue()))
//                .setBucketCounts(bucketIndex(boundaries, value.doubleValue()), 1)
                .setCount(1));
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Unexpedcted aggregation type %s for distribution metric: %s",
                metricBase.getAggregation().getClass().getName(), metricBase.getName()));
      }
    } else {
      if (metricBase.getMeasureClass().equals(MeasureLong.class)) {
        typedValueBuilder.setInt64Value(value.longValue());
      } else if (metricBase.getMeasureClass().equals(MeasureDouble.class)) {
        typedValueBuilder.setDoubleValue(value.doubleValue());
      } else {
        logger.log(
            Level.WARNING,
            String.format("Unrecognized measure class %s", metricBase.getMeasureClass().getName()));
      }
    }

    return typedValueBuilder.build();
  }

  /**
   * Find the index for this (single) point.
   *
   * @param boundaries
   * @param value
   * @return
   */
  private List<Long> getBucketCounts(List<Double> boundaries, double value) {
    logger.info(String.format("Finding index for %f in %s", value, boundaries));
    List<Long> result = Stream.generate(() -> 0L)
        .limit(boundaries.size() - 1)
        .collect(Collectors.toList());
    int index = 0;
    for(Double upperBound : boundaries) {
      if (value <= upperBound) {
        result.set(index, 1L);
      }
    }
    return result;
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
