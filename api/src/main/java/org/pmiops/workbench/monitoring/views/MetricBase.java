package org.pmiops.workbench.monitoring.views;

import com.google.api.MetricDescriptor.MetricKind;
import com.google.common.collect.ImmutableMap;
import com.google.monitoring.v3.TypedValue;
import com.google.monitoring.v3.TypedValue.Builder;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.View;
import io.opencensus.stats.View.Name;
import io.opencensus.tags.TagKey;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;
import org.pmiops.workbench.monitoring.attachments.MetricLabelBase;

/**
 * This is essentially a carbon copy of io.opencensus.stats.View, but written as an interface
 * intstead of an abstract class. This allows us to implement it with enums, which gives an orderly
 * way to describe lots of views without cluttering up the call sites.
 *
 * <p>Note that Stackdriver Monitoring uses different nomenclature from OpenCensus. In particular,
 * there's no such thing as a Metric in the latter. We use OpenCensus terminology in this system as
 * we may wish to support other metrics backends, and don't want to depend on Stackdriver concepts
 * or implementation details.
 *
 * <p>Currenlty, there is a mix of both Stackdriver and OpenCensus types referenced in this
 * interface. The plan is to avoid that by moving these backend-specific functions into translator
 * classes/services.
 */
public interface MetricBase {

  Map<Class, Function<MetricBase, Measure>> MEASURE_CLASS_TO_MEASURE_FUNCTION =
      ImmutableMap.of(
          MeasureLong.class, MetricBase::getMeasureLong,
          MeasureDouble.class, MetricBase::getMeasureDouble);

  String STACKDRIVER_CUSTOM_METRICS_PREFIX = "custom.googleapis.com/";

  String getName();

  default Name getStatsName() {
    return Name.create(getName());
  }

  /**
   * Return
   *
   * @return
   */
  default String getMetricPathName() {
    return String.format("%s%s", STACKDRIVER_CUSTOM_METRICS_PREFIX, getName());
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

  /**
   * Distinguish between Long and Double measures via MeasureLong and MeasureDouble return values
   *
   * @return Measure.MeasureLong, Measure.MeasureDouble, or another (future) class
   */
  Class getMeasureClass();

  Aggregation getAggregation();

  List<MetricLabel> getLabels();

  default List<TagKey> getColumns() {
    return getLabels().stream().map(MetricLabelBase::getTagKey).collect(Collectors.toList());
  }

  default View toView() {
    return View.create(
        getStatsName(), getDescription(), getMeasure(), getAggregation(), getColumns());
  }

  default boolean supportsLabel(MetricLabel label) {
    return getLabels().contains(label);
  }

  MetricKind getMetricKind();

//  BiConsumer<Builder, ? extends Number> getTypedValueSetter();
}
