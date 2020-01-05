package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Domain;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DomainInfo
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class DomainInfo   {
  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("standardConceptCount")
  private Long standardConceptCount = null;

  @JsonProperty("allConceptCount")
  private Long allConceptCount = null;

  @JsonProperty("participantCount")
  private Long participantCount = null;

  public DomainInfo domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * the domain ID
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "the domain ID")
  @NotNull

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public DomainInfo name(String name) {
    this.name = name;
    return this;
  }

   /**
   * display name of the domain
   * @return name
  **/
  @ApiModelProperty(required = true, value = "display name of the domain")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DomainInfo description(String description) {
    this.description = description;
    return this;
  }

   /**
   * description of the domain
   * @return description
  **/
  @ApiModelProperty(required = true, value = "description of the domain")
  @NotNull


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DomainInfo standardConceptCount(Long standardConceptCount) {
    this.standardConceptCount = standardConceptCount;
    return this;
  }

   /**
   * number of standard concepts in this domain
   * @return standardConceptCount
  **/
  @ApiModelProperty(required = true, value = "number of standard concepts in this domain")
  @NotNull


  public Long getStandardConceptCount() {
    return standardConceptCount;
  }

  public void setStandardConceptCount(Long standardConceptCount) {
    this.standardConceptCount = standardConceptCount;
  }

  public DomainInfo allConceptCount(Long allConceptCount) {
    this.allConceptCount = allConceptCount;
    return this;
  }

   /**
   * number of concepts in this domain (standard or non-standard)
   * @return allConceptCount
  **/
  @ApiModelProperty(required = true, value = "number of concepts in this domain (standard or non-standard)")
  @NotNull


  public Long getAllConceptCount() {
    return allConceptCount;
  }

  public void setAllConceptCount(Long allConceptCount) {
    this.allConceptCount = allConceptCount;
  }

  public DomainInfo participantCount(Long participantCount) {
    this.participantCount = participantCount;
    return this;
  }

   /**
   * number of participants with data in the CDR for a concept in this domain
   * @return participantCount
  **/
  @ApiModelProperty(required = true, value = "number of participants with data in the CDR for a concept in this domain")
  @NotNull


  public Long getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Long participantCount) {
    this.participantCount = participantCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DomainInfo domainInfo = (DomainInfo) o;
    return Objects.equals(this.domain, domainInfo.domain) &&
        Objects.equals(this.name, domainInfo.name) &&
        Objects.equals(this.description, domainInfo.description) &&
        Objects.equals(this.standardConceptCount, domainInfo.standardConceptCount) &&
        Objects.equals(this.allConceptCount, domainInfo.allConceptCount) &&
        Objects.equals(this.participantCount, domainInfo.participantCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, name, description, standardConceptCount, allConceptCount, participantCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DomainInfo {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    standardConceptCount: ").append(toIndentedString(standardConceptCount)).append("\n");
    sb.append("    allConceptCount: ").append(toIndentedString(allConceptCount)).append("\n");
    sb.append("    participantCount: ").append(toIndentedString(participantCount)).append("\n");
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

