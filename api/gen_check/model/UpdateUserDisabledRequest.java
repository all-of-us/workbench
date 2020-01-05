package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * UpdateUserDisabledRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class UpdateUserDisabledRequest   {
  @JsonProperty("email")
  private String email = null;

  @JsonProperty("disabled")
  private Boolean disabled = false;

  public UpdateUserDisabledRequest email(String email) {
    this.email = email;
    return this;
  }

   /**
   * Get email
   * @return email
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UpdateUserDisabledRequest disabled(Boolean disabled) {
    this.disabled = disabled;
    return this;
  }

   /**
   * Set to true to disable user in auth domain, false otherwise
   * @return disabled
  **/
  @ApiModelProperty(value = "Set to true to disable user in auth domain, false otherwise")


  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateUserDisabledRequest updateUserDisabledRequest = (UpdateUserDisabledRequest) o;
    return Objects.equals(this.email, updateUserDisabledRequest.email) &&
        Objects.equals(this.disabled, updateUserDisabledRequest.disabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, disabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateUserDisabledRequest {\n");
    
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    disabled: ").append(toIndentedString(disabled)).append("\n");
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

