package org.pmiops.workbench.monitoring.signals;

import com.google.appengine.repackaged.com.google.common.collect.ImmutableMap;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;

import java.util.Map;
import java.util.function.Function;

public enum GaugeSignals implements Signal {
  BILLING_BUFFER_AVAILABLE_PROJECTS("billing_project_buffer_entries",
      "The number of billing project buffer entries.",
      "projects", Measure.MeasureLong.class);

  private static final Map<Class, Function<GaugeSignals, Measure>> MEASURE_CLASS_TO_MEASURE_FUNCTION = ImmutableMap.of(
      Measure.MeasureLong.class, GaugeSignals::getMeasureLong,
      Measure.MeasureDouble.class, GaugeSignals::getMeasureDouble);
  private final String name;
  private final String description;
  private final String unit;
  private final Aggregation aggregation;
  private final Class measureClass;

  GaugeSignals(String name, String description, String unit, Class measureClass) {
    this(name, description, unit, measureClass, Aggregation.LastValue.create());
  }

  // TODO: Support doubles
  GaugeSignals(String name, String description, String unit, Class measureClass, Aggregation aggregation) {
    this.name = name;
    this.description = description;
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
  public Measure getMeasure() {
    return MEASURE_CLASS_TO_MEASURE_FUNCTION.get(measureClass).apply(this);
  }

  @Override
  public Measure.MeasureLong getMeasureLong() {
    return Measure.MeasureLong.create(name, description, unit);
  }

  @Override
  public Measure.MeasureDouble getMeasureDouble() {
    return Measure.MeasureDouble.create(name, description, unit);
  }

  @Override
  public Class getMeasureClass() {
    return measureClass;
  }

  @Override
  public Aggregation getAggregation() {
    return aggregation;
  }
}