package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * actions a user can have authority/permission to perform
 */
public enum Authority {
  
  REVIEW_RESEARCH_PURPOSE("REVIEW_RESEARCH_PURPOSE"),
  
  DEVELOPER("DEVELOPER"),
  
  ACCESS_CONTROL_ADMIN("ACCESS_CONTROL_ADMIN"),
  
  FEATURED_WORKSPACE_ADMIN("FEATURED_WORKSPACE_ADMIN");

  private String value;

  Authority(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static Authority fromValue(String text) {
    for (Authority b : Authority.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

