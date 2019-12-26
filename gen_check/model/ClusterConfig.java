package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ClusterConfig
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class ClusterConfig   {
  @JsonProperty("masterDiskSize")
  private Integer masterDiskSize = null;

  @JsonProperty("machineType")
  private String machineType = null;

  public ClusterConfig masterDiskSize(Integer masterDiskSize) {
    this.masterDiskSize = masterDiskSize;
    return this;
  }

   /**
   * Master persistent disk size in GB.
   * @return masterDiskSize
  **/
  @ApiModelProperty(value = "Master persistent disk size in GB.")


  public Integer getMasterDiskSize() {
    return masterDiskSize;
  }

  public void setMasterDiskSize(Integer masterDiskSize) {
    this.masterDiskSize = masterDiskSize;
  }

  public ClusterConfig machineType(String machineType) {
    this.machineType = machineType;
    return this;
  }

   /**
   * GCE machine type, e.g. n1-standard-2.
   * @return machineType
  **/
  @ApiModelProperty(value = "GCE machine type, e.g. n1-standard-2.")


  public String getMachineType() {
    return machineType;
  }

  public void setMachineType(String machineType) {
    this.machineType = machineType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterConfig clusterConfig = (ClusterConfig) o;
    return Objects.equals(this.masterDiskSize, clusterConfig.masterDiskSize) &&
        Objects.equals(this.machineType, clusterConfig.machineType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(masterDiskSize, machineType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterConfig {\n");
    
    sb.append("    masterDiskSize: ").append(toIndentedString(masterDiskSize)).append("\n");
    sb.append("    machineType: ").append(toIndentedString(machineType)).append("\n");
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

