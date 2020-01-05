package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * a domain for concepts corresponding to a table in the OMOP schema
 */
public enum Domain {
  
  OBSERVATION("OBSERVATION"),
  
  PROCEDURE("PROCEDURE"),
  
  DRUG("DRUG"),
  
  CONDITION("CONDITION"),
  
  MEASUREMENT("MEASUREMENT"),
  
  DEVICE("DEVICE"),
  
  DEATH("DEATH"),
  
  VISIT("VISIT"),
  
  SURVEY("SURVEY"),
  
  PERSON("PERSON"),
  
  PHYSICALMEASUREMENT("PHYSICALMEASUREMENT");

  private String value;

  Domain(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Domain fromValue(String text) {
    for (Domain b : Domain.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

