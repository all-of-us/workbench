package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.FilterList;
import org.pmiops.workbench.model.SortOrder;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * PageFilterRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class PageFilterRequest   {
  @JsonProperty("page")
  private Integer page = null;

  @JsonProperty("pageSize")
  private Integer pageSize = null;

  @JsonProperty("sortOrder")
  private SortOrder sortOrder = null;

  @JsonProperty("sortColumn")
  private FilterColumns sortColumn = null;

  @JsonProperty("filters")
  private FilterList filters = null;

  @JsonProperty("domain")
  private DomainType domain = null;

  public PageFilterRequest page(Integer page) {
    this.page = page;
    return this;
  }

   /**
   * specific page (default is 0)
   * @return page
  **/
  @ApiModelProperty(required = true, value = "specific page (default is 0)")
  @NotNull


  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public PageFilterRequest pageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

   /**
   * page size of results (default is 25)
   * @return pageSize
  **/
  @ApiModelProperty(required = true, value = "page size of results (default is 25)")
  @NotNull


  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public PageFilterRequest sortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

   /**
   * Get sortOrder
   * @return sortOrder
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  public PageFilterRequest sortColumn(FilterColumns sortColumn) {
    this.sortColumn = sortColumn;
    return this;
  }

   /**
   * Column upon which to sort (default is 'participantId')
   * @return sortColumn
  **/
  @ApiModelProperty(value = "Column upon which to sort (default is 'participantId')")

  @Valid

  public FilterColumns getSortColumn() {
    return sortColumn;
  }

  public void setSortColumn(FilterColumns sortColumn) {
    this.sortColumn = sortColumn;
  }

  public PageFilterRequest filters(FilterList filters) {
    this.filters = filters;
    return this;
  }

   /**
   * Get filters
   * @return filters
  **/
  @ApiModelProperty(value = "")

  @Valid

  public FilterList getFilters() {
    return filters;
  }

  public void setFilters(FilterList filters) {
    this.filters = filters;
  }

  public PageFilterRequest domain(DomainType domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Different domain types in omop
   * @return domain
  **/
  @ApiModelProperty(value = "Different domain types in omop")

  @Valid

  public DomainType getDomain() {
    return domain;
  }

  public void setDomain(DomainType domain) {
    this.domain = domain;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageFilterRequest pageFilterRequest = (PageFilterRequest) o;
    return Objects.equals(this.page, pageFilterRequest.page) &&
        Objects.equals(this.pageSize, pageFilterRequest.pageSize) &&
        Objects.equals(this.sortOrder, pageFilterRequest.sortOrder) &&
        Objects.equals(this.sortColumn, pageFilterRequest.sortColumn) &&
        Objects.equals(this.filters, pageFilterRequest.filters) &&
        Objects.equals(this.domain, pageFilterRequest.domain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(page, pageSize, sortOrder, sortColumn, filters, domain);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PageFilterRequest {\n");
    
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    sortColumn: ").append(toIndentedString(sortColumn)).append("\n");
    sb.append("    filters: ").append(toIndentedString(filters)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
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

