package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets NonAcademicAffiliation
 */
public enum NonAcademicAffiliation {
  
  INDUSTRY("INDUSTRY"),
  
  EDUCATIONAL_INSTITUTION("EDUCATIONAL_INSTITUTION"),
  
  COMMUNITY_SCIENTIST("COMMUNITY_SCIENTIST"),
  
  FREE_TEXT("FREE_TEXT");

  private String value;

  NonAcademicAffiliation(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static NonAcademicAffiliation fromValue(String text) {
    for (NonAcademicAffiliation b : NonAcademicAffiliation.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

