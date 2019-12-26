package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets Gender
 */
public enum Gender {
  
  MALE("MALE"),
  
  FEMALE("FEMALE"),
  
  NON_BINARY("NON_BINARY"),
  
  TRANSGENDER("TRANSGENDER"),
  
  INTERSEX("INTERSEX"),
  
  NONE("NONE"),
  
  PREFER_NO_ANSWER("PREFER_NO_ANSWER");

  private String value;

  Gender(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Gender fromValue(String text) {
    for (Gender b : Gender.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

