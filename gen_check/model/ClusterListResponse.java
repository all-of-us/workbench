package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Cluster;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ClusterListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class ClusterListResponse   {
  @JsonProperty("defaultCluster")
  private Cluster defaultCluster = null;

  public ClusterListResponse defaultCluster(Cluster defaultCluster) {
    this.defaultCluster = defaultCluster;
    return this;
  }

   /**
   * Get defaultCluster
   * @return defaultCluster
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public Cluster getDefaultCluster() {
    return defaultCluster;
  }

  public void setDefaultCluster(Cluster defaultCluster) {
    this.defaultCluster = defaultCluster;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterListResponse clusterListResponse = (ClusterListResponse) o;
    return Objects.equals(this.defaultCluster, clusterListResponse.defaultCluster);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultCluster);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterListResponse {\n");
    
    sb.append("    defaultCluster: ").append(toIndentedString(defaultCluster)).append("\n");
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

