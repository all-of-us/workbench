package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Operator;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A filter applied to the results of a query, based on a column value in a table. Only results matching the filter will be returned. One (and only one) of the value columns should be populated. values and valueNumbers should only be used in conjunction with the \&quot;in\&quot; operator. 
 */
@ApiModel(description = "A filter applied to the results of a query, based on a column value in a table. Only results matching the filter will be returned. One (and only one) of the value columns should be populated. values and valueNumbers should only be used in conjunction with the \"in\" operator. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class ColumnFilter   {
  @JsonProperty("columnName")
  private String columnName = null;

  @JsonProperty("operator")
  private Operator operator = null;

  @JsonProperty("value")
  private String value = null;

  @JsonProperty("values")
  private List<String> values = null;

  @JsonProperty("valueDate")
  private String valueDate = null;

  @JsonProperty("valueNumber")
  private BigDecimal valueNumber = null;

  @JsonProperty("valueNumbers")
  private List<BigDecimal> valueNumbers = null;

  @JsonProperty("valueNull")
  private Boolean valueNull = null;

  public ColumnFilter columnName(String columnName) {
    this.columnName = columnName;
    return this;
  }

   /**
   * The name of the column to filter on. 
   * @return columnName
  **/
  @ApiModelProperty(required = true, value = "The name of the column to filter on. ")
  @NotNull


  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public ColumnFilter operator(Operator operator) {
    this.operator = operator;
    return this;
  }

   /**
   * The operator to use when comparing values. Defaults to EQUAL. If the \"in\" operator is used, either values or valueNumbers should be populated. 
   * @return operator
  **/
  @ApiModelProperty(value = "The operator to use when comparing values. Defaults to EQUAL. If the \"in\" operator is used, either values or valueNumbers should be populated. ")

  @Valid

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public ColumnFilter value(String value) {
    this.value = value;
    return this;
  }

   /**
   * A string to use in comparisons (case-sensitive). 
   * @return value
  **/
  @ApiModelProperty(value = "A string to use in comparisons (case-sensitive). ")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ColumnFilter values(List<String> values) {
    this.values = values;
    return this;
  }

  public ColumnFilter addValuesItem(String valuesItem) {
    if (this.values == null) {
      this.values = new ArrayList<String>();
    }
    this.values.add(valuesItem);
    return this;
  }

   /**
   * An array of strings to use in comparisons (case-sensitive); used with the \"in\" operator. 
   * @return values
  **/
  @ApiModelProperty(value = "An array of strings to use in comparisons (case-sensitive); used with the \"in\" operator. ")


  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  public ColumnFilter valueDate(String valueDate) {
    this.valueDate = valueDate;
    return this;
  }

   /**
   * A date (yyyy-MM-dd) or datetime (yyyy-MM-dd HH:mm:ss zzz) value to use in comparisons. 
   * @return valueDate
  **/
  @ApiModelProperty(value = "A date (yyyy-MM-dd) or datetime (yyyy-MM-dd HH:mm:ss zzz) value to use in comparisons. ")


  public String getValueDate() {
    return valueDate;
  }

  public void setValueDate(String valueDate) {
    this.valueDate = valueDate;
  }

  public ColumnFilter valueNumber(BigDecimal valueNumber) {
    this.valueNumber = valueNumber;
    return this;
  }

   /**
   * A number to use in comparisons (either integer or floating point.) 
   * @return valueNumber
  **/
  @ApiModelProperty(value = "A number to use in comparisons (either integer or floating point.) ")

  @Valid

  public BigDecimal getValueNumber() {
    return valueNumber;
  }

  public void setValueNumber(BigDecimal valueNumber) {
    this.valueNumber = valueNumber;
  }

  public ColumnFilter valueNumbers(List<BigDecimal> valueNumbers) {
    this.valueNumbers = valueNumbers;
    return this;
  }

  public ColumnFilter addValueNumbersItem(BigDecimal valueNumbersItem) {
    if (this.valueNumbers == null) {
      this.valueNumbers = new ArrayList<BigDecimal>();
    }
    this.valueNumbers.add(valueNumbersItem);
    return this;
  }

   /**
   * An array of numbers to use in comparisons (used with the \"in\" operator) 
   * @return valueNumbers
  **/
  @ApiModelProperty(value = "An array of numbers to use in comparisons (used with the \"in\" operator) ")

  @Valid

  public List<BigDecimal> getValueNumbers() {
    return valueNumbers;
  }

  public void setValueNumbers(List<BigDecimal> valueNumbers) {
    this.valueNumbers = valueNumbers;
  }

  public ColumnFilter valueNull(Boolean valueNull) {
    this.valueNull = valueNull;
    return this;
  }

   /**
   * Set to true if the column value should be compared to null. 
   * @return valueNull
  **/
  @ApiModelProperty(value = "Set to true if the column value should be compared to null. ")


  public Boolean getValueNull() {
    return valueNull;
  }

  public void setValueNull(Boolean valueNull) {
    this.valueNull = valueNull;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ColumnFilter columnFilter = (ColumnFilter) o;
    return Objects.equals(this.columnName, columnFilter.columnName) &&
        Objects.equals(this.operator, columnFilter.operator) &&
        Objects.equals(this.value, columnFilter.value) &&
        Objects.equals(this.values, columnFilter.values) &&
        Objects.equals(this.valueDate, columnFilter.valueDate) &&
        Objects.equals(this.valueNumber, columnFilter.valueNumber) &&
        Objects.equals(this.valueNumbers, columnFilter.valueNumbers) &&
        Objects.equals(this.valueNull, columnFilter.valueNull);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, operator, value, values, valueDate, valueNumber, valueNumbers, valueNull);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ColumnFilter {\n");
    
    sb.append("    columnName: ").append(toIndentedString(columnName)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
    sb.append("    valueDate: ").append(toIndentedString(valueDate)).append("\n");
    sb.append("    valueNumber: ").append(toIndentedString(valueNumber)).append("\n");
    sb.append("    valueNumbers: ").append(toIndentedString(valueNumbers)).append("\n");
    sb.append("    valueNull: ").append(toIndentedString(valueNull)).append("\n");
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

