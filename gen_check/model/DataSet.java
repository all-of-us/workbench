package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataSet
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class DataSet   {
  @JsonProperty("id")
  private Long id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("includesAllParticipants")
  private Boolean includesAllParticipants = false;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("workspaceId")
  private Long workspaceId = null;

  @JsonProperty("lastModifiedTime")
  private Long lastModifiedTime = null;

  @JsonProperty("conceptSets")
  private List<ConceptSet> conceptSets = null;

  @JsonProperty("cohorts")
  private List<Cohort> cohorts = null;

  @JsonProperty("domainValuePairs")
  private List<DomainValuePair> domainValuePairs = null;

  @JsonProperty("prePackagedConceptSet")
  private PrePackagedConceptSetEnum prePackagedConceptSet = null;

  public DataSet id(Long id) {
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

  public DataSet name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(value = "")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DataSet etag(String etag) {
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

  public DataSet includesAllParticipants(Boolean includesAllParticipants) {
    this.includesAllParticipants = includesAllParticipants;
    return this;
  }

   /**
   * Get includesAllParticipants
   * @return includesAllParticipants
  **/
  @ApiModelProperty(value = "")


  public Boolean getIncludesAllParticipants() {
    return includesAllParticipants;
  }

  public void setIncludesAllParticipants(Boolean includesAllParticipants) {
    this.includesAllParticipants = includesAllParticipants;
  }

  public DataSet description(String description) {
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

  public DataSet workspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

   /**
   * Get workspaceId
   * @return workspaceId
  **/
  @ApiModelProperty(value = "")


  public Long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public DataSet lastModifiedTime(Long lastModifiedTime) {
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

  public DataSet conceptSets(List<ConceptSet> conceptSets) {
    this.conceptSets = conceptSets;
    return this;
  }

  public DataSet addConceptSetsItem(ConceptSet conceptSetsItem) {
    if (this.conceptSets == null) {
      this.conceptSets = new ArrayList<ConceptSet>();
    }
    this.conceptSets.add(conceptSetsItem);
    return this;
  }

   /**
   * All concept sets in the data set 
   * @return conceptSets
  **/
  @ApiModelProperty(value = "All concept sets in the data set ")

  @Valid

  public List<ConceptSet> getConceptSets() {
    return conceptSets;
  }

  public void setConceptSets(List<ConceptSet> conceptSets) {
    this.conceptSets = conceptSets;
  }

  public DataSet cohorts(List<Cohort> cohorts) {
    this.cohorts = cohorts;
    return this;
  }

  public DataSet addCohortsItem(Cohort cohortsItem) {
    if (this.cohorts == null) {
      this.cohorts = new ArrayList<Cohort>();
    }
    this.cohorts.add(cohortsItem);
    return this;
  }

   /**
   * All cohorts in the data set 
   * @return cohorts
  **/
  @ApiModelProperty(value = "All cohorts in the data set ")

  @Valid

  public List<Cohort> getCohorts() {
    return cohorts;
  }

  public void setCohorts(List<Cohort> cohorts) {
    this.cohorts = cohorts;
  }

  public DataSet domainValuePairs(List<DomainValuePair> domainValuePairs) {
    this.domainValuePairs = domainValuePairs;
    return this;
  }

  public DataSet addDomainValuePairsItem(DomainValuePair domainValuePairsItem) {
    if (this.domainValuePairs == null) {
      this.domainValuePairs = new ArrayList<DomainValuePair>();
    }
    this.domainValuePairs.add(domainValuePairsItem);
    return this;
  }

   /**
   * All the selected domain/value pairs in the data set 
   * @return domainValuePairs
  **/
  @ApiModelProperty(value = "All the selected domain/value pairs in the data set ")

  @Valid

  public List<DomainValuePair> getDomainValuePairs() {
    return domainValuePairs;
  }

  public void setDomainValuePairs(List<DomainValuePair> domainValuePairs) {
    this.domainValuePairs = domainValuePairs;
  }

  public DataSet prePackagedConceptSet(PrePackagedConceptSetEnum prePackagedConceptSet) {
    this.prePackagedConceptSet = prePackagedConceptSet;
    return this;
  }

   /**
   * Get prePackagedConceptSet
   * @return prePackagedConceptSet
  **/
  @ApiModelProperty(value = "")

  @Valid

  public PrePackagedConceptSetEnum getPrePackagedConceptSet() {
    return prePackagedConceptSet;
  }

  public void setPrePackagedConceptSet(PrePackagedConceptSetEnum prePackagedConceptSet) {
    this.prePackagedConceptSet = prePackagedConceptSet;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSet dataSet = (DataSet) o;
    return Objects.equals(this.id, dataSet.id) &&
        Objects.equals(this.name, dataSet.name) &&
        Objects.equals(this.etag, dataSet.etag) &&
        Objects.equals(this.includesAllParticipants, dataSet.includesAllParticipants) &&
        Objects.equals(this.description, dataSet.description) &&
        Objects.equals(this.workspaceId, dataSet.workspaceId) &&
        Objects.equals(this.lastModifiedTime, dataSet.lastModifiedTime) &&
        Objects.equals(this.conceptSets, dataSet.conceptSets) &&
        Objects.equals(this.cohorts, dataSet.cohorts) &&
        Objects.equals(this.domainValuePairs, dataSet.domainValuePairs) &&
        Objects.equals(this.prePackagedConceptSet, dataSet.prePackagedConceptSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, etag, includesAllParticipants, description, workspaceId, lastModifiedTime, conceptSets, cohorts, domainValuePairs, prePackagedConceptSet);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataSet {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    includesAllParticipants: ").append(toIndentedString(includesAllParticipants)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    lastModifiedTime: ").append(toIndentedString(lastModifiedTime)).append("\n");
    sb.append("    conceptSets: ").append(toIndentedString(conceptSets)).append("\n");
    sb.append("    cohorts: ").append(toIndentedString(cohorts)).append("\n");
    sb.append("    domainValuePairs: ").append(toIndentedString(domainValuePairs)).append("\n");
    sb.append("    prePackagedConceptSet: ").append(toIndentedString(prePackagedConceptSet)).append("\n");
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

