package org.pmiops.workbench.monitoring.signals;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public enum MonitoringViews implements StatsViewProperties {
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
      "Current number of billing projects with creating status.");

  private static final Map<Class, Function<MonitoringViews, Measure>>
      MEASURE_CLASS_TO_MEASURE_FUNCTION =
          ImmutableMap.of(
              MeasureLong.class, MonitoringViews::getMeasureLong,
              MeasureDouble.class, MonitoringViews::getMeasureDouble);
  private static final String SCALAR_UNIT = "";

  private final String name;
  private final String description;
  private final String unit;
  private final Aggregation aggregation;
  private List<TagKey> columns;
  private final Class measureClass;

  MonitoringViews(String name, String description) {
    this(name, description, SCALAR_UNIT, MeasureLong.class);
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
  public Measure getMeasure() {
    return MEASURE_CLASS_TO_MEASURE_FUNCTION.get(measureClass).apply(this);
  }

  @Override
  public MeasureLong getMeasureLong() {
    return MeasureLong.create(name, description, unit);
  }

  @Override
  public MeasureDouble getMeasureDouble() {
    return MeasureDouble.create(name, description, unit);
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
