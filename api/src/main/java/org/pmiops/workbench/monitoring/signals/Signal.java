package org.pmiops.workbench.monitoring.signals;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;

public interface Signal {
  String getName();
  String getDescription();
  Measure getMeasure();
  Measure.MeasureLong getMeasureLong();
  Measure.MeasureDouble getMeasureDouble();
  Class getMeasureClass();
  Aggregation getAggregation();
}
