package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import java.util.Collections;
import java.util.List;

public enum MonitoringViews implements OpenCensusStatsViewInfo {
  BILLING_BUFFER_SIZE(
      "billing_project_buffer_entries", "The number of billing project buffer entries."),
  BILLING_BUFFER_AVAILABLE_PROJECT_COUNT(
      "billing_project_buffer_available_project_count",
      "Current number of billing projects with available status."),
  BILLING_BUFFER_ASSIGNING_PROJECT_COUNT(
      "billing_project_buffer_assigning_project_count",
      "Current number of billing projects with assigning status."),
  BILLING_BUFFER_CREATING_PROJECT_COUNT(
      "billing_project_buffer_creating_project_count",
      "Current number of billing projects with creating status."),
  DEBUG_MILLISECONDS_SINCE_EPOCH("debug_epoch_millis", "Number of milliseconds since epoch"),
  DEBUG_RANDOM_DOUBLE(
      "debug_random_double",
      "Double value for debugging",
      "MHz",
      MeasureDouble.class,
      Aggregation.Sum.create()),
  NOTEBOOK_SAVE("notebook_save", "Save (or create) a notebook"),
  NOTEBOOK_CLONE("notebook_clone", "Clone (duplicate) a notebook"),
  NOTEBOOK_DELETE("notebook_delete", "Delete a notebook");

  private final String name;
  private final String description;
  private final String unit;
  private final Aggregation aggregation;
  private List<TagKey> columns;
  private final Class measureClass;

  MonitoringViews(String name, String description) {
    this(name, description, OpenCensusStatsViewInfo.SCALAR_UNIT, MeasureLong.class);
  }

  MonitoringViews(String name, String description, String unit, Class measureClass) {
    this(name, description, unit, measureClass, Aggregation.LastValue.create());
  }

  MonitoringViews(
      String name, String description, String unit, Class measureClass, Aggregation aggregation) {
    this(name, description, unit, measureClass, aggregation, Collections.emptyList());
  }

  MonitoringViews(
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
}
