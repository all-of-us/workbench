package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ReviewStatus;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CohortReview
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class CohortReview   {
  @JsonProperty("cohortReviewId")
  private Long cohortReviewId = null;

  @JsonProperty("cohortId")
  private Long cohortId = null;

  @JsonProperty("cdrVersionId")
  private Long cdrVersionId = null;

  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("creationTime")
  private String creationTime = null;

  @JsonProperty("lastModifiedTime")
  private Long lastModifiedTime = null;

  @JsonProperty("cohortDefinition")
  private String cohortDefinition = null;

  @JsonProperty("cohortName")
  private String cohortName = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("matchedParticipantCount")
  private Long matchedParticipantCount = null;

  @JsonProperty("reviewSize")
  private Long reviewSize = null;

  @JsonProperty("reviewedCount")
  private Long reviewedCount = null;

  @JsonProperty("queryResultSize")
  private Long queryResultSize = null;

  @JsonProperty("reviewStatus")
  private ReviewStatus reviewStatus = null;

  @JsonProperty("participantCohortStatuses")
  private List<ParticipantCohortStatus> participantCohortStatuses = null;

  @JsonProperty("page")
  private Integer page = null;

  @JsonProperty("pageSize")
  private Integer pageSize = null;

  @JsonProperty("sortOrder")
  private String sortOrder = null;

  @JsonProperty("sortColumn")
  private String sortColumn = null;

  public CohortReview cohortReviewId(Long cohortReviewId) {
    this.cohortReviewId = cohortReviewId;
    return this;
  }

   /**
   * Get cohortReviewId
   * @return cohortReviewId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Long getCohortReviewId() {
    return cohortReviewId;
  }

  public void setCohortReviewId(Long cohortReviewId) {
    this.cohortReviewId = cohortReviewId;
  }

  public CohortReview cohortId(Long cohortId) {
    this.cohortId = cohortId;
    return this;
  }

   /**
   * Get cohortId
   * @return cohortId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Long getCohortId() {
    return cohortId;
  }

  public void setCohortId(Long cohortId) {
    this.cohortId = cohortId;
  }

  public CohortReview cdrVersionId(Long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
    return this;
  }

   /**
   * Get cdrVersionId
   * @return cdrVersionId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Long getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(Long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  public CohortReview etag(String etag) {
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

  public CohortReview creationTime(String creationTime) {
    this.creationTime = creationTime;
    return this;
  }

   /**
   * Get creationTime
   * @return creationTime
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  public CohortReview lastModifiedTime(Long lastModifiedTime) {
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

  public CohortReview cohortDefinition(String cohortDefinition) {
    this.cohortDefinition = cohortDefinition;
    return this;
  }

   /**
   * Get cohortDefinition
   * @return cohortDefinition
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getCohortDefinition() {
    return cohortDefinition;
  }

  public void setCohortDefinition(String cohortDefinition) {
    this.cohortDefinition = cohortDefinition;
  }

  public CohortReview cohortName(String cohortName) {
    this.cohortName = cohortName;
    return this;
  }

   /**
   * Get cohortName
   * @return cohortName
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getCohortName() {
    return cohortName;
  }

  public void setCohortName(String cohortName) {
    this.cohortName = cohortName;
  }

  public CohortReview description(String description) {
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

  public CohortReview matchedParticipantCount(Long matchedParticipantCount) {
    this.matchedParticipantCount = matchedParticipantCount;
    return this;
  }

   /**
   * Get matchedParticipantCount
   * @return matchedParticipantCount
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Long getMatchedParticipantCount() {
    return matchedParticipantCount;
  }

  public void setMatchedParticipantCount(Long matchedParticipantCount) {
    this.matchedParticipantCount = matchedParticipantCount;
  }

  public CohortReview reviewSize(Long reviewSize) {
    this.reviewSize = reviewSize;
    return this;
  }

   /**
   * Get reviewSize
   * @return reviewSize
  **/
  @ApiModelProperty(value = "")


  public Long getReviewSize() {
    return reviewSize;
  }

  public void setReviewSize(Long reviewSize) {
    this.reviewSize = reviewSize;
  }

  public CohortReview reviewedCount(Long reviewedCount) {
    this.reviewedCount = reviewedCount;
    return this;
  }

   /**
   * Get reviewedCount
   * @return reviewedCount
  **/
  @ApiModelProperty(value = "")


  public Long getReviewedCount() {
    return reviewedCount;
  }

  public void setReviewedCount(Long reviewedCount) {
    this.reviewedCount = reviewedCount;
  }

  public CohortReview queryResultSize(Long queryResultSize) {
    this.queryResultSize = queryResultSize;
    return this;
  }

   /**
   * Get queryResultSize
   * @return queryResultSize
  **/
  @ApiModelProperty(value = "")


  public Long getQueryResultSize() {
    return queryResultSize;
  }

  public void setQueryResultSize(Long queryResultSize) {
    this.queryResultSize = queryResultSize;
  }

  public CohortReview reviewStatus(ReviewStatus reviewStatus) {
    this.reviewStatus = reviewStatus;
    return this;
  }

   /**
   * Get reviewStatus
   * @return reviewStatus
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public ReviewStatus getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(ReviewStatus reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public CohortReview participantCohortStatuses(List<ParticipantCohortStatus> participantCohortStatuses) {
    this.participantCohortStatuses = participantCohortStatuses;
    return this;
  }

  public CohortReview addParticipantCohortStatusesItem(ParticipantCohortStatus participantCohortStatusesItem) {
    if (this.participantCohortStatuses == null) {
      this.participantCohortStatuses = new ArrayList<ParticipantCohortStatus>();
    }
    this.participantCohortStatuses.add(participantCohortStatusesItem);
    return this;
  }

   /**
   * Get participantCohortStatuses
   * @return participantCohortStatuses
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<ParticipantCohortStatus> getParticipantCohortStatuses() {
    return participantCohortStatuses;
  }

  public void setParticipantCohortStatuses(List<ParticipantCohortStatus> participantCohortStatuses) {
    this.participantCohortStatuses = participantCohortStatuses;
  }

  public CohortReview page(Integer page) {
    this.page = page;
    return this;
  }

   /**
   * Get page
   * @return page
  **/
  @ApiModelProperty(value = "")


  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public CohortReview pageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

   /**
   * Get pageSize
   * @return pageSize
  **/
  @ApiModelProperty(value = "")


  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public CohortReview sortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

   /**
   * Get sortOrder
   * @return sortOrder
  **/
  @ApiModelProperty(value = "")


  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }

  public CohortReview sortColumn(String sortColumn) {
    this.sortColumn = sortColumn;
    return this;
  }

   /**
   * Get sortColumn
   * @return sortColumn
  **/
  @ApiModelProperty(value = "")


  public String getSortColumn() {
    return sortColumn;
  }

  public void setSortColumn(String sortColumn) {
    this.sortColumn = sortColumn;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortReview cohortReview = (CohortReview) o;
    return Objects.equals(this.cohortReviewId, cohortReview.cohortReviewId) &&
        Objects.equals(this.cohortId, cohortReview.cohortId) &&
        Objects.equals(this.cdrVersionId, cohortReview.cdrVersionId) &&
        Objects.equals(this.etag, cohortReview.etag) &&
        Objects.equals(this.creationTime, cohortReview.creationTime) &&
        Objects.equals(this.lastModifiedTime, cohortReview.lastModifiedTime) &&
        Objects.equals(this.cohortDefinition, cohortReview.cohortDefinition) &&
        Objects.equals(this.cohortName, cohortReview.cohortName) &&
        Objects.equals(this.description, cohortReview.description) &&
        Objects.equals(this.matchedParticipantCount, cohortReview.matchedParticipantCount) &&
        Objects.equals(this.reviewSize, cohortReview.reviewSize) &&
        Objects.equals(this.reviewedCount, cohortReview.reviewedCount) &&
        Objects.equals(this.queryResultSize, cohortReview.queryResultSize) &&
        Objects.equals(this.reviewStatus, cohortReview.reviewStatus) &&
        Objects.equals(this.participantCohortStatuses, cohortReview.participantCohortStatuses) &&
        Objects.equals(this.page, cohortReview.page) &&
        Objects.equals(this.pageSize, cohortReview.pageSize) &&
        Objects.equals(this.sortOrder, cohortReview.sortOrder) &&
        Objects.equals(this.sortColumn, cohortReview.sortColumn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortReviewId, cohortId, cdrVersionId, etag, creationTime, lastModifiedTime, cohortDefinition, cohortName, description, matchedParticipantCount, reviewSize, reviewedCount, queryResultSize, reviewStatus, participantCohortStatuses, page, pageSize, sortOrder, sortColumn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortReview {\n");
    
    sb.append("    cohortReviewId: ").append(toIndentedString(cohortReviewId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    cdrVersionId: ").append(toIndentedString(cdrVersionId)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    lastModifiedTime: ").append(toIndentedString(lastModifiedTime)).append("\n");
    sb.append("    cohortDefinition: ").append(toIndentedString(cohortDefinition)).append("\n");
    sb.append("    cohortName: ").append(toIndentedString(cohortName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    matchedParticipantCount: ").append(toIndentedString(matchedParticipantCount)).append("\n");
    sb.append("    reviewSize: ").append(toIndentedString(reviewSize)).append("\n");
    sb.append("    reviewedCount: ").append(toIndentedString(reviewedCount)).append("\n");
    sb.append("    queryResultSize: ").append(toIndentedString(queryResultSize)).append("\n");
    sb.append("    reviewStatus: ").append(toIndentedString(reviewStatus)).append("\n");
    sb.append("    participantCohortStatuses: ").append(toIndentedString(participantCohortStatuses)).append("\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    sortColumn: ").append(toIndentedString(sortColumn)).append("\n");
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

