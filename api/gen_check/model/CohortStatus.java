package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * possible values indicating whether participants are in or out of the cohort
 */
public enum CohortStatus {
  
  EXCLUDED("EXCLUDED"),
  
  INCLUDED("INCLUDED"),
  
  NEEDS_FURTHER_REVIEW("NEEDS_FURTHER_REVIEW"),
  
  NOT_REVIEWED("NOT_REVIEWED");

  private String value;

  CohortStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static CohortStatus fromValue(String text) {
    for (CohortStatus b : CohortStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

