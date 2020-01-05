package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Initialization status of a Firecloud billing project for use with Workbench. 
 */
public enum BillingProjectStatus {
  
  NONE("none"),
  
  PENDING("pending"),
  
  READY("ready"),
  
  ERROR("error");

  private String value;

  BillingProjectStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static BillingProjectStatus fromValue(String text) {
    for (BillingProjectStatus b : BillingProjectStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

