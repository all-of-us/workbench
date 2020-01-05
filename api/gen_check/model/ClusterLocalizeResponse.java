package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ClusterLocalizeResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class ClusterLocalizeResponse   {
  @JsonProperty("clusterLocalDirectory")
  private String clusterLocalDirectory = null;

  public ClusterLocalizeResponse clusterLocalDirectory(String clusterLocalDirectory) {
    this.clusterLocalDirectory = clusterLocalDirectory;
    return this;
  }

   /**
   * The directory on the notebook cluster file system where the requested files were localized. This is the \"API\" directory in Jupyter terms, which means it is a relative path in the Jupyter user-facing file system, e.g. \"foo/bar.ipynb\". 
   * @return clusterLocalDirectory
  **/
  @ApiModelProperty(required = true, value = "The directory on the notebook cluster file system where the requested files were localized. This is the \"API\" directory in Jupyter terms, which means it is a relative path in the Jupyter user-facing file system, e.g. \"foo/bar.ipynb\". ")
  @NotNull


  public String getClusterLocalDirectory() {
    return clusterLocalDirectory;
  }

  public void setClusterLocalDirectory(String clusterLocalDirectory) {
    this.clusterLocalDirectory = clusterLocalDirectory;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterLocalizeResponse clusterLocalizeResponse = (ClusterLocalizeResponse) o;
    return Objects.equals(this.clusterLocalDirectory, clusterLocalizeResponse.clusterLocalDirectory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterLocalDirectory);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterLocalizeResponse {\n");
    
    sb.append("    clusterLocalDirectory: ").append(toIndentedString(clusterLocalDirectory)).append("\n");
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

