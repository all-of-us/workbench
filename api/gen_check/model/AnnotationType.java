package org.pmiops.workbench.model;

import java.util.Objects;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * possible values representing the type of annotation.
 */
public enum AnnotationType {
  
  STRING("STRING"),
  
  ENUM("ENUM"),
  
  DATE("DATE"),
  
  BOOLEAN("BOOLEAN"),
  
  INTEGER("INTEGER");

  private String value;

  AnnotationType(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static AnnotationType fromValue(String text) {
    for (AnnotationType b : AnnotationType.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

