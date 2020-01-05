package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status of workspace
 */
public enum WorkspaceActiveStatus {
  
  ACTIVE("ACTIVE"),
  
  DELETED("DELETED"),
  
  PENDING_DELETION_POST_1PPW_MIGRATION("PENDING_DELETION_POST_1PPW_MIGRATION");

  private String value;

  WorkspaceActiveStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static WorkspaceActiveStatus fromValue(String text) {
    for (WorkspaceActiveStatus b : WorkspaceActiveStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

