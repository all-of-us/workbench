package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Short parsable descriptions of specific population categories
 */
public enum SpecificPopulationEnum {
  
  RACE_ETHNICITY("RACE_ETHNICITY"),
  
  AGE_GROUPS("AGE_GROUPS"),
  
  SEX("SEX"),
  
  GENDER_IDENTITY("GENDER_IDENTITY"),
  
  SEXUAL_ORIENTATION("SEXUAL_ORIENTATION"),
  
  GEOGRAPHY("GEOGRAPHY"),
  
  DISABILITY_STATUS("DISABILITY_STATUS"),
  
  ACCESS_TO_CARE("ACCESS_TO_CARE"),
  
  EDUCATION_LEVEL("EDUCATION_LEVEL"),
  
  INCOME_LEVEL("INCOME_LEVEL"),
  
  OTHER("OTHER");

  private String value;

  SpecificPopulationEnum(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static SpecificPopulationEnum fromValue(String text) {
    for (SpecificPopulationEnum b : SpecificPopulationEnum.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

