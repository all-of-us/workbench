package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Cohort
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class Cohort   {
  @JsonProperty("id")
  private Long id = null;

  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("criteria")
  private String criteria = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("creator")
  private String creator = null;

  @JsonProperty("creationTime")
  private Long creationTime = null;

  @JsonProperty("lastModifiedTime")
  private Long lastModifiedTime = null;

  public Cohort id(Long id) {
    this.id = id;
    return this;
  }

   /**
   * Get id
   * @return id
  **/
  @ApiModelProperty(value = "")


  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Cohort etag(String etag) {
    this.etag = etag;
    return this;
  }

   /**
   * Entity tag for optimistic concurrency control. To be set during a read-modify-write to ensure that the client has not attempted to modify a changed resource. 
   * @return etag
  **/
  @ApiModelProperty(value = "Entity tag for optimistic concurrency control. To be set during a read-modify-write to ensure that the client has not attempted to modify a changed resource. ")


  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public Cohort name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Cohort criteria(String criteria) {
    this.criteria = criteria;
    return this;
  }

   /**
   * Internal representation of the cohort definition. Clients should not depend directly on this, but instead call client functions to issue a SQL query for the cohort. 
   * @return criteria
  **/
  @ApiModelProperty(required = true, value = "Internal representation of the cohort definition. Clients should not depend directly on this, but instead call client functions to issue a SQL query for the cohort. ")
  @NotNull


  public String getCriteria() {
    return criteria;
  }

  public void setCriteria(String criteria) {
    this.criteria = criteria;
  }

  public Cohort type(String type) {
    this.type = type;
    return this;
  }

   /**
   * Get type
   * @return type
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Cohort description(String description) {
    this.description = description;
    return this;
  }

   /**
   * Get description
   * @return description
  **/
  @ApiModelProperty(value = "")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Cohort creator(String creator) {
    this.creator = creator;
    return this;
  }

   /**
   * Get creator
   * @return creator
  **/
  @ApiModelProperty(value = "")


  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public Cohort creationTime(Long creationTime) {
    this.creationTime = creationTime;
    return this;
  }

   /**
   * Milliseconds since the UNIX epoch.
   * @return creationTime
  **/
  @ApiModelProperty(value = "Milliseconds since the UNIX epoch.")


  public Long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Long creationTime) {
    this.creationTime = creationTime;
  }

  public Cohort lastModifiedTime(Long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

   /**
   * Milliseconds since the UNIX epoch.
   * @return lastModifiedTime
  **/
  @ApiModelProperty(value = "Milliseconds since the UNIX epoch.")


  public Long getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(Long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Cohort cohort = (Cohort) o;
    return Objects.equals(this.id, cohort.id) &&
        Objects.equals(this.etag, cohort.etag) &&
        Objects.equals(this.name, cohort.name) &&
        Objects.equals(this.criteria, cohort.criteria) &&
        Objects.equals(this.type, cohort.type) &&
        Objects.equals(this.description, cohort.description) &&
        Objects.equals(this.creator, cohort.creator) &&
        Objects.equals(this.creationTime, cohort.creationTime) &&
        Objects.equals(this.lastModifiedTime, cohort.lastModifiedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, etag, name, criteria, type, description, creator, creationTime, lastModifiedTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Cohort {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    criteria: ").append(toIndentedString(criteria)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    creator: ").append(toIndentedString(creator)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    lastModifiedTime: ").append(toIndentedString(lastModifiedTime)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

