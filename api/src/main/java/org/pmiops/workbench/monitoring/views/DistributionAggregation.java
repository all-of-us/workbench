package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableList;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Aggregation.Distribution;
import io.opencensus.stats.BucketBoundaries;
import java.util.List;

public enum DistributionAggregation {
  RANDOM_DOUBLE(
      UnitOfMeasure.COUNT,
      ImmutableList.of(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)),
  OPERATION_TIME(
      UnitOfMeasure.MILLISECOND,
      ImmutableList.of(25.0, 50.0, 75.0, 100.0, 200.0, 400.0, 600.0, 800.0, 1000.0, 2000.0, 4000.0,
          6000.0));

  private final UnitOfMeasure unitOfMeasure;
  private List<Double> boundaryValues;

  DistributionAggregation(UnitOfMeasure unitOfMeasure, List<Double> boundaryValues) {
    this.unitOfMeasure = unitOfMeasure;
    this.boundaryValues = boundaryValues;
  }

  public UnitOfMeasure getUnitOfMeasure() {
    return unitOfMeasure;
  }

  public Distribution getDistribution() {
    return Aggregation.Distribution.create(
        BucketBoundaries.create(boundaryValues));
  }
}
