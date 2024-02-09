package org.pmiops.workbench.actionaudit.targetproperties;

import org.jetbrains.annotations.NotNull;

// This is a bit of a one-off for workspace collaboration, where we have a
// target type of User but properties that don't really belong to a User model.
public enum AclTargetProperty implements SimpleTargetProperty {
  ACCESS_LEVEL("access_level");

  private final String propertyName;

  AclTargetProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }
}
