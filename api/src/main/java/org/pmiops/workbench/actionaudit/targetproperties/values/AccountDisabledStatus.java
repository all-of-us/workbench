package org.pmiops.workbench.actionaudit.targetproperties.values;

public enum AccountDisabledStatus {
  DISABLED("disabled"),
  ENABLED("enabled");

  private String valueName;

  AccountDisabledStatus(String valueName) {
    this.valueName = valueName;
  }

  public String getValueName() {
    return valueName;
  }

  public static AccountDisabledStatus fromDisabledBoolean(boolean isDisabled) {
    return isDisabled ? DISABLED : ENABLED;
  }
}
