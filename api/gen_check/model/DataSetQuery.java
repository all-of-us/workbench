package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.NamedParameterEntry;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataSetQuery
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class DataSetQuery   {
  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("query")
  private String query = null;

  @JsonProperty("namedParameters")
  private List<NamedParameterEntry> namedParameters = null;

  public DataSetQuery domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Domain corresponding to an OMOP table that can contain rows for the concepts in this concept set. Note that the Domain values RACE, GENDER, and ETHNICITY are not allowed here; it makes sense to specify concepts in these domains in cohort criteria, but there isn't much value in having concept sets defined for them. 
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "Domain corresponding to an OMOP table that can contain rows for the concepts in this concept set. Note that the Domain values RACE, GENDER, and ETHNICITY are not allowed here; it makes sense to specify concepts in these domains in cohort criteria, but there isn't much value in having concept sets defined for them. ")
  @NotNull

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public DataSetQuery query(String query) {
    this.query = query;
    return this;
  }

   /**
   * The parameterized BigQuery SQL string to fetch the domain-specific subset of the data set from the CDR. 
   * @return query
  **/
  @ApiModelProperty(required = true, value = "The parameterized BigQuery SQL string to fetch the domain-specific subset of the data set from the CDR. ")
  @NotNull


  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public DataSetQuery namedParameters(List<NamedParameterEntry> namedParameters) {
    this.namedParameters = namedParameters;
    return this;
  }

  public DataSetQuery addNamedParametersItem(NamedParameterEntry namedParametersItem) {
    if (this.namedParameters == null) {
      this.namedParameters = new ArrayList<NamedParameterEntry>();
    }
    this.namedParameters.add(namedParametersItem);
    return this;
  }

   /**
   * The set of named parameters to use for the SQL query. 
   * @return namedParameters
  **/
  @ApiModelProperty(value = "The set of named parameters to use for the SQL query. ")

  @Valid

  public List<NamedParameterEntry> getNamedParameters() {
    return namedParameters;
  }

  public void setNamedParameters(List<NamedParameterEntry> namedParameters) {
    this.namedParameters = namedParameters;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSetQuery dataSetQuery = (DataSetQuery) o;
    return Objects.equals(this.domain, dataSetQuery.domain) &&
        Objects.equals(this.query, dataSetQuery.query) &&
        Objects.equals(this.namedParameters, dataSetQuery.namedParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, query, namedParameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataSetQuery {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    namedParameters: ").append(toIndentedString(namedParameters)).append("\n");
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

