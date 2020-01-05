package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.Valid;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets ClusterStatus
 */
public enum ClusterStatus {
  
  CREATING("Creating"),
  
  RUNNING("Running"),
  
  UPDATING("Updating"),
  
  ERROR("Error"),
  
  STOPPING("Stopping"),
  
  STOPPED("Stopped"),
  
  STARTING("Starting"),
  
  DELETING("Deleting"),
  
  DELETED("Deleted"),
  
  UNKNOWN("Unknown");

  private String value;

  ClusterStatus(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ClusterStatus fromValue(String text) {
    for (ClusterStatus b : ClusterStatus.values()) {
      if (String.valueOf(b.value).equals(text)) {
        return b;
      }
    }
    return null;
  }
}

