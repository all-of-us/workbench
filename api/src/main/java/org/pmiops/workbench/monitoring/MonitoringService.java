package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.MetricBase;

public interface MonitoringService {

  int DELTA_VALUE = 1;
  Map<TagKey, TagValue> DEFAULT_TAGS = Collections.emptyMap();

  /**
   * Record an occurrence of a counted (a.k.a. delta or cumulative) time series. These are events
   * that are typically measured more for frequency than for absolute value.
   *
   * @param eventMetric
   */
  default void recordEvent(EventMetric eventMetric) {
    recordValue(eventMetric, DELTA_VALUE);
  }

  default void recordEvent(EventMetric eventMetric, Map<TagKey, TagValue> tags) {
    recordValues(ImmutableMap.of(eventMetric, DELTA_VALUE), tags);
  }

  default void recordValue(MetricBase metric, Number value) {
    recordValues(ImmutableMap.of(metric, value));
  }

  default void recordValues(Map<MetricBase, Number> metricToValue) {
    recordValues(metricToValue, DEFAULT_TAGS);
  }

  /**
   * Record multiple values at once. An attachment map allows associating these measurements with
   * metadata (shared across all samples). We use a single MeasureMap for all the entries in both
   * maps.
   *
   * @param metricToValue key/value pairs for time series. These need not be related, but any
   *     attachments should apply to all entries in this map.
   * @param tags Map of String/AttachmentValue pairs to be associated with these data.
   */
  void recordValues(Map<MetricBase, Number> metricToValue, Map<TagKey, TagValue> tags);

  default void recordBundle(MeasurementBundle measurementBundle) {
    recordValues(measurementBundle.getMeasurements(), measurementBundle.getTags());
  }

  // Record each ViewBundle object separately, so that we don't
  // mix attachments across MeasureMaps.
  default void recordBundles(Collection<MeasurementBundle> viewBundles) {
    viewBundles.forEach(this::recordBundle);
  }
}
