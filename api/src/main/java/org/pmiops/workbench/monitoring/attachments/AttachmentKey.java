package org.pmiops.workbench.monitoring.attachments;

public enum AttachmentKey implements AttachmentKeyBase {
  BUFFER_ENTRY_STATUS("buffer_entry_status"),
  DEBUG_COLOR("debug_color"),
  NOTEBOOK_NAME("notebook_name"),
  NOTEBOOK_WORKSPACE_NAMESPACE("notebook_workspace_namespace"),
  USER_DISABLED("user_disabled");

  private String keyName;

  AttachmentKey(String keyName) {
    this.keyName = keyName;
  }

  @Override
  public String getKeyName() {
    return keyName;
  }
}
