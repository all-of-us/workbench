package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureLong;
import java.util.Collections;
import java.util.List;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;

public enum DistributionMetric implements Metric {
  LIST_WORKSPACES_TIME(
      "operation_time_tmp",
      "Time to list complete some operation.",
      Collections.singletonList(MetricLabel.OPERATION_NAME),
      DistributionAggregation.TIME,
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
}
