package org.pmiops.workbench.monitoring.views;

import com.google.api.MetricDescriptor.MetricKind;
import com.google.common.collect.ImmutableList;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;

public enum DistributionMetric implements MetricBase {
  COHORT_OPERATION_TIME(
      "cohort_operation_time",
      "Time to complete Cohort-related operation.",
      Collections.singletonList(MetricLabel.OPERATION_NAME),
      DistributionAggregation.OPERATION_TIME,
      MeasureLong.class),
  UNIFORM_RANDOM_SAMPLE(
      "random_sample_2",
      "Random values",
      Collections.emptyList(),
      DistributionAggregation.RANDOM_DOUBLE,
      MeasureDouble.class),
  WORKSPACE_OPERATION_TIME(
      "workspace_operation_time",
      "Time to complete Workspace-related operation.",
      Collections.singletonList(MetricLabel.OPERATION_NAME),
      DistributionAggregation.OPERATION_TIME,
      MeasureLong.class);

  private final String name;
  private final String description;
  private final List<MetricLabel> metricLabels;
  private final DistributionAggregation distributionAggregation;
  private final Class measureClass;

  DistributionMetric(
      String name,
      String description,
      List<MetricLabel> metricLabels,
      DistributionAggregation distributionAggregation,
      Class measureClass) {
    this.name = name;
    this.description = description;
    this.metricLabels = metricLabels;
    this.distributionAggregation = distributionAggregation;
    this.measureClass = measureClass;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getUnit() {
    return distributionAggregation.getUnitOfMeasure().getUcmSymbol();
  }

  @Override
  public Class getMeasureClass() {
    return measureClass;
  }

  @Override
  public Aggregation getAggregation() {
    return distributionAggregation.getDistribution();
  }

  @Override
  public List<MetricLabel> getLabels() {
    return metricLabels;
  }

  @Override
  public MetricKind getMetricKind() {
    return MetricKind.CUMULATIVE;
  }

  public List<Long> getHistogram(List<Double> values) {
    final ImmutableList<Double> boundaryValues = ImmutableList.copyOf(getBoundaryValues());
//    final ArrayList<Long> result = new ArrayList<>(boundaryValues.size());
    final Long[] resultArray = new Long[boundaryValues.size() - 1];

    for (double value : values) {
      final int index = bucketIndex(value, boundaryValues);
      final long existingCount = resultArray[index];
      resultArray[bucketIndex(value, boundaryValues)] = existingCount + 1;
    }

    return Arrays.asList(resultArray);
  }

  /**
   * Pull the double values used by Stackdriver when setting up a bucket.
   */
  public List<Double> getBoundaryValues() {
    return ImmutableList.copyOf(distributionAggregation.getDistribution().getBucketBoundaries().getBoundaries());
  }

  private int bucketIndex(double value, ImmutableList<Double> boundaryValues) {
    int index = 0;
    for (double boundaryValue : boundaryValues) {
      if (value <= boundaryValue) {
        return index;
      }
      index++;
    }
    return Math.max(boundaryValues.size() - 1, 0);
  }
}
