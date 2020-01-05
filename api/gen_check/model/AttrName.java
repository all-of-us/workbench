package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Name that descibes the type of attribute
 */
public enum AttrName {
  
  ANY("ANY"),
  
  NUM("NUM"),
  
  CAT("CAT"),
  
  AGE("AGE");

  private String value;

  AttrName(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static AttrName fromValue(String text) {
    for (AttrName b : AttrName.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

