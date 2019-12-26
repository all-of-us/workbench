package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets IndustryRole
 */
public enum IndustryRole {
  
  PRE_DOCTORAL("PRE_DOCTORAL"),
  
  POST_DOCTORAL("POST_DOCTORAL"),
  
  EARLY("EARLY"),
  
  PI("PI"),
  
  FREE_TEXT("FREE_TEXT");

  private String value;

  IndustryRole(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static IndustryRole fromValue(String text) {
    for (IndustryRole b : IndustryRole.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

