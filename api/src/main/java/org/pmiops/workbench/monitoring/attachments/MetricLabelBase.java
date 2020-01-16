package org.pmiops.workbench.monitoring.attachments;

import io.opencensus.tags.TagKey;
import java.util.Set;

/**
 * Top-level interface for dealing with attachments. Currently the only supported feature is
 * attachment key name. Future capabilities might include listing metrics for which the keys are
 * applicable, or defining a discrete range of values that can be attached with a given key.
 */
public interface MetricLabelBase {
  String getName();

  default TagKey getTagKey() {
    return TagKey.create(getName());
  }

  /**
   * For attachments whose values are in a discrete range (such as booleans, enums, or cloud
   * regions), we can validate the values here before sending to the monitoring backend.
   *
   * <p>Leave this set empty to allow all valuesl
   *
   * @return
   */
  Set<String> getAllowedDiscreteValues();

  default boolean supportsDiscreteValue(String value) {
    return getAllowedDiscreteValues().isEmpty() || getAllowedDiscreteValues().contains(value);
  }
}
