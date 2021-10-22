package org.pmiops.workbench.monitoring.labels;

import java.util.Collections;
import java.util.Set;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.utils.Booleans;
import org.pmiops.workbench.utils.Enums;

/**
 * Specifies label names and allowed discrete values (if given). Convention is lower_snake_case.
 * Some CamelCase labels have been grandfathered in.
 */
public enum MetricLabel implements MetricLabelBase {
  ACCESS_TIER_SHORT_NAME("access_tier_short_name"),
  ACCESS_TIER_SHORT_NAMES("access_tier_short_names"),
  CRON_JOB_NAME("cron_job_name"),
  CRON_JOB_SUCCEEDED("cron_job_completion_status", Booleans.VALUE_STRINGS),
  DATASET_INVALID("Invalid", Booleans.VALUE_STRINGS),
  GSUITE_DOMAIN("gsuite_domain"),
  METHOD_NAME("method_name"),
  OPERATION_NAME("OperationName"),
  USER_DISABLED("Disabled", Booleans.VALUE_STRINGS),
  WORKSPACE_ACTIVE_STATUS("ActiveStatus", Enums.getValueStrings(WorkspaceActiveStatus.class));

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
