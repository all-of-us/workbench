package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * time refers to the amount of time in reference to temporal mentions
 */
public enum TemporalTime {
  
  DURING_SAME_ENCOUNTER_AS("DURING_SAME_ENCOUNTER_AS"),
  
  X_DAYS_BEFORE("X_DAYS_BEFORE"),
  
  X_DAYS_AFTER("X_DAYS_AFTER"),
  
  WITHIN_X_DAYS_OF("WITHIN_X_DAYS_OF");

  private String value;

  TemporalTime(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static TemporalTime fromValue(String text) {
    for (TemporalTime b : TemporalTime.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

