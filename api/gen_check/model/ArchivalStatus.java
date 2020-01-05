package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * CDR archival status; archived CDRs cannot be used for new workspaces.
 */
public enum ArchivalStatus {
  
  LIVE("LIVE"),
  
  ARCHIVED("ARCHIVED");

  private String value;

  ArchivalStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ArchivalStatus fromValue(String text) {
    for (ArchivalStatus b : ArchivalStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

