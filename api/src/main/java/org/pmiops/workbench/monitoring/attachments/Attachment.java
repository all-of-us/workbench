package org.pmiops.workbench.monitoring.attachments;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.monitoring.views.Metric;
import org.pmiops.workbench.utils.Booleans;
import org.pmiops.workbench.utils.Enums;

public enum Attachment implements AttachmentBase {
  BUFFER_ENTRY_STATUS("buffer_entry_status",
      ImmutableSet.of(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT),
      Enums.getValueStrings(BufferEntryStatus.class)),
  DATASET_INVALID("dataset_invalid",
      ImmutableSet.of(GaugeMetric.DATASET_COUNT),
      Booleans.getValueStrings()),
  USER_BYPASSED_BETA("user_bypassed_beta",
      ImmutableSet.of(GaugeMetric.USER_COUNT),
      Booleans.getValueStrings()),
  USER_DISABLED("user_disabled",
      Collections.singleton(GaugeMetric.USER_COUNT),
      Booleans.getValueStrings()),
  USER_DATA_ACCESS_LEVEL("user_data_access_level",
      Collections.singleton(GaugeMetric.USER_COUNT),
      Enums.getValueStrings(DataAccessLevel.class)),
  WORKSPACE_ACTIVE_STATUS("workspace_active_status"),
  WORKSPACE_DATA_ACCESS_LEVEL("workspace_data_access_level");

  private String keyName;
  private Set<Metric> allowedMetrics;
  private Set<String> allowedDiscreteValues;

  Attachment(String keyName) {
    this(keyName, Collections.emptySet());
  }

  Attachment(String keyName, Set<Metric> allowedMetrics) {
    this(keyName, allowedMetrics, Collections.emptySet());
  }

  Attachment(String keyName, Set<Metric> allowedMetrics, Set<String> allowedDiscreteValues) {
    this.keyName = keyName;
    this.allowedMetrics = ImmutableSet.copyOf(allowedMetrics);
    this.allowedDiscreteValues = allowedDiscreteValues;
  }

  @Override
  public String getKeyName() {
    return keyName;
  }

  @Override
  public Set<Metric> getAllowedMetrics() {
    return allowedMetrics;
  }

  @Override
  public Set<String> getAllowedDiscreteValues() {
    return allowedDiscreteValues;
  }

}
