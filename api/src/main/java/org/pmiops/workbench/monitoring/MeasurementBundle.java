package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import io.opencensus.metrics.data.AttachmentValue.AttachmentValueString;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.pmiops.workbench.monitoring.attachments.AttachmentKey;
import org.pmiops.workbench.monitoring.views.OpenCensusView;

/**
 * Simple bundle class to avoid dealing with lists of pairs of maps. This situation can occur in
 * classes that have multiple kinds of signals with different attribute maps among subsets. In order
 * to avoid excessive proliferation of (confusing) argument list types, we try to consolidate here.
 */
public class MeasurementBundle {
  private final Map<OpenCensusView, Number> monitoringViews;
  private final Map<AttachmentKey, String> attachmentKeyToString;

  private MeasurementBundle(
      Map<OpenCensusView, Number> monitoringViews,
      Map<AttachmentKey, String> attachmentKeyToString) {
    this.monitoringViews = monitoringViews;
    this.attachmentKeyToString = attachmentKeyToString;
  }

  public Map<OpenCensusView, Number> getMeasurements() {
    return monitoringViews;
  }

  public Map<String, AttachmentValue> getAttachments() {
    return fromAttachmentKeyToString(attachmentKeyToString);
  }

  // In our system, it's more convenient to work with the AttachmentKey
  // enum directly and String values. For some reason we have to wrap strings
  // in AttachmentValueStrings now (though naked Strings are still supported but
  // deprecated).
  private static Map<String, AttachmentValue> fromAttachmentKeyToString(
      Map<AttachmentKey, String> attachmentKeyStringMap) {
    return attachmentKeyStringMap.entrySet().stream()
        .map(
            e ->
                new SimpleImmutableEntry<String, AttachmentValue>(
                    e.getKey().getKeyName(), AttachmentValueString.create(e.getValue())))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, SimpleImmutableEntry::getValue));
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeasurementBundle)) {
      return false;
    }
    MeasurementBundle that = (MeasurementBundle) o;
    return monitoringViews.equals(that.monitoringViews)
        && attachmentKeyToString.equals(that.attachmentKeyToString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(monitoringViews, attachmentKeyToString);
  }

  @Override
  public String toString() {
    return "MeasurementBundle{"
        + "monitoringViews="
        + monitoringViews
        + ", attachments="
        + attachmentKeyToString
        + '}';
  }

  public static class Builder {
    private ImmutableMap.Builder<OpenCensusView, Number> measurementsBuilder;
    private ImmutableMap.Builder<AttachmentKey, String> attachmentsBuilder;

    private Builder() {
      measurementsBuilder = ImmutableMap.builder();
      attachmentsBuilder = ImmutableMap.builder();
    }

    public Builder add(OpenCensusView viewInfo) {
      measurementsBuilder.put(viewInfo, MonitoringService.DELTA_VALUE);
      return this;
    }

    public Builder add(OpenCensusView viewInfo, Number value) {
      measurementsBuilder.put(viewInfo, value);
      return this;
    }

    public Builder addAll(Map<OpenCensusView, Number> map) {
      measurementsBuilder.putAll(ImmutableMap.copyOf(map));
      return this;
    }

    public Builder attach(AttachmentKey attachmentKey, String value) {
      attachmentsBuilder.put(attachmentKey, value);
      return this;
    }

    public Builder attachAll(Map<AttachmentKey, String> attachmentKeyToString) {
      attachmentsBuilder.putAll(ImmutableMap.copyOf(attachmentKeyToString));
      return this;
    }

    public MeasurementBundle build() {
      if (measurementsBuilder.build().isEmpty()) {
        throw new IllegalStateException("MeasurementBundle must have at least one measurement.");
      }
      return new MeasurementBundle(measurementsBuilder.build(), attachmentsBuilder.build());
    }
  }
}
