package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Mentions refer to occurrences of entry date per person
 */
public enum TemporalMention {
  
  ANY_MENTION("ANY_MENTION"),
  
  FIRST_MENTION("FIRST_MENTION"),
  
  LAST_MENTION("LAST_MENTION");

  private String value;

  TemporalMention(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static TemporalMention fromValue(String text) {
    for (TemporalMention b : TemporalMention.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

