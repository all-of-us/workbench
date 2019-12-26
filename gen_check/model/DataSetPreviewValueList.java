package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataSetPreviewValueList
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class DataSetPreviewValueList   {
  @JsonProperty("value")
  private String value = null;

  @JsonProperty("queryValue")
  private List<String> queryValue = null;

  public DataSetPreviewValueList value(String value) {
    this.value = value;
    return this;
  }

   /**
   * Value selected by user which will act as column header in preview table
   * @return value
  **/
  @ApiModelProperty(value = "Value selected by user which will act as column header in preview table")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public DataSetPreviewValueList queryValue(List<String> queryValue) {
    this.queryValue = queryValue;
    return this;
  }

  public DataSetPreviewValueList addQueryValueItem(String queryValueItem) {
    if (this.queryValue == null) {
      this.queryValue = new ArrayList<String>();
    }
    this.queryValue.add(queryValueItem);
    return this;
  }

   /**
   * Get queryValue
   * @return queryValue
  **/
  @ApiModelProperty(value = "")


  public List<String> getQueryValue() {
    return queryValue;
  }

  public void setQueryValue(List<String> queryValue) {
    this.queryValue = queryValue;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSetPreviewValueList dataSetPreviewValueList = (DataSetPreviewValueList) o;
    return Objects.equals(this.value, dataSetPreviewValueList.value) &&
        Objects.equals(this.queryValue, dataSetPreviewValueList.queryValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, queryValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataSetPreviewValueList {\n");
    
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    queryValue: ").append(toIndentedString(queryValue)).append("\n");
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

