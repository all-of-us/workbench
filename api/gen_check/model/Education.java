package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets Education
 */
public enum Education {
  
  NO_EDUCATION("NO_EDUCATION"),
  
  GRADES_1_12("GRADES_1_12"),
  
  UNDERGRADUATE("UNDERGRADUATE"),
  
  COLLEGE_GRADUATE("COLLEGE_GRADUATE"),
  
  MASTER("MASTER"),
  
  DOCTORATE("DOCTORATE");

  private String value;

  Education(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Education fromValue(String text) {
    for (Education b : Education.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

