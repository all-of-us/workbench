package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * match column type on concept search
 */
public enum MatchType {
  
  CODE("CONCEPT_CODE"),
  
  ID("CONCEPT_ID"),
  
  NAME("CONCEPT_NAME");

  private String value;

  MatchType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static MatchType fromValue(String text) {
    for (MatchType b : MatchType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

