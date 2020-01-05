package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.FileDetail;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * RecentResource
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class RecentResource   {
  @JsonProperty("workspaceId")
  private Long workspaceId = null;

  @JsonProperty("workspaceNamespace")
  private String workspaceNamespace = null;

  @JsonProperty("workspaceFirecloudName")
  private String workspaceFirecloudName = null;

  @JsonProperty("permission")
  private String permission = null;

  @JsonProperty("cohort")
  private Cohort cohort = null;

  @JsonProperty("cohortReview")
  private CohortReview cohortReview = null;

  @JsonProperty("notebook")
  private FileDetail notebook = null;

  @JsonProperty("conceptSet")
  private ConceptSet conceptSet = null;

  @JsonProperty("dataSet")
  private DataSet dataSet = null;

  @JsonProperty("modifiedTime")
  private String modifiedTime = null;

  public RecentResource workspaceId(Long workspaceId) {
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

  public RecentResource workspaceNamespace(String workspaceNamespace) {
    this.workspaceNamespace = workspaceNamespace;
    return this;
  }

   /**
   * Get workspaceNamespace
   * @return workspaceNamespace
  **/
  @ApiModelProperty(value = "")


  public String getWorkspaceNamespace() {
    return workspaceNamespace;
  }

  public void setWorkspaceNamespace(String workspaceNamespace) {
    this.workspaceNamespace = workspaceNamespace;
  }

  public RecentResource workspaceFirecloudName(String workspaceFirecloudName) {
    this.workspaceFirecloudName = workspaceFirecloudName;
    return this;
  }

   /**
   * Get workspaceFirecloudName
   * @return workspaceFirecloudName
  **/
  @ApiModelProperty(value = "")


  public String getWorkspaceFirecloudName() {
    return workspaceFirecloudName;
  }

  public void setWorkspaceFirecloudName(String workspaceFirecloudName) {
    this.workspaceFirecloudName = workspaceFirecloudName;
  }

  public RecentResource permission(String permission) {
    this.permission = permission;
    return this;
  }

   /**
   * Get permission
   * @return permission
  **/
  @ApiModelProperty(value = "")


  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public RecentResource cohort(Cohort cohort) {
    this.cohort = cohort;
    return this;
  }

   /**
   * Get cohort
   * @return cohort
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Cohort getCohort() {
    return cohort;
  }

  public void setCohort(Cohort cohort) {
    this.cohort = cohort;
  }

  public RecentResource cohortReview(CohortReview cohortReview) {
    this.cohortReview = cohortReview;
    return this;
  }

   /**
   * Get cohortReview
   * @return cohortReview
  **/
  @ApiModelProperty(value = "")

  @Valid

  public CohortReview getCohortReview() {
    return cohortReview;
  }

  public void setCohortReview(CohortReview cohortReview) {
    this.cohortReview = cohortReview;
  }

  public RecentResource notebook(FileDetail notebook) {
    this.notebook = notebook;
    return this;
  }

   /**
   * Get notebook
   * @return notebook
  **/
  @ApiModelProperty(value = "")

  @Valid

  public FileDetail getNotebook() {
    return notebook;
  }

  public void setNotebook(FileDetail notebook) {
    this.notebook = notebook;
  }

  public RecentResource conceptSet(ConceptSet conceptSet) {
    this.conceptSet = conceptSet;
    return this;
  }

   /**
   * Get conceptSet
   * @return conceptSet
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ConceptSet getConceptSet() {
    return conceptSet;
  }

  public void setConceptSet(ConceptSet conceptSet) {
    this.conceptSet = conceptSet;
  }

  public RecentResource dataSet(DataSet dataSet) {
    this.dataSet = dataSet;
    return this;
  }

   /**
   * Get dataSet
   * @return dataSet
  **/
  @ApiModelProperty(value = "")

  @Valid

  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  public RecentResource modifiedTime(String modifiedTime) {
    this.modifiedTime = modifiedTime;
    return this;
  }

   /**
   * Get modifiedTime
   * @return modifiedTime
  **/
  @ApiModelProperty(value = "")


  public String getModifiedTime() {
    return modifiedTime;
  }

  public void setModifiedTime(String modifiedTime) {
    this.modifiedTime = modifiedTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecentResource recentResource = (RecentResource) o;
    return Objects.equals(this.workspaceId, recentResource.workspaceId) &&
        Objects.equals(this.workspaceNamespace, recentResource.workspaceNamespace) &&
        Objects.equals(this.workspaceFirecloudName, recentResource.workspaceFirecloudName) &&
        Objects.equals(this.permission, recentResource.permission) &&
        Objects.equals(this.cohort, recentResource.cohort) &&
        Objects.equals(this.cohortReview, recentResource.cohortReview) &&
        Objects.equals(this.notebook, recentResource.notebook) &&
        Objects.equals(this.conceptSet, recentResource.conceptSet) &&
        Objects.equals(this.dataSet, recentResource.dataSet) &&
        Objects.equals(this.modifiedTime, recentResource.modifiedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceId, workspaceNamespace, workspaceFirecloudName, permission, cohort, cohortReview, notebook, conceptSet, dataSet, modifiedTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecentResource {\n");
    
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    workspaceNamespace: ").append(toIndentedString(workspaceNamespace)).append("\n");
    sb.append("    workspaceFirecloudName: ").append(toIndentedString(workspaceFirecloudName)).append("\n");
    sb.append("    permission: ").append(toIndentedString(permission)).append("\n");
    sb.append("    cohort: ").append(toIndentedString(cohort)).append("\n");
    sb.append("    cohortReview: ").append(toIndentedString(cohortReview)).append("\n");
    sb.append("    notebook: ").append(toIndentedString(notebook)).append("\n");
    sb.append("    conceptSet: ").append(toIndentedString(conceptSet)).append("\n");
    sb.append("    dataSet: ").append(toIndentedString(dataSet)).append("\n");
    sb.append("    modifiedTime: ").append(toIndentedString(modifiedTime)).append("\n");
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

