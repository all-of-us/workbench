package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * PageVisit
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class PageVisit   {
  @JsonProperty("userId")
  private Long userId = null;

  @JsonProperty("page")
  private String page = null;

  @JsonProperty("firstVisit")
  private Long firstVisit = null;

  public PageVisit userId(Long userId) {
    this.userId = userId;
    return this;
  }

   /**
   * Get userId
   * @return userId
  **/
  @ApiModelProperty(value = "")


  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public PageVisit page(String page) {
    this.page = page;
    return this;
  }

   /**
   * Get page
   * @return page
  **/
  @ApiModelProperty(value = "")


  public String getPage() {
    return page;
  }

  public void setPage(String page) {
    this.page = page;
  }

  public PageVisit firstVisit(Long firstVisit) {
    this.firstVisit = firstVisit;
    return this;
  }

   /**
   * Get firstVisit
   * @return firstVisit
  **/
  @ApiModelProperty(value = "")


  public Long getFirstVisit() {
    return firstVisit;
  }

  public void setFirstVisit(Long firstVisit) {
    this.firstVisit = firstVisit;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageVisit pageVisit = (PageVisit) o;
    return Objects.equals(this.userId, pageVisit.userId) &&
        Objects.equals(this.page, pageVisit.page) &&
        Objects.equals(this.firstVisit, pageVisit.firstVisit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, page, firstVisit);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PageVisit {\n");
    
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    page: ").append(toIndentedString(page)).append("\n");
    sb.append("    firstVisit: ").append(toIndentedString(firstVisit)).append("\n");
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

