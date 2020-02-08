package org.pmiops.workbench.monitoring;

import java.util.function.Supplier;
import org.pmiops.workbench.monitoring.MeasurementBundle.Builder;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.pmiops.workbench.monitoring.views.EventMetric;

/**
 * We are temporarily using the Stackdriver Logs-based metric facility to record and chart
 * count (event), cumulative, and distribution metrics
 */
public interface LogsBasedMetricService {

  /**
   * This method of metric recording is essentially one LogEntry per Timeseries per sample; Reusing
   * the MeasurementBundle class from the Monitoring implementation is useful because of
   * familiarity, simplicity, and replaceability. If we get unstuck on the OpenCensus side, spots
   * that record gauge and cumulative metrics via this interface can quickly be re-wired to the
   * OpenCensus version.
   *
   * @param measurementBundle - Bundle of one or more measurements corresonding to the same labels
   *     map
   */
  void record(MeasurementBundle measurementBundle);

  /**
   * Record a simple event (count) metric with a value of 1 and no labels
   *
   * @param eventMetric
   */
  default void recordEvent(EventMetric eventMetric) {
    record(MeasurementBundle.builder().addEvent(eventMetric).build());
  }
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
  void timeAndRecord(
      Builder measurementBundleBuilder, DistributionMetric distributionMetric, Runnable operation);

  /**
   * Same as above, but returns the result of the operation
   *
   * @param measurementBundleBuilder - Builder for a MeasurementBundle to be recorded. Typically
   *     only has tags.
   * @param distributionMetric - Metric to be recorded. Always a distribution, as gauge and count
   *     don't make sense for timings
   * @param operation - Code to be run, e.g. myService::getFooList
   */
  <T> T timeAndRecord(
      Builder measurementBundleBuilder,
      DistributionMetric distributionMetric,
      Supplier<T> operation);

}
