package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ResendWelcomeEmailRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class ResendWelcomeEmailRequest   {
  @JsonProperty("username")
  private String username = null;

  @JsonProperty("creationNonce")
  private String creationNonce = null;

  public ResendWelcomeEmailRequest username(String username) {
    this.username = username;
    return this;
  }

   /**
   * Username of account to resend welcome email to
   * @return username
  **/
  @ApiModelProperty(required = true, value = "Username of account to resend welcome email to")
  @NotNull


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public ResendWelcomeEmailRequest creationNonce(String creationNonce) {
    this.creationNonce = creationNonce;
    return this;
  }

   /**
   * The nonce returned from the account creation API.
   * @return creationNonce
  **/
  @ApiModelProperty(required = true, value = "The nonce returned from the account creation API.")
  @NotNull


  public String getCreationNonce() {
    return creationNonce;
  }

  public void setCreationNonce(String creationNonce) {
    this.creationNonce = creationNonce;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResendWelcomeEmailRequest resendWelcomeEmailRequest = (ResendWelcomeEmailRequest) o;
    return Objects.equals(this.username, resendWelcomeEmailRequest.username) &&
        Objects.equals(this.creationNonce, resendWelcomeEmailRequest.creationNonce);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, creationNonce);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResendWelcomeEmailRequest {\n");
    
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    creationNonce: ").append(toIndentedString(creationNonce)).append("\n");
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

