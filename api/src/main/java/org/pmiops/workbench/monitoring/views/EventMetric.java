package org.pmiops.workbench.monitoring.views;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.pmiops.workbench.monitoring.attachments.Attachment;

/** Metric enum values for events to be counted. */
public enum EventMetric implements Metric {
  NOTEBOOK_CLONE("notebook_clone", "Clone (duplicate) a notebook", Collections.emptySet()),
  NOTEBOOK_DELETE("notebook_delete", "Delete a notebook", Collections.emptySet()),
  NOTEBOOK_SAVE("notebook_save", "Save (or create) a notebook", Collections.emptySet());

  private final String name;
  private final String description;
  private final Set<Attachment> allowedAttachments;
  private List<TagKey> columns;

  EventMetric(
      String name, String description, Set<Attachment> allowedAttachments, List<TagKey> columns) {
    this.name = name;
    this.description = description;
    this.allowedAttachments = allowedAttachments;
    this.columns = columns;
  }

  EventMetric(String name, String description, Set<Attachment> allowedAttachments) {
    this(name, description, allowedAttachments, Collections.emptyList());
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

  @Override
  public Set<Attachment> getAllowedAttachments() {
    return allowedAttachments;
  }
}
