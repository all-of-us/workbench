package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * possible types of modifiers
 */
public enum ModifierType {
  
  AGE_AT_EVENT("AGE_AT_EVENT"),
  
  NUM_OF_OCCURRENCES("NUM_OF_OCCURRENCES"),
  
  EVENT_DATE("EVENT_DATE"),
  
  ENCOUNTERS("ENCOUNTERS");

  private String value;

  ModifierType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ModifierType fromValue(String text) {
    for (ModifierType b : ModifierType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

