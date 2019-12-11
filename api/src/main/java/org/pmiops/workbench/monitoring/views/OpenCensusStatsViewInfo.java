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
 *
 * Note that Stackdriver Monitoring uses different nomenclature from OpenCensus. In particular,
 * there's no such thing as a Metric in the latter. We use OpenCensus terminology in this system
 * as we may wish to support other metrics backends, and don't want to depend on Stackdriver
 * concepts or implementation details.
 */
public interface OpenCensusStatsViewInfo {

  Map<Class, Function<OpenCensusStatsViewInfo, Measure>> MEASURE_CLASS_TO_MEASURE_FUNCTION =
      ImmutableMap.of(
          MeasureLong.class, OpenCensusStatsViewInfo::getMeasureLong,
          MeasureDouble.class, OpenCensusStatsViewInfo::getMeasureDouble);

  String UNITLESS_UNIT = "1";

  String getName();

  default Name getStatsName() {
    return Name.create(getName());
  }

  String getDescription();

  /**
   * Unit strings must conform to the
   *
   * @see <a href=https://unitsofmeasure.org/ucum.html>Unified Code for Units of Measure</a>
   * @return canonical string for unit
   */
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

  default View toOpenCensusView() {
    return View.create(
        getStatsName(), getDescription(), getMeasure(), getAggregation(), getColumns());
  }
}
