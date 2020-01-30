package org.pmiops.workbench.monitoring;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.pmiops.workbench.monitoring.MeasurementBundle.Builder;
import org.pmiops.workbench.monitoring.views.CumulativeMetric;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.pmiops.workbench.monitoring.views.MetricBase;

public interface MonitoringService {

  int DELTA_VALUE = 1;

  /**
   * Record an occurrence of a counted (a.k.a. delta or cumulative) time series. These are events
   * that are typically measured more for frequency than for absolute value.
   *
   * @param eventMetric
   */
  default void recordEvent(CumulativeMetric eventMetric) {
    recordValue(eventMetric, DELTA_VALUE);
  }

  default void recordEvent(CumulativeMetric eventMetric, Map<TagKey, TagValue> tags) {
    recordValues(ImmutableMap.of(eventMetric, DELTA_VALUE), tags);
  }

  default void recordValue(MetricBase metric, Number value) {
    recordValues(ImmutableMap.of(metric, value));
  }

  default void recordValues(Map<MetricBase, Number> metricToValue) {
    recordValues(metricToValue, Collections.emptyMap());
  }

  /**
   * Record multiple values at once. An attachment map allows associating these measurements with
   * metadata (shared across all samples). We use a single MeasureMap for all the entries in both
   * maps.
   *
   * @param metricToValue key/value pairs for time series. These need not be related, but any
   *     attachments should apply to all entries in this map.
   * @param tags Map of String/AttachmentValue pairs to be associated with these data.
   */
  void recordValues(Map<MetricBase, Number> metricToValue, Map<TagKey, TagValue> tags);

  default void recordBundle(MeasurementBundle measurementBundle) {
    recordValues(measurementBundle.getMeasurements(), measurementBundle.getTags());
  }

  // Record each ViewBundle object separately, so that we don't
  // mix attachments across MeasureMaps.
  default void recordBundles(Collection<MeasurementBundle> viewBundles) {
    viewBundles.forEach(this::recordBundle);
  }

  /**
   * Record a pre-aggregated histogram to a distribution metric.
   * @param metric
   * @param values
   */
  void recordDistribution(DistributionMetric metric, List<Double> values);

  /**
   * Use a Stopwatch to time the supplied operation, then add a measurement to the supplied
   * measurementBundleBuilder and record the associated DistributionMetric.
   *
   * @param measurementBundleBuilder - Builder for a MeasurementBundle to be recorded. Typically
   *     only has tags.
   * @param distributionMetric - Metric to be recorded. Always a distribution, as gauge and count
   *     don't make sense for timings
   * @param operation - Code to be run, e.g. () -> myService.computeThings()
   */
  default void timeAndRecordOperation(
      Builder measurementBundleBuilder, DistributionMetric distributionMetric, Runnable operation) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    operation.run();
    stopwatch.stop();
    recordBundle(
        measurementBundleBuilder
            .addMeasurement(distributionMetric, stopwatch.elapsed().toMillis())
            .build());
  }

  /**
   * Same as above, but returns the result of the operation
   *
   * @param measurementBundleBuilder - Builder for a MeasurementBundle to be recorded. Typically
   *     only has tags.
   * @param distributionMetric - Metric to be recorded. Always a distribution, as gauge and count
   *     don't make sense for timings
   * @param operation - Code to be run, e.g. myService::getFooList
   */
  default <T> T timeAndRecordOperation(
      Builder measurementBundleBuilder,
      DistributionMetric distributionMetric,
      Supplier<T> operation) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    final T result = operation.get();
    stopwatch.stop();
    recordBundle(
        measurementBundleBuilder
            .addMeasurement(distributionMetric, stopwatch.elapsed().toMillis())
            .build());
    return result;
  }
}
