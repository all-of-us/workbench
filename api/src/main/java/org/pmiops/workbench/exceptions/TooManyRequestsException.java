package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TooManyRequestsException extends WorkbenchException {
  public TooManyRequestsException() {
    super();
  }

  public TooManyRequestsException(String message) {
    super(message);
  }

  public TooManyRequestsException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public TooManyRequestsException(Throwable t) {
    super(t);
  }

  public TooManyRequestsException(String message, Throwable t) {
    super(message, t);
  }
}
