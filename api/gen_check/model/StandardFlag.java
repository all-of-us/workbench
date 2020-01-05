package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * StandardFlag
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class StandardFlag   {
  @JsonProperty("standard")
  private Boolean standard = false;

  public StandardFlag standard(Boolean standard) {
    this.standard = standard;
    return this;
  }

   /**
   * flag that determines standard or source
   * @return standard
  **/
  @ApiModelProperty(required = true, value = "flag that determines standard or source")
  @NotNull


  public Boolean getStandard() {
    return standard;
  }

  public void setStandard(Boolean standard) {
    this.standard = standard;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StandardFlag standardFlag = (StandardFlag) o;
    return Objects.equals(this.standard, standardFlag.standard);
  }

  @Override
  public int hashCode() {
    return Objects.hash(standard);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StandardFlag {\n");
    
    sb.append("    standard: ").append(toIndentedString(standard)).append("\n");
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

