package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets AcademicRole
 */
public enum AcademicRole {
  
  UNDERGRADUATE("UNDERGRADUATE"),
  
  TRAINEE("TRAINEE"),
  
  FELLOW("FELLOW"),
  
  EARLY_CAREER("EARLY_CAREER"),
  
  NON_TENURE("NON_TENURE"),
  
  MID_CAREER("MID_CAREER"),
  
  LATE_CAREER("LATE_CAREER"),
  
  PROJECT_PERSONNEL("PROJECT_PERSONNEL");

  private String value;

  AcademicRole(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static AcademicRole fromValue(String text) {
    for (AcademicRole b : AcademicRole.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

