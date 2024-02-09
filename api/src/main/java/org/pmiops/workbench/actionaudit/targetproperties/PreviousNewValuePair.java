package org.pmiops.workbench.actionaudit.targetproperties;

public record PreviousNewValuePair(String previousValue, String newValue) {
  public boolean hasValueChanged() {
    return (previousValue == null && newValue != null)
        || (previousValue != null && !previousValue.equals(newValue));
  }
}
