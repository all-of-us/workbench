package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableSet;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureLong;
import java.util.Collections;
import java.util.Set;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;

public enum GaugeMetric implements Metric {
  BILLING_BUFFER_PROJECT_COUNT(
      "billing_buffer_project_count",
      "Number of projects in the billing buffer for each status",
      ImmutableSet.of(MetricLabel.BUFFER_ENTRY_STATUS)),
  COHORT_COUNT("cohort_count_2", "Count of all cohorts in existence"),
  COHORT_REVIEW_COUNT("cohort_review_count_2", "Total number of cohort reviews in existence"),
  DATASET_COUNT(
      "dataset_count_2",
      "Count of all datasets in existence",
      Collections.singleton(MetricLabel.DATASET_INVALID)),
  USER_COUNT(
      "user_count_2",
      "total number of users",
      ImmutableSet.of(
          MetricLabel.USER_BYPASSED_BETA,
          MetricLabel.USER_DISABLED,
          MetricLabel.DATA_ACCESS_LEVEL)),
  WORKSPACE_COUNT(
      "workspace_count",
      "Count of all workspaces",
      ImmutableSet.of(MetricLabel.WORKSPACE_ACTIVE_STATUS, MetricLabel.DATA_ACCESS_LEVEL),
      Metric.UNITLESS_UNIT,
      MeasureLong.class,
      Aggregation.LastValue.create());

  private final String name;
  private final String description;
  private final String unit;
  private final Aggregation aggregation;
  private final Class measureClass;
  private final Set<MetricLabel> allowedAttachments;

  GaugeMetric(String name, String description) {
    this(name, description, Collections.emptySet(), Metric.UNITLESS_UNIT, MeasureLong.class);
  }

  GaugeMetric(String name, String description, Set<MetricLabel> labels) {
    this(name, description, labels, Metric.UNITLESS_UNIT, MeasureLong.class);
  }

  GaugeMetric(
      String name, String description, Set<MetricLabel> labels, String unit, Class measureClass) {
    this(name, description, labels, unit, measureClass, Aggregation.LastValue.create());
  }

  GaugeMetric(
      String name,
      String description,
      Set<MetricLabel> labels,
      String unit,
      Class measureClass,
      Aggregation aggregation) {
    this.name = name;
    this.description = description;
    this.allowedAttachments = labels;
    this.unit = unit;
    this.measureClass = measureClass;
    this.aggregation = aggregation;
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
    return unit;
  }

  @Override
  public Class getMeasureClass() {
    return measureClass;
  }

  @Override
  public Aggregation getAggregation() {
    return aggregation;
  }

  @Override
  public Set<MetricLabel> getLabels() {
    return allowedAttachments;
  }

  @Override
  public String toString() {
    return "Metric{"
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
        + aggregation
        + ", columns="
        + getColumns()
        + ", measureClass="
        + measureClass
        + '}';
  }
}
