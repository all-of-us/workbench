package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableSet;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.tags.TagKey;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.pmiops.workbench.monitoring.attachments.Attachment;

public enum GaugeMetric implements Metric {
  BILLING_BUFFER_PROJECT_COUNT(
      "billing_buffer_project_count",
      "Number of projects in the billing buffer for each status",
      ImmutableSet.of(Attachment.BUFFER_ENTRY_STATUS)),
  COHORT_COUNT("cohort_count", "Count of all cohorts in existence"),
  COHORT_REVIEW_COUNT("cohort_review_count", "Total number of cohort reviews in existence"),
  DATASET_COUNT(
      "dataset_count",
      "Count of all datasets in existence",
      Collections.singleton(Attachment.DATASET_INVALID)),
  USER_COUNT(
      "user_count",
      "total number of users",
      ImmutableSet.of(
          Attachment.USER_BYPASSED_BETA, Attachment.USER_DISABLED, Attachment.DATA_ACCESS_LEVEL)),
  WORKSPACE_COUNT(
      "workspace_count",
      "Count of all workspaces",
      ImmutableSet.of(Attachment.WORKSPACE_ACTIVE_STATUS, Attachment.DATA_ACCESS_LEVEL));

  private final String name;
  private final String description;
  private final String unit;
  private final Aggregation aggregation;
  private List<TagKey> columns;
  private final Class measureClass;
  private final Set<Attachment> allowedAttachments;

  GaugeMetric(String name, String description) {
    this(name, description, Collections.emptySet(), Metric.UNITLESS_UNIT, MeasureLong.class);
  }

  GaugeMetric(String name, String description, Set<Attachment> allowedAttachments) {
    this(name, description, allowedAttachments, Metric.UNITLESS_UNIT, MeasureLong.class);
  }

  GaugeMetric(
      String name,
      String description,
      Set<Attachment> allowedAttachments,
      String unit,
      Class measureClass) {
    this(name, description, allowedAttachments, unit, measureClass, Aggregation.LastValue.create());
  }

  GaugeMetric(
      String name,
      String description,
      Set<Attachment> allowedAttachments,
      String unit,
      Class measureClass,
      Aggregation aggregation) {
    this(
        name,
        description,
        allowedAttachments,
        unit,
        measureClass,
        aggregation,
        Collections.emptyList());
  }

  GaugeMetric(
      String name,
      String description,
      Set<Attachment> allowedAttachments,
      String unit,
      Class measureClass,
      Aggregation aggregation,
      List<TagKey> columns) {
    this.name = name;
    this.description = description;
    this.allowedAttachments = allowedAttachments;
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
  public Set<Attachment> getSupportedAttachments() {
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
        + columns
        + ", measureClass="
        + measureClass
        + '}';
  }
}
