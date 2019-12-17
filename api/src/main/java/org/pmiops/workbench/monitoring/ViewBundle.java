package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import io.opencensus.metrics.data.AttachmentValue.AttachmentValueString;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.pmiops.workbench.monitoring.attachments.AttachmentKey;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;

/**
 * Simple bundle class to avoid dealing with lists of pairs of maps. This
 * situation can occur in classes that have multiple kinds of signals with different attribute
 * maps among subsets. In order to avoid excessive proliferation of (confusing) argument list types,
 * we try to consolidate here.
 */
public class ViewBundle {
  private final Map<OpenCensusStatsViewInfo, Number> monitoringViews;
  private final Map<AttachmentKey, String> attachmentKeyToString;

  private ViewBundle(
      Map<OpenCensusStatsViewInfo, Number> monitoringViews,
      Map<AttachmentKey, String> attachmentKeyToString) {
    this.monitoringViews = monitoringViews;
    this.attachmentKeyToString = attachmentKeyToString;
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
    return fromAttachmentKeyToString(attachmentKeyToString);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ViewBundle)) {
      return false;
    }
    ViewBundle that = (ViewBundle) o;
    return monitoringViews.equals(that.monitoringViews) &&
        attachmentKeyToString.equals(that.attachmentKeyToString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(monitoringViews, attachmentKeyToString);
  }

  @Override
  public String toString() {
    return "ViewBundle{" +
        "monitoringViews=" + monitoringViews +
        ", attachmentKeyToString=" + attachmentKeyToString +
        '}';
  }

  public static class Builder {
    private ImmutableMap.Builder<OpenCensusStatsViewInfo, Number> viewToValueBuilder;
    private ImmutableMap.Builder<AttachmentKey, String> attachmentKeyToValueBuilder;

    private Builder() {
      viewToValueBuilder = ImmutableMap.builder();
      attachmentKeyToValueBuilder = ImmutableMap.builder();
    }

    public Builder addViewInfoDelta(OpenCensusStatsViewInfo viewInfo) {
      viewToValueBuilder.put(viewInfo, MonitoringService.DELTA_VALUE);
      return this;
    }

    public Builder addViewInfoValuePair(OpenCensusStatsViewInfo viewInfo, Number value) {
      viewToValueBuilder.put(viewInfo, value);
      return this;
    }

    public Builder addViewInfoToValueMap(Map<OpenCensusStatsViewInfo, Number> map) {
      viewToValueBuilder.putAll(ImmutableMap.copyOf(map));
      return this;
    }

    public Builder addAttachmentKeyValuePair(AttachmentKey attachmentKey, String value) {
      attachmentKeyToValueBuilder.put(attachmentKey, value);
      return this;
    }

    public Builder addAttachmentKeyValueMap(Map<AttachmentKey, String> attachmentKeyToString) {
      attachmentKeyToValueBuilder.putAll(ImmutableMap.copyOf(attachmentKeyToString));
      return this;
    }

    public ViewBundle build() {
      return new ViewBundle(viewToValueBuilder.build(), attachmentKeyToValueBuilder.build());
    }
  }
}
