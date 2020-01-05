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
public enum CriteriaType {
  
  AGE("AGE"),
  
  ATC("ATC"),
  
  BRAND("BRAND"),
  
  CPT4("CPT4"),
  
  DECEASED("DECEASED"),
  
  ETHNICITY("ETHNICITY"),
  
  GENDER("GENDER"),
  
  HCPCS("HCPCS"),
  
  ICD10CM("ICD10CM"),
  
  ICD10PCS("ICD10PCS"),
  
  ICD9CM("ICD9CM"),
  
  ICD9PROC("ICD9Proc"),
  
  LOINC("LOINC"),
  
  PPI("PPI"),
  
  RACE("RACE"),
  
  RXNORM("RXNORM"),
  
  SEX("SEX"),
  
  SNOMED("SNOMED"),
  
  VISIT("VISIT");

  private String value;

  CriteriaType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static CriteriaType fromValue(String text) {
    for (CriteriaType b : CriteriaType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

