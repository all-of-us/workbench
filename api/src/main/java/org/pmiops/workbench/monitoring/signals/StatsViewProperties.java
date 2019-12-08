package org.pmiops.workbench.monitoring.signals;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.View;
import io.opencensus.stats.View.Name;
import io.opencensus.tags.TagKey;
import java.util.List;

/**
 * This is essentially a carbon copy of io.opencensus.stats.View, but written as an interface
 * intstead of an abstract class. This allows us to implement it with enums, which gives an orderly
 * way to describe lots of views without cluttering up the call sites.
 */
public interface StatsViewProperties {
  String getName();

  default Name getStatsName() {
    return Name.create(getName());
  }

  String getDescription();

  Measure getMeasure();

  Measure.MeasureLong getMeasureLong();

  Measure.MeasureDouble getMeasureDouble();

  Class getMeasureClass();

  Aggregation getAggregation();

  List<TagKey> getColumns();

  default View toStatsView() {
    return View.create(
        getStatsName(), getDescription(), getMeasure(), getAggregation(), getColumns());
  }
}
