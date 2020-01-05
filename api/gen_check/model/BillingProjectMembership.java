package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.BillingProjectStatus;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class BillingProjectMembership   {
  @JsonProperty("projectName")
  private String projectName = null;

  @JsonProperty("role")
  private String role = null;

  @JsonProperty("status")
  private BillingProjectStatus status = null;

  public BillingProjectMembership projectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

   /**
   * the name of the project
   * @return projectName
  **/
  @ApiModelProperty(required = true, value = "the name of the project")
  @NotNull


  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public BillingProjectMembership role(String role) {
    this.role = role;
    return this;
  }

   /**
   * the role of the current user in the project
   * @return role
  **/
  @ApiModelProperty(required = true, value = "the role of the current user in the project")
  @NotNull


  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public BillingProjectMembership status(BillingProjectStatus status) {
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

  public BillingProjectStatus getStatus() {
    return status;
  }

  public void setStatus(BillingProjectStatus status) {
    this.status = status;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BillingProjectMembership billingProjectMembership = (BillingProjectMembership) o;
    return Objects.equals(this.projectName, billingProjectMembership.projectName) &&
        Objects.equals(this.role, billingProjectMembership.role) &&
        Objects.equals(this.status, billingProjectMembership.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectName, role, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BillingProjectMembership {\n");
    
    sb.append("    projectName: ").append(toIndentedString(projectName)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

