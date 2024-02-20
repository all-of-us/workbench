package org.pmiops.workbench.actionaudit.targetproperties;

import org.jetbrains.annotations.NotNull;

public enum AccountTargetProperty implements SimpleTargetProperty {
  IS_ENABLED("is_enabled"),
  ACKNOWLEDGED_TOS_VERSION("acknowledged_tos_version"),
  FREE_TIER_DOLLAR_QUOTA("free_tier_dollar_quota"),
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
