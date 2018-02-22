package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends WorkbenchException {
  public ForbiddenException() {
    super();
  }

  public ForbiddenException(String message) {
    super(message);
  }

  public ForbiddenException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public ForbiddenException(Throwable t) {
    super(t);
  }

  public ForbiddenException(String message, Throwable t) {
    super(message, t);
  }
}
