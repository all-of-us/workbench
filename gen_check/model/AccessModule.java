package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets AccessModule
 */
public enum AccessModule {
  
  DATA_USE_AGREEMENT("DATA_USE_AGREEMENT"),
  
  COMPLIANCE_TRAINING("COMPLIANCE_TRAINING"),
  
  BETA_ACCESS("BETA_ACCESS"),
  
  ERA_COMMONS("ERA_COMMONS"),
  
  TWO_FACTOR_AUTH("TWO_FACTOR_AUTH");

  private String value;

  AccessModule(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static AccessModule fromValue(String text) {
    for (AccessModule b : AccessModule.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

