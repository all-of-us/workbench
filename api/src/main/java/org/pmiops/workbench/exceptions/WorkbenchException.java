package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;

public class WorkbenchException extends RuntimeException {

  private ErrorResponse errorResponse;

  public WorkbenchException() {
    super();
  }

  public WorkbenchException(String message) {
    this(ExceptionUtils.errorResponse(message));
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
    this.errorResponse = ExceptionUtils.errorResponse(message);
  }

  public ErrorResponse getErrorResponse() {
    return errorResponse;
  }
}
