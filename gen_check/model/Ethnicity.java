package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets Ethnicity
 */
public enum Ethnicity {
  
  HISPANIC("HISPANIC"),
  
  NOT_HISPANIC("NOT_HISPANIC"),
  
  PREFER_NO_ANSWER("PREFER_NO_ANSWER");

  private String value;

  Ethnicity(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Ethnicity fromValue(String text) {
    for (Ethnicity b : Ethnicity.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

