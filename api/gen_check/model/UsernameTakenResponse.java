package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * UsernameTakenResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class UsernameTakenResponse   {
  @JsonProperty("isTaken")
  private Boolean isTaken = false;

  public UsernameTakenResponse isTaken(Boolean isTaken) {
    this.isTaken = isTaken;
    return this;
  }

   /**
   * Boolean response to whether username is already taken.
   * @return isTaken
  **/
  @ApiModelProperty(required = true, value = "Boolean response to whether username is already taken.")
  @NotNull


  public Boolean getIsTaken() {
    return isTaken;
  }

  public void setIsTaken(Boolean isTaken) {
    this.isTaken = isTaken;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UsernameTakenResponse usernameTakenResponse = (UsernameTakenResponse) o;
    return Objects.equals(this.isTaken, usernameTakenResponse.isTaken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isTaken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UsernameTakenResponse {\n");
    
    sb.append("    isTaken: ").append(toIndentedString(isTaken)).append("\n");
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

