package org.pmiops.workbench.cohortreview.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.SortOrder;

/** PageRequest */
public class PageRequest {
  private Integer page;
  private Integer pageSize;
  private SortOrder sortOrder;
  private String sortColumn;
  private List<Filter> filters = new ArrayList<>();

  public PageRequest page(Integer page) {
    this.page = page;
    return this;
  }

  /**
   * the page
   *
   * @return page
   */
  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public PageRequest pageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * the page size.
   *
   * @return pageSize
   */
  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public PageRequest sortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Get sortOrder
   *
   * @return sortOrder
   */
  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  public PageRequest sortColumn(String sortColumn) {
    this.sortColumn = sortColumn;
    return this;
  }

  /**
   * sort column
   *
   * @return sortColumn
   */
  public String getSortColumn() {
    return sortColumn;
  }

  public void setSortColumn(String sortColumn) {
    this.sortColumn = sortColumn;
  }

  public PageRequest filters(List<Filter> filters) {
    this.filters = filters;
    return this;
  }

  /**
   * Get filters
   *
   * @return filters
   */
  public List<Filter> getFilters() {
    return filters;
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageRequest pageRequest = (PageRequest) o;
    return Objects.equals(this.page, pageRequest.page)
        && Objects.equals(this.pageSize, pageRequest.pageSize)
        && Objects.equals(this.sortOrder, pageRequest.sortOrder)
        && Objects.equals(this.sortColumn, pageRequest.sortColumn)
        && Objects.equals(this.filters, pageRequest.filters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(page, pageSize, sortOrder, sortColumn, filters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PageRequest {\n");

    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    sortColumn: ").append(toIndentedString(sortColumn)).append("\n");
    sb.append("    filters: ").append(toIndentedString(filters)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
