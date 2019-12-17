package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import io.opencensus.metrics.data.AttachmentValue.AttachmentValueString;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import org.pmiops.workbench.monitoring.attachments.AttachmentKey;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;

/**
 * Simple bundle class to avoid dealing with lists of pairs of maps. This
 * situation can occur in classes that have multiple kinds of signals with different attribute
 * maps among subsets. In order to avoid excessive proliferation of (confusing) argument list types,
 * we try to consolidate here.
 */
public abstract class ViewBundle {
  private final Map<OpenCensusStatsViewInfo, Number> monitoringViews;
  private final Map<AttachmentKey, String> attchmentKeyToString;

  public ViewBundle(
      Map<OpenCensusStatsViewInfo, Number> monitoringViews,
      Map<AttachmentKey, String> attchmentKeyToString) {
    this.monitoringViews = monitoringViews;
    this.attchmentKeyToString = attchmentKeyToString;
  }

  public static Map<String, AttachmentValue> fromAttachmentKeyToString(Map<AttachmentKey, String> attachmentKeyStringMap) {
    return attachmentKeyStringMap.entrySet().stream()
        .map(e -> new SimpleImmutableEntry<String, AttachmentValue>(e.getKey().getKeyName(), AttachmentValueString
            .create(e.getValue())))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, SimpleImmutableEntry::getValue));
  }

  public Map<OpenCensusStatsViewInfo, Number> getMonitoringViews() {
    return monitoringViews;
  }

  public Map<String, AttachmentValue> getAttachments() {
    return fromAttachmentKeyToString(attchmentKeyToString);
  }

}
