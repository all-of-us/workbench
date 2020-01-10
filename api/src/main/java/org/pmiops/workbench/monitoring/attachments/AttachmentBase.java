package org.pmiops.workbench.monitoring.attachments;

import java.util.Set;
import org.pmiops.workbench.monitoring.views.Metric;

/**
 * Top-level interface for dealing with attachments. Currently the only supported feature is
 * attachment key name. Future capabilities might include listing metrics for which the keys are
 * applicable, or defining a discrete range of values that can be attached with a given key.
 */
public interface AttachmentBase {
  String getKeyName();

  /**
   * In some cases, we may want key names to apply to only certain Metrics. If this set is empty, we
   * support all of them, otherwise, just the listed ones. This is enforced in the MeasurementBundle
   * validator.
   *
   * @return
   */
  Set<Metric> getAllowedMetrics();

  /**
   * Return true if this AttachmentKeyBase supports the given Metric.
   *
   * @param metric
   * @return
   */
  default boolean supportsMetric(Metric metric) {
    return getAllowedMetrics().isEmpty() || getAllowedMetrics().contains(metric);
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
