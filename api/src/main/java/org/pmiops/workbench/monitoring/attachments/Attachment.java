package org.pmiops.workbench.monitoring.attachments;

import java.util.Collections;
import java.util.Set;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.utils.Booleans;
import org.pmiops.workbench.utils.Enums;

public enum Attachment implements AttachmentBase {
  BUFFER_ENTRY_STATUS("buffer_entry_status", Enums.getValueStrings(BufferEntryStatus.class)),
  DEBUG_CONTINUOUS_VALUE("debug_continuous_value"),
  DATASET_INVALID("dataset_invalid", Booleans.VALUE_STRINGS),
  USER_BYPASSED_BETA("user_bypassed_beta", Booleans.VALUE_STRINGS),
  USER_DISABLED("user_disabled", Booleans.VALUE_STRINGS),
  DATA_ACCESS_LEVEL("data_access_level", Enums.getValueStrings(DataAccessLevel.class)),
  WORKSPACE_ACTIVE_STATUS("workspace_active_status");

  private String keyName;
  private Set<String> allowedDiscreteValues;

  Attachment(String keyName) {
    this(keyName, Collections.emptySet());
  }

  Attachment(String keyName, Set<String> allowedDiscreteValues) {
    this.keyName = keyName;
    this.allowedDiscreteValues = allowedDiscreteValues;
  }

  @Override
  public String getKeyName() {
    return keyName;
  }

  @Override
  public Set<String> getAllowedDiscreteValues() {
    return allowedDiscreteValues;
  }
}
