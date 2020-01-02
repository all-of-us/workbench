package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.pmiops.workbench.monitoring.views.OpenCensusView;

public interface MonitoringService {

  int DELTA_VALUE = 1;
  Map<String, AttachmentValue> DEFAULT_ATTACHMENTS = Collections.emptyMap();

  /**
   * Record an occurrence of a counted (a.k.a. delta or cumulative) time series. These are events
   * that are typically measured more for frequency than for absolute value.
   *
   * @param viewInfo
   */
  default void recordEvent(OpenCensusView viewInfo) {
    recordValue(viewInfo, DELTA_VALUE);
  }

  default void recordEvent(OpenCensusView viewInfo, Map<String, AttachmentValue> attachments) {
    recordValues(ImmutableMap.of(viewInfo, DELTA_VALUE), attachments);
  }

  default void recordValue(OpenCensusView viewInfo, Number value) {
    recordValues(ImmutableMap.of(viewInfo, value));
  }

  default void recordValues(Map<OpenCensusView, Number> viewInfoToValue) {
    recordValues(viewInfoToValue, DEFAULT_ATTACHMENTS);
  }

  void recordValues(
      Map<OpenCensusView, Number> viewInfoToValue,
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
