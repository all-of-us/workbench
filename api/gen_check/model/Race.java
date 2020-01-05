package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets Race
 */
public enum Race {
  
  AIAN("AIAN"),
  
  ASIAN("ASIAN"),
  
  AA("AA"),
  
  NHOPI("NHOPI"),
  
  WHITE("WHITE"),
  
  NONE("NONE"),
  
  PREFER_NO_ANSWER("PREFER_NO_ANSWER");

  private String value;

  Race(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Race fromValue(String text) {
    for (Race b : Race.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

