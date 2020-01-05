package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DuplicateCohortRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public class DuplicateCohortRequest   {
  @JsonProperty("originalCohortId")
  private Long originalCohortId = null;

  @JsonProperty("newName")
  private String newName = null;

  public DuplicateCohortRequest originalCohortId(Long originalCohortId) {
    this.originalCohortId = originalCohortId;
    return this;
  }

   /**
   * Get originalCohortId
   * @return originalCohortId
  **/
  @ApiModelProperty(value = "")


  public Long getOriginalCohortId() {
    return originalCohortId;
  }

  public void setOriginalCohortId(Long originalCohortId) {
    this.originalCohortId = originalCohortId;
  }

  public DuplicateCohortRequest newName(String newName) {
    this.newName = newName;
    return this;
  }

   /**
   * Get newName
   * @return newName
  **/
  @ApiModelProperty(value = "")


  public String getNewName() {
    return newName;
  }

  public void setNewName(String newName) {
    this.newName = newName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DuplicateCohortRequest duplicateCohortRequest = (DuplicateCohortRequest) o;
    return Objects.equals(this.originalCohortId, duplicateCohortRequest.originalCohortId) &&
        Objects.equals(this.newName, duplicateCohortRequest.newName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(originalCohortId, newName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DuplicateCohortRequest {\n");
    
    sb.append("    originalCohortId: ").append(toIndentedString(originalCohortId)).append("\n");
    sb.append("    newName: ").append(toIndentedString(newName)).append("\n");
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

