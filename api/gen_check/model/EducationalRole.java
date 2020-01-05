package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets EducationalRole
 */
public enum EducationalRole {
  
  TEACHER("TEACHER"),
  
  STUDENT("STUDENT"),
  
  ADMIN("ADMIN"),
  
  FREE_TEXT("FREE_TEXT");

  private String value;

  EducationalRole(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static EducationalRole fromValue(String text) {
    for (EducationalRole b : EducationalRole.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

