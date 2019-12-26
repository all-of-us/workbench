package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.ClusterStatus;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A Firecloud notebook cluster.
 */
@ApiModel(description = "A Firecloud notebook cluster.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class Cluster   {
  @JsonProperty("clusterName")
  private String clusterName = null;

  @JsonProperty("clusterNamespace")
  private String clusterNamespace = null;

  @JsonProperty("status")
  private ClusterStatus status = null;

  @JsonProperty("createdDate")
  private String createdDate = null;

  public Cluster clusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

   /**
   * The user-supplied name for the cluster
   * @return clusterName
  **/
  @ApiModelProperty(required = true, value = "The user-supplied name for the cluster")
  @NotNull


  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public Cluster clusterNamespace(String clusterNamespace) {
    this.clusterNamespace = clusterNamespace;
    return this;
  }

   /**
   * The Google Project used to create the cluster
   * @return clusterNamespace
  **/
  @ApiModelProperty(required = true, value = "The Google Project used to create the cluster")
  @NotNull


  public String getClusterNamespace() {
    return clusterNamespace;
  }

  public void setClusterNamespace(String clusterNamespace) {
    this.clusterNamespace = clusterNamespace;
  }

  public Cluster status(ClusterStatus status) {
    this.status = status;
    return this;
  }

   /**
   * Get status
   * @return status
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public ClusterStatus getStatus() {
    return status;
  }

  public void setStatus(ClusterStatus status) {
    this.status = status;
  }

  public Cluster createdDate(String createdDate) {
    this.createdDate = createdDate;
    return this;
  }

   /**
   * The date and time the cluster was created, in ISO-8601 format
   * @return createdDate
  **/
  @ApiModelProperty(value = "The date and time the cluster was created, in ISO-8601 format")


  public String getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Cluster cluster = (Cluster) o;
    return Objects.equals(this.clusterName, cluster.clusterName) &&
        Objects.equals(this.clusterNamespace, cluster.clusterNamespace) &&
        Objects.equals(this.status, cluster.status) &&
        Objects.equals(this.createdDate, cluster.createdDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterName, clusterNamespace, status, createdDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Cluster {\n");
    
    sb.append("    clusterName: ").append(toIndentedString(clusterName)).append("\n");
    sb.append("    clusterNamespace: ").append(toIndentedString(clusterNamespace)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
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

