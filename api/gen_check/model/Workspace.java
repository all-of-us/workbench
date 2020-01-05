package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Workspace
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class Workspace   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("namespace")
  private String namespace = null;

  @JsonProperty("cdrVersionId")
  private String cdrVersionId = null;

  @JsonProperty("creator")
  private String creator = null;

  @JsonProperty("googleBucketName")
  private String googleBucketName = null;

  @JsonProperty("dataAccessLevel")
  private DataAccessLevel dataAccessLevel = null;

  @JsonProperty("researchPurpose")
  private ResearchPurpose researchPurpose = null;

  @JsonProperty("creationTime")
  private Long creationTime = null;

  @JsonProperty("lastModifiedTime")
  private Long lastModifiedTime = null;

  @JsonProperty("published")
  private Boolean published = false;

  public Workspace id(String id) {
    this.id = id;
    return this;
  }

   /**
   * Get id
   * @return id
  **/
  @ApiModelProperty(value = "")


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Workspace etag(String etag) {
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

  public Workspace name(String name) {
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

  public Workspace namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

   /**
   * Get namespace
   * @return namespace
  **/
  @ApiModelProperty(value = "")


  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public Workspace cdrVersionId(String cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
    return this;
  }

   /**
   * Get cdrVersionId
   * @return cdrVersionId
  **/
  @ApiModelProperty(value = "")


  public String getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(String cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  public Workspace creator(String creator) {
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

  public Workspace googleBucketName(String googleBucketName) {
    this.googleBucketName = googleBucketName;
    return this;
  }

   /**
   * Get googleBucketName
   * @return googleBucketName
  **/
  @ApiModelProperty(value = "")


  public String getGoogleBucketName() {
    return googleBucketName;
  }

  public void setGoogleBucketName(String googleBucketName) {
    this.googleBucketName = googleBucketName;
  }

  public Workspace dataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
    return this;
  }

   /**
   * Get dataAccessLevel
   * @return dataAccessLevel
  **/
  @ApiModelProperty(value = "")

  @Valid

  public DataAccessLevel getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  public Workspace researchPurpose(ResearchPurpose researchPurpose) {
    this.researchPurpose = researchPurpose;
    return this;
  }

   /**
   * Get researchPurpose
   * @return researchPurpose
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ResearchPurpose getResearchPurpose() {
    return researchPurpose;
  }

  public void setResearchPurpose(ResearchPurpose researchPurpose) {
    this.researchPurpose = researchPurpose;
  }

  public Workspace creationTime(Long creationTime) {
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

  public Workspace lastModifiedTime(Long lastModifiedTime) {
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

  public Workspace published(Boolean published) {
    this.published = published;
    return this;
  }

   /**
   * Get published
   * @return published
  **/
  @ApiModelProperty(value = "")


  public Boolean getPublished() {
    return published;
  }

  public void setPublished(Boolean published) {
    this.published = published;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Workspace workspace = (Workspace) o;
    return Objects.equals(this.id, workspace.id) &&
        Objects.equals(this.etag, workspace.etag) &&
        Objects.equals(this.name, workspace.name) &&
        Objects.equals(this.namespace, workspace.namespace) &&
        Objects.equals(this.cdrVersionId, workspace.cdrVersionId) &&
        Objects.equals(this.creator, workspace.creator) &&
        Objects.equals(this.googleBucketName, workspace.googleBucketName) &&
        Objects.equals(this.dataAccessLevel, workspace.dataAccessLevel) &&
        Objects.equals(this.researchPurpose, workspace.researchPurpose) &&
        Objects.equals(this.creationTime, workspace.creationTime) &&
        Objects.equals(this.lastModifiedTime, workspace.lastModifiedTime) &&
        Objects.equals(this.published, workspace.published);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, etag, name, namespace, cdrVersionId, creator, googleBucketName, dataAccessLevel, researchPurpose, creationTime, lastModifiedTime, published);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Workspace {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("    cdrVersionId: ").append(toIndentedString(cdrVersionId)).append("\n");
    sb.append("    creator: ").append(toIndentedString(creator)).append("\n");
    sb.append("    googleBucketName: ").append(toIndentedString(googleBucketName)).append("\n");
    sb.append("    dataAccessLevel: ").append(toIndentedString(dataAccessLevel)).append("\n");
    sb.append("    researchPurpose: ").append(toIndentedString(researchPurpose)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    lastModifiedTime: ").append(toIndentedString(lastModifiedTime)).append("\n");
    sb.append("    published: ").append(toIndentedString(published)).append("\n");
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

