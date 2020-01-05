package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Surveys;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ConceptSet
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class ConceptSet   {
  @JsonProperty("id")
  private Long id = null;

  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("survey")
  private Surveys survey = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("creator")
  private String creator = null;

  @JsonProperty("creationTime")
  private Long creationTime = null;

  @JsonProperty("lastModifiedTime")
  private Long lastModifiedTime = null;

  @JsonProperty("participantCount")
  private Integer participantCount = null;

  @JsonProperty("concepts")
  private List<Concept> concepts = null;

  public ConceptSet id(Long id) {
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

  public ConceptSet etag(String etag) {
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

  public ConceptSet name(String name) {
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

  public ConceptSet domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Domain corresponding to an OMOP table that can contain rows for the concepts in this concept set. Note that the Domain values RACE, GENDER, and ETHNICITY are not allowed here; it makes sense to specify concepts in these domains in cohort criteria, but there isn't much value in having concept sets defined for them. 
   * @return domain
  **/
  @ApiModelProperty(value = "Domain corresponding to an OMOP table that can contain rows for the concepts in this concept set. Note that the Domain values RACE, GENDER, and ETHNICITY are not allowed here; it makes sense to specify concepts in these domains in cohort criteria, but there isn't much value in having concept sets defined for them. ")

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public ConceptSet survey(Surveys survey) {
    this.survey = survey;
    return this;
  }

   /**
   * Survey 
   * @return survey
  **/
  @ApiModelProperty(value = "Survey ")

  @Valid

  public Surveys getSurvey() {
    return survey;
  }

  public void setSurvey(Surveys survey) {
    this.survey = survey;
  }

  public ConceptSet description(String description) {
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

  public ConceptSet creator(String creator) {
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

  public ConceptSet creationTime(Long creationTime) {
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

  public ConceptSet lastModifiedTime(Long lastModifiedTime) {
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

  public ConceptSet participantCount(Integer participantCount) {
    this.participantCount = participantCount;
    return this;
  }

   /**
   * Count of participants in the CDR version for the workspace containing this concept set that match the specified concept set 
   * @return participantCount
  **/
  @ApiModelProperty(value = "Count of participants in the CDR version for the workspace containing this concept set that match the specified concept set ")


  public Integer getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Integer participantCount) {
    this.participantCount = participantCount;
  }

  public ConceptSet concepts(List<Concept> concepts) {
    this.concepts = concepts;
    return this;
  }

  public ConceptSet addConceptsItem(Concept conceptsItem) {
    if (this.concepts == null) {
      this.concepts = new ArrayList<Concept>();
    }
    this.concepts.add(conceptsItem);
    return this;
  }

   /**
   * Concepts in the concept set, in conceptName order.
   * @return concepts
  **/
  @ApiModelProperty(value = "Concepts in the concept set, in conceptName order.")

  @Valid

  public List<Concept> getConcepts() {
    return concepts;
  }

  public void setConcepts(List<Concept> concepts) {
    this.concepts = concepts;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConceptSet conceptSet = (ConceptSet) o;
    return Objects.equals(this.id, conceptSet.id) &&
        Objects.equals(this.etag, conceptSet.etag) &&
        Objects.equals(this.name, conceptSet.name) &&
        Objects.equals(this.domain, conceptSet.domain) &&
        Objects.equals(this.survey, conceptSet.survey) &&
        Objects.equals(this.description, conceptSet.description) &&
        Objects.equals(this.creator, conceptSet.creator) &&
        Objects.equals(this.creationTime, conceptSet.creationTime) &&
        Objects.equals(this.lastModifiedTime, conceptSet.lastModifiedTime) &&
        Objects.equals(this.participantCount, conceptSet.participantCount) &&
        Objects.equals(this.concepts, conceptSet.concepts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, etag, name, domain, survey, description, creator, creationTime, lastModifiedTime, participantCount, concepts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConceptSet {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    survey: ").append(toIndentedString(survey)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    creator: ").append(toIndentedString(creator)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    lastModifiedTime: ").append(toIndentedString(lastModifiedTime)).append("\n");
    sb.append("    participantCount: ").append(toIndentedString(participantCount)).append("\n");
    sb.append("    concepts: ").append(toIndentedString(concepts)).append("\n");
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

