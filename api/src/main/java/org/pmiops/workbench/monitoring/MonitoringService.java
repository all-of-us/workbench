package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.Metric;

public interface MonitoringService {

  int DELTA_VALUE = 1;
  Map<String, AttachmentValue> DEFAULT_ATTACHMENTS = Collections.emptyMap();

  /**
   * Record an occurrence of a counted (a.k.a. delta or cumulative) time series. These are events
   * that are typically measured more for frequency than for absolute value.
   *
   * @param eventMetric
   */
  default void recordEvent(EventMetric eventMetric) {
    recordValue(eventMetric, DELTA_VALUE);
  }

  default void recordEvent(EventMetric eventMetric, Map<String, AttachmentValue> attachments) {
    recordValues(ImmutableMap.of(eventMetric, DELTA_VALUE), attachments);
  }

  default void recordValue(Metric viewInfo, Number value) {
    recordValues(ImmutableMap.of(viewInfo, value));
  }

  default void recordValues(Map<Metric, Number> viewInfoToValue) {
    recordValues(viewInfoToValue, DEFAULT_ATTACHMENTS);
  }

  /**
   * Record multiple values at once. An attachment map allows associating these measurements with
   * metadata (shared across all samples). We use a single MeasureMap for all the entries in both
   * maps.
   *
   * @param viewInfoToValue key/value pairs for time series. These need not be related, but any
   *     attachments should apply to all entries in this map.
   * @param attachmentKeyToValue Map of String/AttachmentValue pairs to be associated with these
   *     data.
   */
  void recordValues(
      Map<Metric, Number> viewInfoToValue,
      Map<String, AttachmentValue> attachmentKeyToValue);

  default void recordBundle(MeasurementBundle viewBundle) {
    recordValues(viewBundle.getMeasurements(), viewBundle.getAttachments());
  }

  // Record each ViewBundle object separately, so that we don't
  // mix attachments across MeasureMaps.
  default void recordBundles(Collection<MeasurementBundle> viewBundles) {
    viewBundles.forEach(this::recordBundle);
  }
}
