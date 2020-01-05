package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.ClusterConfig;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Request to update the default cluster configuration for a given user. Fields of the config may be omitted, in which case a default will be used. Set clusterConfig to null to clear it. 
 */
@ApiModel(description = "Request to update the default cluster configuration for a given user. Fields of the config may be omitted, in which case a default will be used. Set clusterConfig to null to clear it. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class UpdateClusterConfigRequest   {
  @JsonProperty("userEmail")
  private String userEmail = null;

  @JsonProperty("clusterConfig")
  private ClusterConfig clusterConfig = null;

  public UpdateClusterConfigRequest userEmail(String userEmail) {
    this.userEmail = userEmail;
    return this;
  }

   /**
   * Get userEmail
   * @return userEmail
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  public UpdateClusterConfigRequest clusterConfig(ClusterConfig clusterConfig) {
    this.clusterConfig = clusterConfig;
    return this;
  }

   /**
   * Get clusterConfig
   * @return clusterConfig
  **/
  @ApiModelProperty(value = "")

  @Valid

  public ClusterConfig getClusterConfig() {
    return clusterConfig;
  }

  public void setClusterConfig(ClusterConfig clusterConfig) {
    this.clusterConfig = clusterConfig;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateClusterConfigRequest updateClusterConfigRequest = (UpdateClusterConfigRequest) o;
    return Objects.equals(this.userEmail, updateClusterConfigRequest.userEmail) &&
        Objects.equals(this.clusterConfig, updateClusterConfigRequest.clusterConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userEmail, clusterConfig);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateClusterConfigRequest {\n");
    
    sb.append("    userEmail: ").append(toIndentedString(userEmail)).append("\n");
    sb.append("    clusterConfig: ").append(toIndentedString(clusterConfig)).append("\n");
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

