package org.pmiops.workbench.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Cohort {

  @JsonProperty("id")
  private String id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("criteria")
  private String criteria;

  @JsonProperty("type")
  private String type;

  public Cohort(String id, String name, String criteria, String type) {
    this.id = id;
    this.name = name;
    this.criteria = criteria;
    this.type = type;
  }
}
