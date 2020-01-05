package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.NonAcademicAffiliation;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * InstitutionalAffiliation
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class InstitutionalAffiliation   {
  @JsonProperty("institution")
  private String institution = null;

  @JsonProperty("role")
  private String role = null;

  @JsonProperty("nonAcademicAffiliation")
  private NonAcademicAffiliation nonAcademicAffiliation = null;

  @JsonProperty("other")
  private String other = null;

  public InstitutionalAffiliation institution(String institution) {
    this.institution = institution;
    return this;
  }

   /**
   * Get institution
   * @return institution
  **/
  @ApiModelProperty(value = "")


  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public InstitutionalAffiliation role(String role) {
    this.role = role;
    return this;
  }

   /**
   * Get role
   * @return role
  **/
  @ApiModelProperty(value = "")


  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public InstitutionalAffiliation nonAcademicAffiliation(NonAcademicAffiliation nonAcademicAffiliation) {
    this.nonAcademicAffiliation = nonAcademicAffiliation;
    return this;
  }

   /**
   * Get nonAcademicAffiliation
   * @return nonAcademicAffiliation
  **/
  @ApiModelProperty(value = "")

  @Valid

  public NonAcademicAffiliation getNonAcademicAffiliation() {
    return nonAcademicAffiliation;
  }

  public void setNonAcademicAffiliation(NonAcademicAffiliation nonAcademicAffiliation) {
    this.nonAcademicAffiliation = nonAcademicAffiliation;
  }

  public InstitutionalAffiliation other(String other) {
    this.other = other;
    return this;
  }

   /**
   * Get other
   * @return other
  **/
  @ApiModelProperty(value = "")


  public String getOther() {
    return other;
  }

  public void setOther(String other) {
    this.other = other;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstitutionalAffiliation institutionalAffiliation = (InstitutionalAffiliation) o;
    return Objects.equals(this.institution, institutionalAffiliation.institution) &&
        Objects.equals(this.role, institutionalAffiliation.role) &&
        Objects.equals(this.nonAcademicAffiliation, institutionalAffiliation.nonAcademicAffiliation) &&
        Objects.equals(this.other, institutionalAffiliation.other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institution, role, nonAcademicAffiliation, other);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InstitutionalAffiliation {\n");
    
    sb.append("    institution: ").append(toIndentedString(institution)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    nonAcademicAffiliation: ").append(toIndentedString(nonAcademicAffiliation)).append("\n");
    sb.append("    other: ").append(toIndentedString(other)).append("\n");
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

