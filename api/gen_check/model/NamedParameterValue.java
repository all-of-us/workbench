package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * NamedParameterValue
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class NamedParameterValue   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("parameterType")
  private String parameterType = null;

  @JsonProperty("arrayType")
  private String arrayType = null;

  @JsonProperty("parameterValue")
  private Object parameterValue = null;

  public NamedParameterValue name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public NamedParameterValue parameterType(String parameterType) {
    this.parameterType = parameterType;
    return this;
  }

   /**
   * Should be any parameter allowed by bigquery, with the exception of struct. The list of parameter types can be found here: https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types 
   * @return parameterType
  **/
  @ApiModelProperty(required = true, value = "Should be any parameter allowed by bigquery, with the exception of struct. The list of parameter types can be found here: https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types ")
  @NotNull


  public String getParameterType() {
    return parameterType;
  }

  public void setParameterType(String parameterType) {
    this.parameterType = parameterType;
  }

  public NamedParameterValue arrayType(String arrayType) {
    this.arrayType = arrayType;
    return this;
  }

   /**
   * Get arrayType
   * @return arrayType
  **/
  @ApiModelProperty(value = "")


  public String getArrayType() {
    return arrayType;
  }

  public void setArrayType(String arrayType) {
    this.arrayType = arrayType;
  }

  public NamedParameterValue parameterValue(Object parameterValue) {
    this.parameterValue = parameterValue;
    return this;
  }

   /**
   * Can be any value
   * @return parameterValue
  **/
  @ApiModelProperty(required = true, value = "Can be any value")
  @NotNull


  public Object getParameterValue() {
    return parameterValue;
  }

  public void setParameterValue(Object parameterValue) {
    this.parameterValue = parameterValue;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NamedParameterValue namedParameterValue = (NamedParameterValue) o;
    return Objects.equals(this.name, namedParameterValue.name) &&
        Objects.equals(this.parameterType, namedParameterValue.parameterType) &&
        Objects.equals(this.arrayType, namedParameterValue.arrayType) &&
        Objects.equals(this.parameterValue, namedParameterValue.parameterValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, parameterType, arrayType, parameterValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NamedParameterValue {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    parameterType: ").append(toIndentedString(parameterType)).append("\n");
    sb.append("    arrayType: ").append(toIndentedString(arrayType)).append("\n");
    sb.append("    parameterValue: ").append(toIndentedString(parameterValue)).append("\n");
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

