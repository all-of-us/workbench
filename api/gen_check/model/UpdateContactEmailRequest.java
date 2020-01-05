package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * UpdateContactEmailRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class UpdateContactEmailRequest   {
  @JsonProperty("contactEmail")
  private String contactEmail = null;

  @JsonProperty("username")
  private String username = null;

  @JsonProperty("creationNonce")
  private String creationNonce = null;

  public UpdateContactEmailRequest contactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
    return this;
  }

   /**
   * Email to update contact email
   * @return contactEmail
  **/
  @ApiModelProperty(required = true, value = "Email to update contact email")
  @NotNull


  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public UpdateContactEmailRequest username(String username) {
    this.username = username;
    return this;
  }

   /**
   * Username for account.
   * @return username
  **/
  @ApiModelProperty(required = true, value = "Username for account.")
  @NotNull


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public UpdateContactEmailRequest creationNonce(String creationNonce) {
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
    UpdateContactEmailRequest updateContactEmailRequest = (UpdateContactEmailRequest) o;
    return Objects.equals(this.contactEmail, updateContactEmailRequest.contactEmail) &&
        Objects.equals(this.username, updateContactEmailRequest.username) &&
        Objects.equals(this.creationNonce, updateContactEmailRequest.creationNonce);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contactEmail, username, creationNonce);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateContactEmailRequest {\n");
    
    sb.append("    contactEmail: ").append(toIndentedString(contactEmail)).append("\n");
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

