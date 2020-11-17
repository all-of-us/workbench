package org.pmiops.workbench.trackedproperties;

import java.util.Optional;

public class PropertyUpdateResult<TARGET_T> {
  private final TARGET_T updatedTarget;
  private final PropertyUpdateStatus updateStatus;
  private final String statusMessage;

  public PropertyUpdateResult(TARGET_T updatedTarget,
      PropertyUpdateStatus updateStatus, String statusMessage) {
    this.updatedTarget = updatedTarget;
    this.updateStatus = updateStatus;
    this.statusMessage = statusMessage;
  }

  public PropertyUpdateResult(TARGET_T updatedTarget,
      PropertyUpdateStatus updateStatus) {
    this(updatedTarget, updateStatus, null);
  }

  public TARGET_T getUpdatedTarget() {
    return updatedTarget;
  }

  public PropertyUpdateStatus getUpdateStatus() {
    return updateStatus;
  }

  public Optional<String> getStatusMessage() {
    return Optional.ofNullable(statusMessage);
  }
}
