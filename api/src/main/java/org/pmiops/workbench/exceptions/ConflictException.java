package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

  private ErrorResponse errorResponse;

  public ConflictException(String message) {
    this(errorResponse(message));
  }

  public ConflictException(ErrorResponse errorResponse) {
    super(errorResponse.getMessage());
    this.errorResponse = errorResponse;
  }

  public ConflictException(Throwable t) {
    super(t);
  }

  public ErrorResponse getErrorResponse() {
    return errorResponse;
  }

  private static ErrorResponse errorResponse(String message) {
    ErrorResponse response = new ErrorResponse();
    response.setMessage(message);
    return response;
  }
}
