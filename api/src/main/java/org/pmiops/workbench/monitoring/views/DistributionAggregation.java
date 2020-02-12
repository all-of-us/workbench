package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableList;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Aggregation.Distribution;
import io.opencensus.stats.BucketBoundaries;

/**
 * Aggregation values to use with OpenCensus Distribution metrics. The bucket bounadries are inner
 * boundaries, so a list of [0.25, 0.50, 0.75] would actually make 5 buckets, with the lowest being
 * [0.0, 0.25), [0.25, 0.5), [0.5, 0.75), [0.75, MAX_DOUBLE].
 *
 * <p>Note: These buckets are provided for use with OpenCenss distribution metrics, which are not
 * currenlty in service. Since every MetricBase needs an Aggregation anyway, and I wanted the
 * Logs-based workaround to be a drop-in replacement as much as possible, I decided to keep them
 * around until we have a resolution on the OpenCenss issues.
 */
public enum DistributionAggregation {
  RANDOM_DOUBLE(
      UnitOfMeasure.COUNT,
      Aggregation.Distribution.create(
          BucketBoundaries.create(ImmutableList.of(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)))),
  OPERATION_TIME(
      UnitOfMeasure.MILLISECOND,
      Aggregation.Distribution.create(
          BucketBoundaries.create(
              ImmutableList.of(
                  25.0, 50.0, 75.0, 100.0, 200.0, 400.0, 600.0, 800.0, 1000.0, 2000.0, 4000.0))));

  private final UnitOfMeasure unitOfMeasure;
  private final Aggregation.Distribution distribution;

  DistributionAggregation(UnitOfMeasure unitOfMeasure, Distribution distribution) {

    this.unitOfMeasure = unitOfMeasure;
    this.distribution = distribution;
  }

  public UnitOfMeasure getUnitOfMeasure() {
    return unitOfMeasure;
  }

  public Distribution getDistribution() {
    return distribution;
  }
}
