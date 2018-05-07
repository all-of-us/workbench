package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends WorkbenchException {
  public UnauthorizedException() {
    super();
  }

  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public UnauthorizedException(Throwable t) {
    super(t);
  }

  public UnauthorizedException(String message, Throwable t) {
    super(message, t);
  }
}