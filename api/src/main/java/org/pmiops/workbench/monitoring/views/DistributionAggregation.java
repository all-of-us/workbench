package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableList;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Aggregation.Distribution;
import io.opencensus.stats.BucketBoundaries;

public enum DistributionAggregation {
  TIME(
      UnitOfMeasure.MILLISECOND,
      Aggregation.Distribution.create(
          BucketBoundaries.create(
              ImmutableList.of(
                  0.0, 25.0, 50.0, 75.0, 100.0, 200.0, 400.0, 600.0, 800.0, 1000.0, 2000.0, 4000.0,
                  6000.0))));

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
