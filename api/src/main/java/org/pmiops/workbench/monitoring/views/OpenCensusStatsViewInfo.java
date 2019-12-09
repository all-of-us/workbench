package org.pmiops.workbench.monitoring.views;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.View;
import io.opencensus.stats.View.Name;
import io.opencensus.tags.TagKey;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This is essentially a carbon copy of io.opencensus.stats.View, but written as an interface
 * intstead of an abstract class. This allows us to implement it with enums, which gives an orderly
 * way to describe lots of views without cluttering up the call sites.
 */
public interface OpenCensusStatsViewInfo {

  Map<Class, Function<OpenCensusStatsViewInfo, Measure>> MEASURE_CLASS_TO_MEASURE_FUNCTION =
      ImmutableMap.of(
          MeasureLong.class, OpenCensusStatsViewInfo::getMeasureLong,
          MeasureDouble.class, OpenCensusStatsViewInfo::getMeasureDouble);

  String SCALAR_UNIT = "";

  String getName();

  default Name getStatsName() {
    return Name.create(getName());
  }

  String getDescription();

  String getUnit();

  default Measure getMeasure() {
    return MEASURE_CLASS_TO_MEASURE_FUNCTION.get(getMeasureClass()).apply(this);
  }

  default MeasureLong getMeasureLong() {
    return MeasureLong.create(getName(), getDescription(), getUnit());
  }

  default MeasureDouble getMeasureDouble() {
    return MeasureDouble.create(getName(), getDescription(), getUnit());
  }

  Class getMeasureClass();

  Aggregation getAggregation();

  List<TagKey> getColumns();

  default View toStatsView() {
    return View.create(
        getStatsName(), getDescription(), getMeasure(), getAggregation(), getColumns());
  }
}
