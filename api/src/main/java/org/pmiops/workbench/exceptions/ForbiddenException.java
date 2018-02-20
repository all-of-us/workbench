package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

  private ErrorResponse errorResponse;

  public ForbiddenException() {
    super();
  }

  public ForbiddenException(String message) {
    this(ExceptionUtils.errorResponse(message));
  }

  public ForbiddenException(ErrorResponse errorResponse) {
    super(errorResponse.getMessage());
    this.errorResponse = errorResponse;
  }

  public ForbiddenException(Throwable t) {
    super(t);
  }

  public ErrorResponse getErrorResponse() {
    return errorResponse;
  }
}
