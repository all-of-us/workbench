package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableMap;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.Metric;

/**
 * Simple bundle class to avoid dealing with lists of pairs of maps. This situation can occur in
 * classes that have multiple kinds of signals with different attribute maps among subsets. In order
 * to avoid excessive proliferation of (confusing) argument list types, we try to consolidate here.
 */
public class MeasurementBundle {
  private final Map<Metric, Number> measurements;
  private final Map<MetricLabel, TagValue> tags;

  private MeasurementBundle(Map<Metric, Number> measurements, Map<MetricLabel, TagValue> tags) {
    this.measurements = measurements;
    this.tags = tags;
  }

  public Map<Metric, Number> getMeasurements() {
    return measurements;
  }

  public Map<TagKey, TagValue> getTags() {
    return tags.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(e -> TagKey.create(e.getKey().getName()), Entry::getValue));
  }

  public Optional<String> getTagValue(MetricLabel metricLabel) {
    return Optional.ofNullable(tags.get(metricLabel))
        .map(TagValue::asString);
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
    MeasurementBundle bundle = (MeasurementBundle) o;
    return Objects.equals(measurements, bundle.measurements) && Objects.equals(tags, bundle.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(measurements, tags);
  }

  @Override
  public String toString() {
    return String.format(
        "{measurements: [%s] tags: [%s]}",
        getMeasurements().entrySet().stream()
            .map(e -> String.format("%s: %s", e.getKey().getName(), e.getValue()))
            .collect(Collectors.joining(", ")),
        getTags().entrySet().stream()
            .map(e -> String.format("%s: %s", e.getKey().getName(), e.getValue().asString()))
            .collect(Collectors.joining(", ")));
  }

  public static class Builder {
    private ImmutableMap.Builder<Metric, Number> measurementsBuilder;
    private ImmutableMap.Builder<MetricLabel, TagValue> tagsBuilder;

    private Builder() {
      measurementsBuilder = ImmutableMap.builder();
      tagsBuilder = ImmutableMap.builder();
    }

    public Builder addEvent(Metric metric) {
      measurementsBuilder.put(metric, MonitoringService.COUNT_INCREMENT);
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

    public Builder addTag(MetricLabel metricLabel, String value) {
      tagsBuilder.put(metricLabel, TagValue.create(value));
      return this;
    }

    public Builder addAllTags(Map<MetricLabel, String> attachmentKeyToString) {
      tagsBuilder.putAll(
          attachmentKeyToString.entrySet().stream()
              .collect(
                  ImmutableMap.toImmutableMap(Entry::getKey, e -> TagValue.create(e.getValue()))));
      return this;
    }

    public MeasurementBundle build() {
      validate();
      return new MeasurementBundle(measurementsBuilder.build(), tagsBuilder.build());
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
      ImmutableMap<MetricLabel, TagValue> tags = tagsBuilder.build();
      if (measurements.isEmpty()) {
        throw new IllegalStateException("MeasurementBundle must have at least one measurement.");
      }
      // Validate each attachment supports all metrics
      for (Map.Entry<MetricLabel, TagValue> entry : tags.entrySet()) {
        final MetricLabel attachment = entry.getKey();
        final TagValue attachmentValue = entry.getValue();

        for (Metric metric : measurements.keySet()) {
          if (!metric.supportsLabel(attachment)) {
            throw new IllegalStateException(
                (String.format(
                    "Metric %s does not support Attachment %s",
                    metric.getName(), attachment.getName())));
          }
        }
        if (!entry.getKey().supportsDiscreteValue(attachmentValue.asString())) {
          throw new IllegalStateException(
              String.format(
                  "Attachment %s does not support provided value %s",
                  attachment.getName(), attachmentValue));
        }
      }
    }
  }
}
