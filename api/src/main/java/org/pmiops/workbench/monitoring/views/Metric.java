package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import java.util.Collections;
import java.util.List;

public enum Metric implements OpenCensusView {
  BILLING_BUFFER_SIZE(
      "billing_project_buffer_entries", "The number of billing project buffer entries."),
  BILLING_BUFFER_COUNT_BY_STATUS(
      "billing_buffer_count_by_status", "Number of projects inthe billing buffer for each status"),
  BILLING_BUFFER_AVAILABLE_PROJECT_COUNT(
      "billing_project_buffer_available_project_count",
      "Current number of billing projects with available status."),
  BILLING_BUFFER_ASSIGNING_PROJECT_COUNT(
      "billing_project_buffer_assigning_project_count",
      "Current number of billing projects with assigning status."),
  BILLING_BUFFER_CREATING_PROJECT_COUNT(
      "billing_project_buffer_creating_project_count",
      "Current number of billing projects with creating status."),
  DEBUG_CONSTANT_VALUE("debug_constant_value", "Always 101"),
  DEBUG_MILLISECONDS_SINCE_EPOCH("debug_epoch_millis", "Number of milliseconds since epoch"),
  DEBUG_RANDOM_DOUBLE(
      "debug_random_double",
      "Double value for debugging",
      "MHz",
      MeasureDouble.class,
      Aggregation.Sum.create()),
  NOTEBOOK_SAVE("notebook_save", "Save (or create) a notebook"),
  NOTEBOOK_CLONE("notebook_clone", "Clone (duplicate) a notebook"),
  NOTEBOOK_DELETE("notebook_delete", "Delete a notebook"),
  WORKSPACE_TOTAL_COUNT("workspace_total_count", "Count of all workspaces (including inactive)"),
  DATASET_COUNT("dataset_count", "Count of all datasets in existence"),
  DATASET_COUNT_BY_INVALID("dataset_count_by_invalid", "Count of all datasets by invalid status"),
  COHORT_COUNT("cohort_count", "Count of all cohorts in existence"),
  USER_COUNT_BY_DISABLED_STATUS(
      "user_count_by_disabled_status", "Count of users, labeled by disabled status");

  private final String name;
  private final String description;
  private final String unit;
  private final Aggregation aggregation;
  private List<TagKey> columns;
  private final Class measureClass;

  Metric(String name, String description) {
    this(name, description, OpenCensusView.UNITLESS_UNIT, MeasureLong.class);
  }

  Metric(String name, String description, String unit, Class measureClass) {
    this(name, description, unit, measureClass, Aggregation.LastValue.create());
  }

  Metric(
      String name, String description, String unit, Class measureClass, Aggregation aggregation) {
    this(name, description, unit, measureClass, aggregation, Collections.emptyList());
  }

  Metric(
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
