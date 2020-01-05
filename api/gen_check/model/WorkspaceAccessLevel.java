package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * levels of access to workspace, NO ACCESS is akin to removing a user from a workspace ACL.
 */
public enum WorkspaceAccessLevel {
  
  NO_ACCESS("NO ACCESS"),
  
  READER("READER"),
  
  WRITER("WRITER"),
  
  OWNER("OWNER");

  private String value;

  WorkspaceAccessLevel(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static WorkspaceAccessLevel fromValue(String text) {
    for (WorkspaceAccessLevel b : WorkspaceAccessLevel.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

