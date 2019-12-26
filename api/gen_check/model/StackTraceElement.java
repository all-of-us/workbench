package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * 
 */
@ApiModel(description = "")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class StackTraceElement   {
  @JsonProperty("className")
  private String className = null;

  @JsonProperty("methodName")
  private String methodName = null;

  @JsonProperty("fileName")
  private String fileName = null;

  @JsonProperty("lineNumber")
  private Integer lineNumber = null;

  public StackTraceElement className(String className) {
    this.className = className;
    return this;
  }

   /**
   * class name
   * @return className
  **/
  @ApiModelProperty(required = true, value = "class name")
  @NotNull


  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public StackTraceElement methodName(String methodName) {
    this.methodName = methodName;
    return this;
  }

   /**
   * method name
   * @return methodName
  **/
  @ApiModelProperty(required = true, value = "method name")
  @NotNull


  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public StackTraceElement fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

   /**
   * source file name
   * @return fileName
  **/
  @ApiModelProperty(required = true, value = "source file name")
  @NotNull


  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public StackTraceElement lineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
    return this;
  }

   /**
   * line number
   * @return lineNumber
  **/
  @ApiModelProperty(required = true, value = "line number")
  @NotNull


  public Integer getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(Integer lineNumber) {
    this.lineNumber = lineNumber;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackTraceElement stackTraceElement = (StackTraceElement) o;
    return Objects.equals(this.className, stackTraceElement.className) &&
        Objects.equals(this.methodName, stackTraceElement.methodName) &&
        Objects.equals(this.fileName, stackTraceElement.fileName) &&
        Objects.equals(this.lineNumber, stackTraceElement.lineNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, methodName, fileName, lineNumber);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StackTraceElement {\n");
    
    sb.append("    className: ").append(toIndentedString(className)).append("\n");
    sb.append("    methodName: ").append(toIndentedString(methodName)).append("\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    lineNumber: ").append(toIndentedString(lineNumber)).append("\n");
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

