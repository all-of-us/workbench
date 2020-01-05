package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * possible criteria types
 */
public enum CriteriaSubType {
  
  ANSWER("ANSWER"),
  
  BMI("BMI"),
  
  BP("BP"),
  
  CLIN("CLIN"),
  
  HC("HC"),
  
  HEIGHT("HEIGHT"),
  
  HR("HR"),
  
  HR_DETAIL("HR-DETAIL"),
  
  HR_IRR("HR-IRR"),
  
  HR_NOIRR("HR-NOIRR"),
  
  LAB("LAB"),
  
  PREG("PREG"),
  
  QUESTION("QUESTION"),
  
  SURVEY("SURVEY"),
  
  WC("WC"),
  
  WEIGHT("WEIGHT"),
  
  WHEEL("WHEEL");

  private String value;

  CriteriaSubType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static CriteriaSubType fromValue(String text) {
    for (CriteriaSubType b : CriteriaSubType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

