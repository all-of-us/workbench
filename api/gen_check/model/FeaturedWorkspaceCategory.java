package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets FeaturedWorkspaceCategory
 */
public enum FeaturedWorkspaceCategory {
  
  PHENOTYPE_LIBRARY("PHENOTYPE_LIBRARY"),
  
  TUTORIAL_WORKSPACES("TUTORIAL_WORKSPACES");

  private String value;

  FeaturedWorkspaceCategory(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static FeaturedWorkspaceCategory fromValue(String text) {
    for (FeaturedWorkspaceCategory b : FeaturedWorkspaceCategory.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

