package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.AccessModule;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * AccessBypassRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class AccessBypassRequest   {
  @JsonProperty("moduleName")
  private AccessModule moduleName = null;

  @JsonProperty("isBypassed")
  private Boolean isBypassed = false;

  public AccessBypassRequest moduleName(AccessModule moduleName) {
    this.moduleName = moduleName;
    return this;
  }

   /**
   * Get moduleName
   * @return moduleName
  **/
  @ApiModelProperty(value = "")

  @Valid

  public AccessModule getModuleName() {
    return moduleName;
  }

  public void setModuleName(AccessModule moduleName) {
    this.moduleName = moduleName;
  }

  public AccessBypassRequest isBypassed(Boolean isBypassed) {
    this.isBypassed = isBypassed;
    return this;
  }

   /**
   * Get isBypassed
   * @return isBypassed
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getIsBypassed() {
    return isBypassed;
  }

  public void setIsBypassed(Boolean isBypassed) {
    this.isBypassed = isBypassed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccessBypassRequest accessBypassRequest = (AccessBypassRequest) o;
    return Objects.equals(this.moduleName, accessBypassRequest.moduleName) &&
        Objects.equals(this.isBypassed, accessBypassRequest.isBypassed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moduleName, isBypassed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccessBypassRequest {\n");
    
    sb.append("    moduleName: ").append(toIndentedString(moduleName)).append("\n");
    sb.append("    isBypassed: ").append(toIndentedString(isBypassed)).append("\n");
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

