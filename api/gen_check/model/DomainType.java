package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets DomainType
 */
public enum DomainType {
  
  CONDITION("CONDITION"),
  
  PROCEDURE("PROCEDURE"),
  
  OBSERVATION("OBSERVATION"),
  
  DRUG("DRUG"),
  
  ALL_EVENTS("ALL_EVENTS"),
  
  DEVICE("DEVICE"),
  
  VISIT("VISIT"),
  
  MEASUREMENT("MEASUREMENT"),
  
  PHYSICAL_MEASUREMENT("PHYSICAL_MEASUREMENT"),
  
  LAB("LAB"),
  
  VITAL("VITAL"),
  
  SURVEY("SURVEY"),
  
  PERSON("PERSON");

  private String value;

  DomainType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static DomainType fromValue(String text) {
    for (DomainType b : DomainType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

