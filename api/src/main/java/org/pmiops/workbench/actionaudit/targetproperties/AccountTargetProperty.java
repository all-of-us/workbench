package org.pmiops.workbench.actionaudit.targetproperties;

import jakarta.validation.constraints.NotNull;

public enum AccountTargetProperty implements SimpleTargetProperty {
  IS_ENABLED("is_enabled"),
  ACKNOWLEDGED_TOS_VERSION("acknowledged_tos_version"),
  INITIAL_CREDITS_OVERRIDE("initial_credits_override"),
  ACCESS_TIERS("access_tiers");

  private final String propertyName;

  AccountTargetProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }
}
