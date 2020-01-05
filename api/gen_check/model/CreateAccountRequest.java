package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Profile;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CreateAccountRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class CreateAccountRequest   {
  @JsonProperty("profile")
  private Profile profile = null;

  @JsonProperty("invitationKey")
  private String invitationKey = null;

  public CreateAccountRequest profile(Profile profile) {
    this.profile = profile;
    return this;
  }

   /**
   * Get profile
   * @return profile
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Profile getProfile() {
    return profile;
  }

  public void setProfile(Profile profile) {
    this.profile = profile;
  }

  public CreateAccountRequest invitationKey(String invitationKey) {
    this.invitationKey = invitationKey;
    return this;
  }

   /**
   * Get invitationKey
   * @return invitationKey
  **/
  @ApiModelProperty(value = "")


  public String getInvitationKey() {
    return invitationKey;
  }

  public void setInvitationKey(String invitationKey) {
    this.invitationKey = invitationKey;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateAccountRequest createAccountRequest = (CreateAccountRequest) o;
    return Objects.equals(this.profile, createAccountRequest.profile) &&
        Objects.equals(this.invitationKey, createAccountRequest.invitationKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profile, invitationKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateAccountRequest {\n");
    
    sb.append("    profile: ").append(toIndentedString(profile)).append("\n");
    sb.append("    invitationKey: ").append(toIndentedString(invitationKey)).append("\n");
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

