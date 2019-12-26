package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.StandardConceptFilter;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * SearchConceptsRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class SearchConceptsRequest   {
  @JsonProperty("query")
  private String query = null;

  @JsonProperty("standardConceptFilter")
  private StandardConceptFilter standardConceptFilter = null;

  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("maxResults")
  private Integer maxResults = null;

  @JsonProperty("includeDomainCounts")
  private Boolean includeDomainCounts = false;

  @JsonProperty("pageNumber")
  private Integer pageNumber = null;

  public SearchConceptsRequest query(String query) {
    this.query = query;
    return this;
  }

   /**
   * A query string that can be used to match a subset of the name (case-insensitively), the entire code value (case-insensitively), or the concept ID. If not specified, all concepts are returned. 
   * @return query
  **/
  @ApiModelProperty(value = "A query string that can be used to match a subset of the name (case-insensitively), the entire code value (case-insensitively), or the concept ID. If not specified, all concepts are returned. ")


  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public SearchConceptsRequest standardConceptFilter(StandardConceptFilter standardConceptFilter) {
    this.standardConceptFilter = standardConceptFilter;
    return this;
  }

   /**
   * STANDARD_CONCEPTS if only standard concepts should be returned, NON_STANDARD_CONCEPTS if only non-standard concepts should be returned; defaults to ALL_CONCEPTS, meaning both standard and non-standard concepts will be returned. 
   * @return standardConceptFilter
  **/
  @ApiModelProperty(required = true, value = "STANDARD_CONCEPTS if only standard concepts should be returned, NON_STANDARD_CONCEPTS if only non-standard concepts should be returned; defaults to ALL_CONCEPTS, meaning both standard and non-standard concepts will be returned. ")
  @NotNull

  @Valid

  public StandardConceptFilter getStandardConceptFilter() {
    return standardConceptFilter;
  }

  public void setStandardConceptFilter(StandardConceptFilter standardConceptFilter) {
    this.standardConceptFilter = standardConceptFilter;
  }

  public SearchConceptsRequest domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * The domain for the concepts returned (e.g. OBSERVATION, DRUG). Note that this may map to multiple domain ID values in OMOP. 
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "The domain for the concepts returned (e.g. OBSERVATION, DRUG). Note that this may map to multiple domain ID values in OMOP. ")
  @NotNull

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public SearchConceptsRequest maxResults(Integer maxResults) {
    this.maxResults = maxResults;
    return this;
  }

   /**
   * The maximum number of results returned. Defaults to 20.
   * @return maxResults
  **/
  @ApiModelProperty(value = "The maximum number of results returned. Defaults to 20.")


  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
  }

  public SearchConceptsRequest includeDomainCounts(Boolean includeDomainCounts) {
    this.includeDomainCounts = includeDomainCounts;
    return this;
  }

   /**
   * True if per-domain counts of concepts matching the criteria should be included in the response
   * @return includeDomainCounts
  **/
  @ApiModelProperty(required = true, value = "True if per-domain counts of concepts matching the criteria should be included in the response")
  @NotNull


  public Boolean getIncludeDomainCounts() {
    return includeDomainCounts;
  }

  public void setIncludeDomainCounts(Boolean includeDomainCounts) {
    this.includeDomainCounts = includeDomainCounts;
  }

  public SearchConceptsRequest pageNumber(Integer pageNumber) {
    this.pageNumber = pageNumber;
    return this;
  }

   /**
   * By default it returns the first page and then its next pages from that on.
   * @return pageNumber
  **/
  @ApiModelProperty(value = "By default it returns the first page and then its next pages from that on.")


  public Integer getPageNumber() {
    return pageNumber;
  }

  public void setPageNumber(Integer pageNumber) {
    this.pageNumber = pageNumber;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchConceptsRequest searchConceptsRequest = (SearchConceptsRequest) o;
    return Objects.equals(this.query, searchConceptsRequest.query) &&
        Objects.equals(this.standardConceptFilter, searchConceptsRequest.standardConceptFilter) &&
        Objects.equals(this.domain, searchConceptsRequest.domain) &&
        Objects.equals(this.maxResults, searchConceptsRequest.maxResults) &&
        Objects.equals(this.includeDomainCounts, searchConceptsRequest.includeDomainCounts) &&
        Objects.equals(this.pageNumber, searchConceptsRequest.pageNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(query, standardConceptFilter, domain, maxResults, includeDomainCounts, pageNumber);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchConceptsRequest {\n");
    
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    standardConceptFilter: ").append(toIndentedString(standardConceptFilter)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    maxResults: ").append(toIndentedString(maxResults)).append("\n");
    sb.append("    includeDomainCounts: ").append(toIndentedString(includeDomainCounts)).append("\n");
    sb.append("    pageNumber: ").append(toIndentedString(pageNumber)).append("\n");
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

