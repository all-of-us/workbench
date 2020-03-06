package org.pmiops.workbench.exceptions;

import java.util.UUID;
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
    this.errorResponse = errorResponse();
  }

  public WorkbenchException(String message, Throwable t) {
    super(message, t);
    this.errorResponse = errorResponse(message);
  }

  public ErrorResponse getErrorResponse() {
    return errorResponse;
  }

  // For security reasons, we want a response stripped of everything
  // but this identifying UUID.
  public static ErrorResponse errorResponse() {
    return new ErrorResponse().errorUniqueId(UUID.randomUUID().toString());
  }

  public static ErrorResponse errorResponse(String message) {
    return errorResponse().message(message);
  }

  public static ErrorResponse errorResponse(String message, ErrorCode code) {
    return errorResponse(message).errorCode(code);
  }
}
