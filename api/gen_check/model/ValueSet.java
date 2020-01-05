package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuesResponse;
import org.pmiops.workbench.model.Surveys;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ValueSet
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class ValueSet   {
  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("survey")
  private Surveys survey = null;

  @JsonProperty("values")
  private DomainValuesResponse values = null;

  public ValueSet domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Domain corresponding to an OMOP table. 
   * @return domain
  **/
  @ApiModelProperty(value = "Domain corresponding to an OMOP table. ")

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public ValueSet survey(Surveys survey) {
    this.survey = survey;
    return this;
  }

   /**
   * Survey corresponding to an OMOP table. 
   * @return survey
  **/
  @ApiModelProperty(value = "Survey corresponding to an OMOP table. ")

  @Valid

  public Surveys getSurvey() {
    return survey;
  }

  public void setSurvey(Surveys survey) {
    this.survey = survey;
  }

  public ValueSet values(DomainValuesResponse values) {
    this.values = values;
    return this;
  }

   /**
   * Get values
   * @return values
  **/
  @ApiModelProperty(value = "")

  @Valid

  public DomainValuesResponse getValues() {
    return values;
  }

  public void setValues(DomainValuesResponse values) {
    this.values = values;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValueSet valueSet = (ValueSet) o;
    return Objects.equals(this.domain, valueSet.domain) &&
        Objects.equals(this.survey, valueSet.survey) &&
        Objects.equals(this.values, valueSet.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, survey, values);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValueSet {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    survey: ").append(toIndentedString(survey)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
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

