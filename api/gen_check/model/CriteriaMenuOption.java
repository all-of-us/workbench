package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.CriteriaMenuSubOption;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CriteriaMenuOption
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class CriteriaMenuOption   {
  @JsonProperty("domain")
  private String domain = null;

  @JsonProperty("types")
  private List<CriteriaMenuSubOption> types = new ArrayList<CriteriaMenuSubOption>();

  public CriteriaMenuOption domain(String domain) {
    this.domain = domain;
    return this;
  }

   /**
   * The criteria domains that are searchable in Cohort Builder
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "The criteria domains that are searchable in Cohort Builder")
  @NotNull


  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public CriteriaMenuOption types(List<CriteriaMenuSubOption> types) {
    this.types = types;
    return this;
  }

  public CriteriaMenuOption addTypesItem(CriteriaMenuSubOption typesItem) {
    this.types.add(typesItem);
    return this;
  }

   /**
   * Get types
   * @return types
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<CriteriaMenuSubOption> getTypes() {
    return types;
  }

  public void setTypes(List<CriteriaMenuSubOption> types) {
    this.types = types;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CriteriaMenuOption criteriaMenuOption = (CriteriaMenuOption) o;
    return Objects.equals(this.domain, criteriaMenuOption.domain) &&
        Objects.equals(this.types, criteriaMenuOption.types);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, types);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CriteriaMenuOption {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    types: ").append(toIndentedString(types)).append("\n");
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

