package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import java.util.Collections;
import java.util.List;

public enum GaugeMetric implements Metric {
  BILLING_BUFFER_PROJECT_COUNT(
      "billing_buffer_project_count", "Number of projects in the billing buffer for each status"),
  BILLING_BUFFER_SIZE(
      "billing_project_buffer_entries", "The number of billing project buffer entries."),
  DATASET_COUNT("dataset_count", "Count of all datasets in existence"),
  DEBUG_CONSTANT_VALUE("debug_constant_value", "Always 101"),
  DEBUG_MILLISECONDS_SINCE_EPOCH("debug_epoch_millis", "Number of milliseconds since epoch"),
  DEBUG_RANDOM_DOUBLE(
      "debug_random_double",
      "Double value for debugging",
      "MHz",
      MeasureDouble.class,
      Aggregation.Sum.create()),
  WORKSPACE_TOTAL_COUNT("workspace_total_count", "Count of all workspaces (including inactive)"),
  WORKSPACE_COUNT_BY_ACTIVE_STATUS(
      "workspace_count_by_active_status", "Count of workspaces by active status"),
  WORKSPACE_COUNT_BY_DATA_ACCESS_LEVEL(
      "workspace_count_by_data_access_level", "Count of workspaces for each data access level"),
  COHORT_COUNT("cohort_count", "Count of all cohorts in existence"),
  COHORT_REVIEW_COUNT("cohort_review_count", "Total number of cohort reviews in existence"),
  USER_COUNT("user_count", "total number of users");

  private final String name;
  private final String description;
  private final String unit;
  private final Aggregation aggregation;
  private List<TagKey> columns;
  private final Class measureClass;

  GaugeMetric(String name, String description) {
    this(name, description, Metric.UNITLESS_UNIT, MeasureLong.class);
  }

  GaugeMetric(String name, String description, String unit, Class measureClass) {
    this(name, description, unit, measureClass, Aggregation.LastValue.create());
  }

  GaugeMetric(
      String name, String description, String unit, Class measureClass, Aggregation aggregation) {
    this(name, description, unit, measureClass, aggregation, Collections.emptyList());
  }

  GaugeMetric(
      String name,
      String description,
      String unit,
      Class measureClass,
      Aggregation aggregation,
      List<TagKey> columns) {
    this.name = name;
    this.description = description;
    this.unit = unit;
    this.measureClass = measureClass;
    this.aggregation = aggregation;
    this.columns = columns;
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
  public List<TagKey> getColumns() {
    return columns;
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
        + columns
        + ", measureClass="
        + measureClass
        + '}';
  }
}
