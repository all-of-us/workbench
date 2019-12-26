package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.ErrorCode;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ErrorResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class ErrorResponse   {
  @JsonProperty("message")
  private String message = null;

  @JsonProperty("statusCode")
  private Integer statusCode = null;

  @JsonProperty("errorClassName")
  private String errorClassName = null;

  @JsonProperty("errorCode")
  private ErrorCode errorCode = null;

  @JsonProperty("errorUniqueId")
  private String errorUniqueId = null;

  public ErrorResponse message(String message) {
    this.message = message;
    return this;
  }

   /**
   * General error message for the response.
   * @return message
  **/
  @ApiModelProperty(value = "General error message for the response.")


  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ErrorResponse statusCode(Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

   /**
   * HTTP status code
   * @return statusCode
  **/
  @ApiModelProperty(value = "HTTP status code")


  public Integer getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  public ErrorResponse errorClassName(String errorClassName) {
    this.errorClassName = errorClassName;
    return this;
  }

   /**
   * Get errorClassName
   * @return errorClassName
  **/
  @ApiModelProperty(value = "")


  public String getErrorClassName() {
    return errorClassName;
  }

  public void setErrorClassName(String errorClassName) {
    this.errorClassName = errorClassName;
  }

  public ErrorResponse errorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
    return this;
  }

   /**
   * Short description of the type of error
   * @return errorCode
  **/
  @ApiModelProperty(value = "Short description of the type of error")

  @Valid

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public ErrorResponse errorUniqueId(String errorUniqueId) {
    this.errorUniqueId = errorUniqueId;
    return this;
  }

   /**
   * Unique ID for this error response, for correlation with backend logs
   * @return errorUniqueId
  **/
  @ApiModelProperty(value = "Unique ID for this error response, for correlation with backend logs")


  public String getErrorUniqueId() {
    return errorUniqueId;
  }

  public void setErrorUniqueId(String errorUniqueId) {
    this.errorUniqueId = errorUniqueId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorResponse errorResponse = (ErrorResponse) o;
    return Objects.equals(this.message, errorResponse.message) &&
        Objects.equals(this.statusCode, errorResponse.statusCode) &&
        Objects.equals(this.errorClassName, errorResponse.errorClassName) &&
        Objects.equals(this.errorCode, errorResponse.errorCode) &&
        Objects.equals(this.errorUniqueId, errorResponse.errorUniqueId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, statusCode, errorClassName, errorCode, errorUniqueId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorResponse {\n");
    
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
    sb.append("    errorClassName: ").append(toIndentedString(errorClassName)).append("\n");
    sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
    sb.append("    errorUniqueId: ").append(toIndentedString(errorUniqueId)).append("\n");
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

