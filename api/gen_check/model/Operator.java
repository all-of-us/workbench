package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets Operator
 */
public enum Operator {
  
  EQUAL("EQUAL"),
  
  NOT_EQUAL("NOT_EQUAL"),
  
  LESS_THAN("LESS_THAN"),
  
  GREATER_THAN("GREATER_THAN"),
  
  LESS_THAN_OR_EQUAL_TO("LESS_THAN_OR_EQUAL_TO"),
  
  GREATER_THAN_OR_EQUAL_TO("GREATER_THAN_OR_EQUAL_TO"),
  
  LIKE("LIKE"),
  
  IN("IN"),
  
  BETWEEN("BETWEEN");

  private String value;

  Operator(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Operator fromValue(String text) {
    for (Operator b : Operator.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

