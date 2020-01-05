package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets FilterColumns
 */
public enum FilterColumns {
  
  PARTICIPANTID("PARTICIPANTID"),
  
  STATUS("STATUS"),
  
  GENDER("GENDER"),
  
  BIRTHDATE("BIRTHDATE"),
  
  RACE("RACE"),
  
  ETHNICITY("ETHNICITY"),
  
  DECEASED("DECEASED"),
  
  START_DATE("START_DATE"),
  
  STANDARD_CODE("STANDARD_CODE"),
  
  STANDARD_VOCAB("STANDARD_VOCAB"),
  
  STANDARD_NAME("STANDARD_NAME"),
  
  STANDARD_CONCEPT_ID("STANDARD_CONCEPT_ID"),
  
  SOURCE_CODE("SOURCE_CODE"),
  
  SOURCE_VOCAB("SOURCE_VOCAB"),
  
  SOURCE_NAME("SOURCE_NAME"),
  
  SOURCE_CONCEPT_ID("SOURCE_CONCEPT_ID"),
  
  DOMAIN("DOMAIN"),
  
  AGE_AT_EVENT("AGE_AT_EVENT"),
  
  NUM_OF_MENTIONS("NUM_OF_MENTIONS"),
  
  FIRST_MENTION("FIRST_MENTION"),
  
  LAST_MENTION("LAST_MENTION"),
  
  VISIT_TYPE("VISIT_TYPE"),
  
  ROUTE("ROUTE"),
  
  DOSE("DOSE"),
  
  STRENGTH("STRENGTH"),
  
  VAL_AS_NUMBER("VAL_AS_NUMBER"),
  
  UNIT("UNIT"),
  
  REF_RANGE("REF_RANGE"),
  
  SURVEY_NAME("SURVEY_NAME"),
  
  QUESTION("QUESTION"),
  
  ANSWER("ANSWER");

  private String value;

  FilterColumns(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static FilterColumns fromValue(String text) {
    for (FilterColumns b : FilterColumns.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

