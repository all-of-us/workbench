package org.pmiops.workbench.audit.targetproperties;

import java.util.Objects;

public class PreviousNewValuePair {

  // TODO: make these nullable after Kotlin conversion
  private String previousValue;
  private String newValue;

  private PreviousNewValuePair(String previousValue, String newValue) {
    this.previousValue = previousValue;
    this.newValue = newValue;
  }

  public String getPreviousValue() {
    return previousValue;
  }

  public void setPreviousValue(String previousValue) {
    this.previousValue = previousValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public boolean valueChanged() {
    return !previousValue.equals(newValue);
  }

  @Override
  public String toString() {
    return "PreviousNewValuePair{"
        + "previousValue='"
        + previousValue
        + '\''
        + ", newValue='"
        + newValue
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PreviousNewValuePair)) {
      return false;
    }
    PreviousNewValuePair that = (PreviousNewValuePair) o;
    return Objects.equals(previousValue, that.previousValue)
        && Objects.equals(newValue, that.newValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(previousValue, newValue);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String previousValue;
    private String newValue;

    public Builder setPreviousValue(String previousValue) {
      this.previousValue = previousValue;
      return this;
    }

    public Builder setNewValue(String newValue) {
      this.newValue = newValue;
      return this;
    }

    public PreviousNewValuePair build() {
      return new PreviousNewValuePair(previousValue, newValue);
    }
  }
}
