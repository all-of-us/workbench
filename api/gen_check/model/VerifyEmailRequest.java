package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * VerifyEmailRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public class VerifyEmailRequest   {
  @JsonProperty("emailToVerify")
  private String emailToVerify = null;

  @JsonProperty("username")
  private String username = null;

  public VerifyEmailRequest emailToVerify(String emailToVerify) {
    this.emailToVerify = emailToVerify;
    return this;
  }

   /**
   * Get emailToVerify
   * @return emailToVerify
  **/
  @ApiModelProperty(value = "")


  public String getEmailToVerify() {
    return emailToVerify;
  }

  public void setEmailToVerify(String emailToVerify) {
    this.emailToVerify = emailToVerify;
  }

  public VerifyEmailRequest username(String username) {
    this.username = username;
    return this;
  }

   /**
   * Get username
   * @return username
  **/
  @ApiModelProperty(value = "")


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VerifyEmailRequest verifyEmailRequest = (VerifyEmailRequest) o;
    return Objects.equals(this.emailToVerify, verifyEmailRequest.emailToVerify) &&
        Objects.equals(this.username, verifyEmailRequest.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailToVerify, username);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VerifyEmailRequest {\n");
    
    sb.append("    emailToVerify: ").append(toIndentedString(emailToVerify)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
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

