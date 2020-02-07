package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableList;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureLong;
import java.util.Collections;
import java.util.List;
import org.pmiops.workbench.monitoring.labels.MetricLabel;

/** Metric enum values for events to be counted. */
public enum EventMetric implements Metric {
  CRON_JOB_END(
      "cron_job_complete",
      "Cron job finishes",
      ImmutableList.of(MetricLabel.CRON_JOB_SUCCEEDED, MetricLabel.CRON_JOB_NAME)),
  NOTEBOOK_CLONE("notebook_clone_2", "Clone (duplicate) a notebook"),
  NOTEBOOK_DELETE("notebook_delete_2", "Delete a notebook"),
  NOTEBOOK_SAVE("notebook_save_2", "Save (or create) a notebook");

  private final String name;
  private final String description;
  private final List<MetricLabel> labels;

  CumulativeMetric(String name, String description) {
    this(name, description, Collections.emptyList());
  }

  CumulativeMetric(String name, String description, List<MetricLabel> labels) {
    this.name = name;
    this.description = description;
    this.labels = labels;
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
    return UnitOfMeasure.COUNT.getUcmSymbol();
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
  public List<MetricLabel> getLabels() {
    return labels;
  }
}
