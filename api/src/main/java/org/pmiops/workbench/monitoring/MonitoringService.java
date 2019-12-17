package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;

public interface MonitoringService {

  int DELTA_VALUE = 1;
  Map<String, AttachmentValue> DEFAULT_ATTACHMENTS = Collections.emptyMap();

  /**
   * Record an occurrence of a counted (a.k.a. delta or cumulative) time series.
   * These are events that are typically measured more for frequency than for absolute
   * value.
   * @param viewInfo
   */
  default void recordDelta(OpenCensusStatsViewInfo viewInfo) {
    recordValue(viewInfo, DELTA_VALUE);
  }

  default void recordDelta(OpenCensusStatsViewInfo viewInfo, Map<String, AttachmentValue> attachments) {
    recordValues(ImmutableMap.of(viewInfo, DELTA_VALUE), attachments);
  }

  default void recordValue(OpenCensusStatsViewInfo viewInfo, Number value) {
    recordValues(ImmutableMap.of(viewInfo, value));
  }

  default void recordValues(Map<OpenCensusStatsViewInfo, Number> viewInfoToValue) {
    recordValues(viewInfoToValue, DEFAULT_ATTACHMENTS);
  }

  void recordValues(Map<OpenCensusStatsViewInfo, Number> viewInfoToValue,
      Map<String, AttachmentValue> attachmentKeyToValue);

  default void recordBundle(ViewBundle viewBundle) {
    recordValues(viewBundle.getMonitoringViews(), viewBundle.getAttachments());
  }

  // Record each ViewBundle object separately, so that we don't
  // mix attachments across MeasureMaps.
  default void recordBundles(List<ViewBundle> viewBundles) {
    viewBundles.forEach(this::recordBundle);
  }
}
