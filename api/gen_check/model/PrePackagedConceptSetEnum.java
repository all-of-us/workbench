package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets PrePackagedConceptSetEnum
 */
public enum PrePackagedConceptSetEnum {
  
  NONE("NONE"),
  
  DEMOGRAPHICS("DEMOGRAPHICS"),
  
  SURVEY("SURVEY"),
  
  BOTH("BOTH");

  private String value;

  PrePackagedConceptSetEnum(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static PrePackagedConceptSetEnum fromValue(String text) {
    for (PrePackagedConceptSetEnum b : PrePackagedConceptSetEnum.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

