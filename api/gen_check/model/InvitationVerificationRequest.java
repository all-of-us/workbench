package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * InvitationVerificationRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class InvitationVerificationRequest   {
  @JsonProperty("invitationKey")
  private String invitationKey = null;

  public InvitationVerificationRequest invitationKey(String invitationKey) {
    this.invitationKey = invitationKey;
    return this;
  }

   /**
   * Invitation key for verification
   * @return invitationKey
  **/
  @ApiModelProperty(required = true, value = "Invitation key for verification")
  @NotNull


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
    InvitationVerificationRequest invitationVerificationRequest = (InvitationVerificationRequest) o;
    return Objects.equals(this.invitationKey, invitationVerificationRequest.invitationKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(invitationKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InvitationVerificationRequest {\n");
    
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

