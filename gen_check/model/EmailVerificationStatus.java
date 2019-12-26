package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * stage of email verification
 */
public enum EmailVerificationStatus {
  
  UNVERIFIED("unverified"),
  
  PENDING("pending"),
  
  SUBSCRIBED("subscribed");

  private String value;

  EmailVerificationStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static EmailVerificationStatus fromValue(String text) {
    for (EmailVerificationStatus b : EmailVerificationStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

