package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataSetRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public class DataSetRequest   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("workspaceId")
  private Long workspaceId = null;

  @JsonProperty("includesAllParticipants")
  private Boolean includesAllParticipants = false;

  @JsonProperty("prePackagedConceptSet")
  private PrePackagedConceptSetEnum prePackagedConceptSet = null;

  @JsonProperty("conceptSetIds")
  private List<Long> conceptSetIds = null;

  @JsonProperty("cohortIds")
  private List<Long> cohortIds = null;

  @JsonProperty("domainValuePairs")
  private List<DomainValuePair> domainValuePairs = null;

  public DataSetRequest name(String name) {
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

  public DataSetRequest description(String description) {
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

  public DataSetRequest etag(String etag) {
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

  public DataSetRequest workspaceId(Long workspaceId) {
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

  public DataSetRequest includesAllParticipants(Boolean includesAllParticipants) {
    this.includesAllParticipants = includesAllParticipants;
    return this;
  }

   /**
   * Whether to include all participants or filter by cohorts 
   * @return includesAllParticipants
  **/
  @ApiModelProperty(value = "Whether to include all participants or filter by cohorts ")


  public Boolean getIncludesAllParticipants() {
    return includesAllParticipants;
  }

  public void setIncludesAllParticipants(Boolean includesAllParticipants) {
    this.includesAllParticipants = includesAllParticipants;
  }

  public DataSetRequest prePackagedConceptSet(PrePackagedConceptSetEnum prePackagedConceptSet) {
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

  public DataSetRequest conceptSetIds(List<Long> conceptSetIds) {
    this.conceptSetIds = conceptSetIds;
    return this;
  }

  public DataSetRequest addConceptSetIdsItem(Long conceptSetIdsItem) {
    if (this.conceptSetIds == null) {
      this.conceptSetIds = new ArrayList<Long>();
    }
    this.conceptSetIds.add(conceptSetIdsItem);
    return this;
  }

   /**
   * The ids of all concept sets in the data set 
   * @return conceptSetIds
  **/
  @ApiModelProperty(value = "The ids of all concept sets in the data set ")


  public List<Long> getConceptSetIds() {
    return conceptSetIds;
  }

  public void setConceptSetIds(List<Long> conceptSetIds) {
    this.conceptSetIds = conceptSetIds;
  }

  public DataSetRequest cohortIds(List<Long> cohortIds) {
    this.cohortIds = cohortIds;
    return this;
  }

  public DataSetRequest addCohortIdsItem(Long cohortIdsItem) {
    if (this.cohortIds == null) {
      this.cohortIds = new ArrayList<Long>();
    }
    this.cohortIds.add(cohortIdsItem);
    return this;
  }

   /**
   * The ids of all cohorts in the data set 
   * @return cohortIds
  **/
  @ApiModelProperty(value = "The ids of all cohorts in the data set ")


  public List<Long> getCohortIds() {
    return cohortIds;
  }

  public void setCohortIds(List<Long> cohortIds) {
    this.cohortIds = cohortIds;
  }

  public DataSetRequest domainValuePairs(List<DomainValuePair> domainValuePairs) {
    this.domainValuePairs = domainValuePairs;
    return this;
  }

  public DataSetRequest addDomainValuePairsItem(DomainValuePair domainValuePairsItem) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSetRequest dataSetRequest = (DataSetRequest) o;
    return Objects.equals(this.name, dataSetRequest.name) &&
        Objects.equals(this.description, dataSetRequest.description) &&
        Objects.equals(this.etag, dataSetRequest.etag) &&
        Objects.equals(this.workspaceId, dataSetRequest.workspaceId) &&
        Objects.equals(this.includesAllParticipants, dataSetRequest.includesAllParticipants) &&
        Objects.equals(this.prePackagedConceptSet, dataSetRequest.prePackagedConceptSet) &&
        Objects.equals(this.conceptSetIds, dataSetRequest.conceptSetIds) &&
        Objects.equals(this.cohortIds, dataSetRequest.cohortIds) &&
        Objects.equals(this.domainValuePairs, dataSetRequest.domainValuePairs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, etag, workspaceId, includesAllParticipants, prePackagedConceptSet, conceptSetIds, cohortIds, domainValuePairs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataSetRequest {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    includesAllParticipants: ").append(toIndentedString(includesAllParticipants)).append("\n");
    sb.append("    prePackagedConceptSet: ").append(toIndentedString(prePackagedConceptSet)).append("\n");
    sb.append("    conceptSetIds: ").append(toIndentedString(conceptSetIds)).append("\n");
    sb.append("    cohortIds: ").append(toIndentedString(cohortIds)).append("\n");
    sb.append("    domainValuePairs: ").append(toIndentedString(domainValuePairs)).append("\n");
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

