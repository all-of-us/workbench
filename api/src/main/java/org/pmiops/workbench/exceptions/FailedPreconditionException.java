package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
public class FailedPreconditionException extends WorkbenchException {
  public FailedPreconditionException() {
    super();
  }

  public FailedPreconditionException(String message) {
    super(message);
  }

  public FailedPreconditionException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public FailedPreconditionException(Throwable t) {
    super(t);
  }

  public FailedPreconditionException(String message, Throwable t) {
    super(message, t);
  }
}
