package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Profile;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * UserListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class UserListResponse   {
  @JsonProperty("profileList")
  private List<Profile> profileList = new ArrayList<Profile>();

  public UserListResponse profileList(List<Profile> profileList) {
    this.profileList = profileList;
    return this;
  }

  public UserListResponse addProfileListItem(Profile profileListItem) {
    this.profileList.add(profileListItem);
    return this;
  }

   /**
   * Get profileList
   * @return profileList
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<Profile> getProfileList() {
    return profileList;
  }

  public void setProfileList(List<Profile> profileList) {
    this.profileList = profileList;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserListResponse userListResponse = (UserListResponse) o;
    return Objects.equals(this.profileList, userListResponse.profileList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profileList);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserListResponse {\n");
    
    sb.append("    profileList: ").append(toIndentedString(profileList)).append("\n");
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

