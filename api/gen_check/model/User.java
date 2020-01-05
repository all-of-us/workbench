package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * User
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class User   {
  @JsonProperty("email")
  private String email = null;

  @JsonProperty("userName")
  private String userName = null;

  @JsonProperty("givenName")
  private String givenName = null;

  @JsonProperty("familyName")
  private String familyName = null;

  public User email(String email) {
    this.email = email;
    return this;
  }

   /**
   * researchallofus email address (deprecated in favor of userName)
   * @return email
  **/
  @ApiModelProperty(value = "researchallofus email address (deprecated in favor of userName)")


  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public User userName(String userName) {
    this.userName = userName;
    return this;
  }

   /**
   * Unique researchallofus username (a Google account email)
   * @return userName
  **/
  @ApiModelProperty(value = "Unique researchallofus username (a Google account email)")


  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public User givenName(String givenName) {
    this.givenName = givenName;
    return this;
  }

   /**
   * the user's given name (e.g. Alice)
   * @return givenName
  **/
  @ApiModelProperty(value = "the user's given name (e.g. Alice)")


  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public User familyName(String familyName) {
    this.familyName = familyName;
    return this;
  }

   /**
   * the user's family  name (e.g. Jones)
   * @return familyName
  **/
  @ApiModelProperty(value = "the user's family  name (e.g. Jones)")


  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(this.email, user.email) &&
        Objects.equals(this.userName, user.userName) &&
        Objects.equals(this.givenName, user.givenName) &&
        Objects.equals(this.familyName, user.familyName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, userName, givenName, familyName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class User {\n");
    
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    givenName: ").append(toIndentedString(givenName)).append("\n");
    sb.append("    familyName: ").append(toIndentedString(familyName)).append("\n");
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

