package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * FileDetail
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class FileDetail   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("path")
  private String path = null;

  @JsonProperty("lastModifiedTime")
  private Long lastModifiedTime = null;

  public FileDetail name(String name) {
    this.name = name;
    return this;
  }

   /**
   * File Name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "File Name")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FileDetail path(String path) {
    this.path = path;
    return this;
  }

   /**
   * The path is in format of gs://bucket-name/name
   * @return path
  **/
  @ApiModelProperty(required = true, value = "The path is in format of gs://bucket-name/name")
  @NotNull


  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public FileDetail lastModifiedTime(Long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

   /**
   * Milliseconds since the UNIX epoch.
   * @return lastModifiedTime
  **/
  @ApiModelProperty(required = true, value = "Milliseconds since the UNIX epoch.")
  @NotNull


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
    FileDetail fileDetail = (FileDetail) o;
    return Objects.equals(this.name, fileDetail.name) &&
        Objects.equals(this.path, fileDetail.path) &&
        Objects.equals(this.lastModifiedTime, fileDetail.lastModifiedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, path, lastModifiedTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileDetail {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
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

