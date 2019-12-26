package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CdrVersion
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class CdrVersion   {
  @JsonProperty("cdrVersionId")
  private String cdrVersionId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("dataAccessLevel")
  private DataAccessLevel dataAccessLevel = null;

  @JsonProperty("archivalStatus")
  private ArchivalStatus archivalStatus = null;

  @JsonProperty("creationTime")
  private Long creationTime = null;

  public CdrVersion cdrVersionId(String cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
    return this;
  }

   /**
   * Get cdrVersionId
   * @return cdrVersionId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(String cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  public CdrVersion name(String name) {
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

  public CdrVersion dataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
    return this;
  }

   /**
   * Get dataAccessLevel
   * @return dataAccessLevel
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public DataAccessLevel getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  public CdrVersion archivalStatus(ArchivalStatus archivalStatus) {
    this.archivalStatus = archivalStatus;
    return this;
  }

   /**
   * Get archivalStatus
   * @return archivalStatus
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ArchivalStatus getArchivalStatus() {
    return archivalStatus;
  }

  public void setArchivalStatus(ArchivalStatus archivalStatus) {
    this.archivalStatus = archivalStatus;
  }

  public CdrVersion creationTime(Long creationTime) {
    this.creationTime = creationTime;
    return this;
  }

   /**
   * Milliseconds since the UNIX epoch.
   * @return creationTime
  **/
  @ApiModelProperty(required = true, value = "Milliseconds since the UNIX epoch.")
  @NotNull


  public Long getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Long creationTime) {
    this.creationTime = creationTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CdrVersion cdrVersion = (CdrVersion) o;
    return Objects.equals(this.cdrVersionId, cdrVersion.cdrVersionId) &&
        Objects.equals(this.name, cdrVersion.name) &&
        Objects.equals(this.dataAccessLevel, cdrVersion.dataAccessLevel) &&
        Objects.equals(this.archivalStatus, cdrVersion.archivalStatus) &&
        Objects.equals(this.creationTime, cdrVersion.creationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cdrVersionId, name, dataAccessLevel, archivalStatus, creationTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CdrVersion {\n");
    
    sb.append("    cdrVersionId: ").append(toIndentedString(cdrVersionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    dataAccessLevel: ").append(toIndentedString(dataAccessLevel)).append("\n");
    sb.append("    archivalStatus: ").append(toIndentedString(archivalStatus)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
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

