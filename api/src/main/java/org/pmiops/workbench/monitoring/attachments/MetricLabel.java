package org.pmiops.workbench.monitoring.attachments;

import java.util.Collections;
import java.util.Set;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.utils.Booleans;
import org.pmiops.workbench.utils.Enums;

public enum MetricLabel implements MetricLabelBase {
  BUFFER_ENTRY_STATUS("BufferEntryStatus", Enums.getValueStrings(BufferEntryStatus.class)),
  DATA_ACCESS_LEVEL("DataAccessLevel", Enums.getValueStrings(DataAccessLevel.class)),
  DATASET_INVALID("Invalid", Booleans.VALUE_STRINGS),
  OPERATION_NAME("OperationName"),
  USER_BYPASSED_BETA("BypassedBeta", Booleans.VALUE_STRINGS),
  USER_DISABLED("Disabled", Booleans.VALUE_STRINGS),
  WORKSPACE_ACTIVE_STATUS("ActiveStatus");

  private String keyName;
  private Set<String> allowedDiscreteValues;

  MetricLabel(String keyName) {
    this(keyName, Collections.emptySet());
  }

  MetricLabel(String displayName, Set<String> allowedDiscreteValues) {
    this.keyName = displayName;
    this.allowedDiscreteValues = allowedDiscreteValues;
  }

  @Override
  public String getName() {
    return keyName;
  }

  @Override
  public Set<String> getAllowedDiscreteValues() {
    return allowedDiscreteValues;
  }
}
