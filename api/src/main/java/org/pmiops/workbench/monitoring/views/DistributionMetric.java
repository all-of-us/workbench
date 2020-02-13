package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import java.util.Collections;
import java.util.List;
import org.pmiops.workbench.monitoring.labels.MetricLabel;

/**
 * Define info for MetricDescriptors that are aggregated as Distributions (aka percentiles or
 * heatmaps). Basically we capture a histogram for each sampled timeseries data point.
 *
 * <p>Note: We are using this OpenCensus-centric enum with the LogsBasedMetricService workaround so
 * that existing (if not yet working) definitions could be reused. In Stackdriver Logging, these
 * metrics are always double-valued and the user doesn't specify an aggregation. Also, we do not
 * want to register custom metric views for these metrics yet, so they are omitted in
 * MonitoringServiceImpl#registerMetricViews() for now. It gets a bit confusing having a logging and
 * custom metric with the same name (even if the suffix is logging/user instead of
 * custom.googleapis.com/).
 */
public enum DistributionMetric implements Metric {
  COHORT_OPERATION_TIME(
      "cohort_operation_time",
      "Time to complete Cohort-related operation.",
      Collections.singletonList(MetricLabel.OPERATION_NAME),
      DistributionAggregation.OPERATION_TIME,
      MeasureLong.class),
  GAUGE_COLLECTION_TIME(
      "gauge_collection_time",
      "Time to get data from all gauges",
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
}
