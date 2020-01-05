package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * filter on whether standard, non-standard, or all concepts should be returned\\
 */
public enum StandardConceptFilter {
  
  ALL_CONCEPTS("ALL_CONCEPTS"),
  
  STANDARD_CONCEPTS("STANDARD_CONCEPTS"),
  
  NON_STANDARD_CONCEPTS("NON_STANDARD_CONCEPTS"),
  
  STANDARD_OR_CODE_ID_MATCH("STANDARD_OR_CODE_ID_MATCH");

  private String value;

  StandardConceptFilter(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static StandardConceptFilter fromValue(String text) {
    for (StandardConceptFilter b : StandardConceptFilter.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

