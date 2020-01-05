package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.StandardFlag;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CriteriaMenuSubOption
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class CriteriaMenuSubOption   {
  @JsonProperty("type")
  private String type = null;

  @JsonProperty("standardFlags")
  private List<StandardFlag> standardFlags = new ArrayList<StandardFlag>();

  public CriteriaMenuSubOption type(String type) {
    this.type = type;
    return this;
  }

   /**
   * The criteria types that are searchable in Cohort Builder
   * @return type
  **/
  @ApiModelProperty(required = true, value = "The criteria types that are searchable in Cohort Builder")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public CriteriaMenuSubOption standardFlags(List<StandardFlag> standardFlags) {
    this.standardFlags = standardFlags;
    return this;
  }

  public CriteriaMenuSubOption addStandardFlagsItem(StandardFlag standardFlagsItem) {
    this.standardFlags.add(standardFlagsItem);
    return this;
  }

   /**
   * Get standardFlags
   * @return standardFlags
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<StandardFlag> getStandardFlags() {
    return standardFlags;
  }

  public void setStandardFlags(List<StandardFlag> standardFlags) {
    this.standardFlags = standardFlags;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CriteriaMenuSubOption criteriaMenuSubOption = (CriteriaMenuSubOption) o;
    return Objects.equals(this.type, criteriaMenuSubOption.type) &&
        Objects.equals(this.standardFlags, criteriaMenuSubOption.standardFlags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, standardFlags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CriteriaMenuSubOption {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    standardFlags: ").append(toIndentedString(standardFlags)).append("\n");
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

