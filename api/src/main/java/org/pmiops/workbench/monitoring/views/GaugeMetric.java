package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableList;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Aggregation.LastValue;
import io.opencensus.stats.Measure.MeasureLong;
import java.util.Collections;
import java.util.List;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;

public enum GaugeMetric implements Metric {
  BILLING_BUFFER_PROJECT_COUNT(
      "billing_buffer_project_count",
      "Number of projects in the billing buffer for each status",
      ImmutableList.of(MetricLabel.BUFFER_ENTRY_STATUS)),
  COHORT_COUNT("cohort_count_2", "Count of all cohorts in existence"),
  COHORT_REVIEW_COUNT("cohort_review_count_2", "Total number of cohort reviews in existence"),
  DATASET_COUNT(
      "dataset_count_2",
      "Count of all datasets in existence",
      ImmutableList.of(MetricLabel.DATASET_INVALID)),
  USER_COUNT(
      "user_count_2",
      "total number of users",
      ImmutableList.of(
          MetricLabel.USER_BYPASSED_BETA,
          MetricLabel.USER_DISABLED,
          MetricLabel.DATA_ACCESS_LEVEL)),
  WORKSPACE_COUNT(
      "workspace_count_3",
      "Count of all workspaces",
      ImmutableList.of(MetricLabel.WORKSPACE_ACTIVE_STATUS, MetricLabel.DATA_ACCESS_LEVEL),
      UnitOfMeasure.COUNT,
      MeasureLong.class);

  public static final LastValue AGGREGATION = LastValue.create();
  private final String name;
  private final String description;
  private final UnitOfMeasure unit;
  private final Class measureClass;
  private final List<MetricLabel> allowedAttachments;

  GaugeMetric(String name, String description) {
    this(name, description, Collections.emptyList(), UnitOfMeasure.COUNT, MeasureLong.class);
  }

  GaugeMetric(String name, String description, List<MetricLabel> labels) {
    this(name, description, labels, UnitOfMeasure.COUNT, MeasureLong.class);
  }

  GaugeMetric(
      String name,
      String description,
      List<MetricLabel> labels,
      UnitOfMeasure unit,
      Class measureClass) {
    this.name = name;
    this.description = description;
    this.allowedAttachments = labels;
    this.unit = unit;
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
    return unit.getUcmSymbol();
  }

  @Override
  public Class getMeasureClass() {
    return measureClass;
  }

  /**
   * In Stackdriver, using LastValue is the only way to signify that a metric is a gauge. It turns
   * out it's what we want anyway for gauges, since effectively we only need one value per interval,
   * taken at the sampling time.
   */
  @Override
  public Aggregation getAggregation() {
    return AGGREGATION;
  }

  @Override
  public List<MetricLabel> getLabels() {
    return allowedAttachments;
  }

  @Override
  public String toString() {
    return "GaugeMetric{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", unit='"
        + unit
        + '\''
        + ", aggregation="
        + getAggregation()
        + ", columns="
        + getColumns()
        + ", measureClass="
        + measureClass
        + '}';
  }
}
