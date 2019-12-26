package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Whether a billing project is provided by the Researcher Workbench as a Free Tier project subject to quota checks, or the project has a billing account provided by the user 
 */
public enum BillingAccountType {
  
  FREE_TIER("FREE_TIER"),
  
  USER_PROVIDED("USER_PROVIDED");

  private String value;

  BillingAccountType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static BillingAccountType fromValue(String text) {
    for (BillingAccountType b : BillingAccountType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

