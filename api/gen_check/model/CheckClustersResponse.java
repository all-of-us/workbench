package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CheckClustersResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class CheckClustersResponse   {
  @JsonProperty("clusterDeletionCount")
  private Integer clusterDeletionCount = null;

  public CheckClustersResponse clusterDeletionCount(Integer clusterDeletionCount) {
    this.clusterDeletionCount = clusterDeletionCount;
    return this;
  }

   /**
   * Number of clusters deleted during the check. 
   * @return clusterDeletionCount
  **/
  @ApiModelProperty(value = "Number of clusters deleted during the check. ")


  public Integer getClusterDeletionCount() {
    return clusterDeletionCount;
  }

  public void setClusterDeletionCount(Integer clusterDeletionCount) {
    this.clusterDeletionCount = clusterDeletionCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckClustersResponse checkClustersResponse = (CheckClustersResponse) o;
    return Objects.equals(this.clusterDeletionCount, checkClustersResponse.clusterDeletionCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterDeletionCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckClustersResponse {\n");
    
    sb.append("    clusterDeletionCount: ").append(toIndentedString(clusterDeletionCount)).append("\n");
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

