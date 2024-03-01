package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.Metric;

public interface MonitoringService {

  int COUNT_INCREMENT = 1;

  /**
   * Record an occurrence of a counted (a.k.a. delta or cumulative) time series. These are events
   * that are typically measured more for frequency than for absolute value.
   *
   * @param eventMetric
   */
  default void recordEvent(EventMetric eventMetric) {
    recordValue(eventMetric, COUNT_INCREMENT);
  }

  default void recordValue(Metric metric, Number value) {
    recordValues(ImmutableMap.of(metric, value));
  }

  default void recordValues(Map<Metric, Number> metricToValue) {
    recordValues(metricToValue, Collections.emptyMap());
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
  void recordValues(Map<Metric, Number> metricToValue, Map<TagKey, TagValue> tags);

  default void recordBundle(MeasurementBundle measurementBundle) {
    recordValues(measurementBundle.getMeasurements(), measurementBundle.getTags());
  }

  // Record each ViewBundle object separately, so that we don't
  // mix attachments across MeasureMaps.
  default void recordBundles(Collection<MeasurementBundle> viewBundles) {
    viewBundles.forEach(this::recordBundle);
  }
}
