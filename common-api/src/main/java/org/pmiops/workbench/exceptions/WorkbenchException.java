package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.model.ErrorResponse;

public class WorkbenchException extends RuntimeException {

  private ErrorResponse errorResponse;

  public WorkbenchException() {
    super();
  }

  public WorkbenchException(String message) {
    this(errorResponse(message));
  }

  public WorkbenchException(ErrorResponse errorResponse) {
    super(errorResponse.getMessage());
    this.errorResponse = errorResponse;
  }

  public WorkbenchException(Throwable t) {
    super(t);
  }

  public WorkbenchException(String message, Throwable t) {
    super(message, t);
    this.errorResponse = errorResponse(message);
  }

  public ErrorResponse getErrorResponse() {
    return errorResponse;
  }

  public static ErrorResponse errorResponse(String message) {
    return errorResponse(null, message);
  }

  public static ErrorResponse errorResponse(ErrorCode code, String message) {
    ErrorResponse response = new ErrorResponse();
    response.setMessage(message);
    response.setErrorCode(code);
    return response;
  }
}
