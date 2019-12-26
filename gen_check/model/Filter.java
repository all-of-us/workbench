package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.Operator;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Filter
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class Filter   {
  @JsonProperty("property")
  private FilterColumns property = null;

  @JsonProperty("operator")
  private Operator operator = null;

  @JsonProperty("values")
  private List<String> values = new ArrayList<String>();

  public Filter property(FilterColumns property) {
    this.property = property;
    return this;
  }

   /**
   * Get property
   * @return property
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public FilterColumns getProperty() {
    return property;
  }

  public void setProperty(FilterColumns property) {
    this.property = property;
  }

  public Filter operator(Operator operator) {
    this.operator = operator;
    return this;
  }

   /**
   * Get operator
   * @return operator
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public Filter values(List<String> values) {
    this.values = values;
    return this;
  }

  public Filter addValuesItem(String valuesItem) {
    this.values.add(valuesItem);
    return this;
  }

   /**
   * Get values
   * @return values
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
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
    Filter filter = (Filter) o;
    return Objects.equals(this.property, filter.property) &&
        Objects.equals(this.operator, filter.operator) &&
        Objects.equals(this.values, filter.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(property, operator, values);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Filter {\n");
    
    sb.append("    property: ").append(toIndentedString(property)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
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

