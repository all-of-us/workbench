package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataSetPreviewRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class DataSetPreviewRequest   {
  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("includesAllParticipants")
  private Boolean includesAllParticipants = false;

  @JsonProperty("prePackagedConceptSet")
  private PrePackagedConceptSetEnum prePackagedConceptSet = null;

  @JsonProperty("conceptSetIds")
  private List<Long> conceptSetIds = null;

  @JsonProperty("cohortIds")
  private List<Long> cohortIds = null;

  @JsonProperty("values")
  private List<String> values = null;

  public DataSetPreviewRequest domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Get domain
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public DataSetPreviewRequest includesAllParticipants(Boolean includesAllParticipants) {
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

  public DataSetPreviewRequest prePackagedConceptSet(PrePackagedConceptSetEnum prePackagedConceptSet) {
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

  public DataSetPreviewRequest conceptSetIds(List<Long> conceptSetIds) {
    this.conceptSetIds = conceptSetIds;
    return this;
  }

  public DataSetPreviewRequest addConceptSetIdsItem(Long conceptSetIdsItem) {
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

  public DataSetPreviewRequest cohortIds(List<Long> cohortIds) {
    this.cohortIds = cohortIds;
    return this;
  }

  public DataSetPreviewRequest addCohortIdsItem(Long cohortIdsItem) {
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

  public DataSetPreviewRequest values(List<String> values) {
    this.values = values;
    return this;
  }

  public DataSetPreviewRequest addValuesItem(String valuesItem) {
    if (this.values == null) {
      this.values = new ArrayList<String>();
    }
    this.values.add(valuesItem);
    return this;
  }

   /**
   * All the selected values in the data set 
   * @return values
  **/
  @ApiModelProperty(value = "All the selected values in the data set ")


  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSetPreviewRequest dataSetPreviewRequest = (DataSetPreviewRequest) o;
    return Objects.equals(this.domain, dataSetPreviewRequest.domain) &&
        Objects.equals(this.includesAllParticipants, dataSetPreviewRequest.includesAllParticipants) &&
        Objects.equals(this.prePackagedConceptSet, dataSetPreviewRequest.prePackagedConceptSet) &&
        Objects.equals(this.conceptSetIds, dataSetPreviewRequest.conceptSetIds) &&
        Objects.equals(this.cohortIds, dataSetPreviewRequest.cohortIds) &&
        Objects.equals(this.values, dataSetPreviewRequest.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, includesAllParticipants, prePackagedConceptSet, conceptSetIds, cohortIds, values);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataSetPreviewRequest {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    includesAllParticipants: ").append(toIndentedString(includesAllParticipants)).append("\n");
    sb.append("    prePackagedConceptSet: ").append(toIndentedString(prePackagedConceptSet)).append("\n");
    sb.append("    conceptSetIds: ").append(toIndentedString(conceptSetIds)).append("\n");
    sb.append("    cohortIds: ").append(toIndentedString(cohortIds)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
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

