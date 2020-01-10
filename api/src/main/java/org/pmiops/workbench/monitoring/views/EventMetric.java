package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import java.util.Collections;
import java.util.List;

/** Metric enum values for events to be counted. */
public enum EventMetric implements Metric {
  NOTEBOOK_CLONE("notebook_clone", "Clone (duplicate) a notebook"),
  NOTEBOOK_DELETE("notebook_delete", "Delete a notebook"),
  NOTEBOOK_SAVE("notebook_save", "Save (or create) a notebook");

  private final String name;
  private final String description;
  private List<TagKey> columns;

  EventMetric(String name, String description, List<TagKey> columns) {
    this.name = name;
    this.description = description;
    this.columns = columns;
  }

  EventMetric(String name, String description) {
    this(name, description, Collections.emptyList());
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
    return Metric.UNITLESS_UNIT;
  }

  @Override
  public Class getMeasureClass() {
    return MeasureLong.class;
  }

  @Override
  public Aggregation getAggregation() {
    return Aggregation.Count.create();
  }

  @Override
  public List<TagKey> getColumns() {
    return columns;
  }
}
