package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.metrics.data.AttachmentValue;
import io.opencensus.metrics.data.AttachmentValue.AttachmentValueString;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.pmiops.workbench.monitoring.attachments.Attachment;
import org.pmiops.workbench.monitoring.views.Metric;

/**
 * Simple bundle class to avoid dealing with lists of pairs of maps. This situation can occur in
 * classes that have multiple kinds of signals with different attribute maps among subsets. In order
 * to avoid excessive proliferation of (confusing) argument list types, we try to consolidate here.
 */
public class MeasurementBundle {
  private final Map<Metric, Number> measurements;
  private final Map<Attachment, String> attachments;

  private MeasurementBundle(Map<Metric, Number> measurements, Map<Attachment, String> attachments) {
    this.measurements = measurements;
    this.attachments = attachments;
  }

  public Map<Metric, Number> getMeasurements() {
    return measurements;
  }

  public Map<String, AttachmentValue> getAttachments() {
    return fromAttachmentKeyToString(attachments);
  }

  // In our system, it's more convenient to work with the AttachmentKey
  // enum directly and String values. For some reason we have to wrap strings
  // in AttachmentValueStrings now (though naked Strings are still supported but
  // deprecated).
  private static Map<String, AttachmentValue> fromAttachmentKeyToString(
      Map<Attachment, String> attachmentKeyStringMap) {
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

  public static MeasurementBundle ofDelta(Metric view) {
    return builder().addEvent(view).build();
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
    return measurements.equals(that.measurements) && attachments.equals(that.attachments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(measurements, attachments);
  }

  @Override
  public String toString() {
    return String.format(
        "measurements: [%s] attachments: [%s]",
        getMeasurements().entrySet().stream()
            .map(e -> String.format("%s: %s", e.getKey().getName(), e.getValue()))
            .collect(Collectors.joining(", ")),
        getAttachments().entrySet().stream()
            .map(e -> String.format("%s: %s", e.getKey(), e.getValue().getValue()))
            .collect(Collectors.joining(", ")));
  }

  public static class Builder {
    private ImmutableMap.Builder<Metric, Number> measurementsBuilder;
    private ImmutableMap.Builder<Attachment, String> attachmentsBuilder;

    private Builder() {
      measurementsBuilder = ImmutableMap.builder();
      attachmentsBuilder = ImmutableMap.builder();
    }

    public Builder addEvent(Metric viewInfo) {
      measurementsBuilder.put(viewInfo, MonitoringService.DELTA_VALUE);
      return this;
    }

    public Builder addMeasurement(Metric metric, Number value) {
      measurementsBuilder.put(metric, value);
      return this;
    }

    public Builder addAllMeasurements(Map<Metric, Number> map) {
      measurementsBuilder.putAll(ImmutableMap.copyOf(map));
      return this;
    }

    public Builder addAttachment(Attachment attachmentKey, String value) {
      attachmentsBuilder.put(attachmentKey, value);
      return this;
    }

    public Builder addAllAttachments(Map<Attachment, String> attachmentKeyToString) {
      attachmentsBuilder.putAll(ImmutableMap.copyOf(attachmentKeyToString));
      return this;
    }

    public MeasurementBundle build() {
      validate();
      return new MeasurementBundle(measurementsBuilder.build(), attachmentsBuilder.build());
    }

    /**
     * Validate the MeasurementBundle Builder inputs here, throwing IllegalStateException if not
     * valid. Rules are: 1. Bundle must have at least one measurement 2. Any attachment provided
     * must support all metrics provided in the masurements map, as determined by
     * Attachment#supportsMetric() 3. Additionally, all attachment values must be supported by their
     * attachments, according to Attachment#supportsDiscreteValue()
     */
    private void validate() {
      ImmutableMap<Metric, Number> measurements = measurementsBuilder.build();
      ImmutableMap<Attachment, String> attachments = attachmentsBuilder.build();
      if (measurements.isEmpty()) {
        throw new IllegalStateException("MeasurementBundle must have at least one measurement.");
      }
      // Validate each attachment supports all metrics
      for (Map.Entry<Attachment, String> entry : attachments.entrySet()) {
        final Attachment attachment = entry.getKey();
        final String attachmentValue = entry.getValue();

        for (Metric metric : measurements.keySet()) {
          if (!attachment.supportsMetric(metric)) {
            throw new IllegalStateException(
                (String.format(
                    "Attachment %s does not support metric %s",
                    attachment.getKeyName(), metric.getName())));
          }
        }
        if (!entry.getKey().supportsDiscreteValue(attachmentValue)) {
          throw new IllegalStateException(
              String.format(
                  "Attachment %s does not support provided value %s",
                  attachment.getKeyName(), attachmentValue));
        }
      }
    }
  }
}
