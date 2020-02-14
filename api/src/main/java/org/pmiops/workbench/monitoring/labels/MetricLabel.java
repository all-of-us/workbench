package org.pmiops.workbench.monitoring.labels;

import java.util.Collections;
import java.util.Set;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.utils.Booleans;
import org.pmiops.workbench.utils.Enums;

/**
 * Specifies label names and allowed discrete values (if given).
 * Convention is lower_snake_case. Some CamelCase labels have been
 * grandfathered in.
 */
public enum MetricLabel implements MetricLabelBase {
  BUFFER_ENTRY_STATUS("BufferEntryStatus", Enums.getValueStrings(BufferEntryStatus.class)),
  CRON_JOB_NAME("cron_job_name"),
  CRON_JOB_SUCCEEDED("cron_job_completion_status", Booleans.VALUE_STRINGS),
  DATASET_INVALID("Invalid", Booleans.VALUE_STRINGS),
  DATA_ACCESS_LEVEL("DataAccessLevel", Enums.getValueStrings(DataAccessLevel.class)),
  GSUITE_DOMAIN("gsuite_domain"),
  METHOD_NAME("method_name"),
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
