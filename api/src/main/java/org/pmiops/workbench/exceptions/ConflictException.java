package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends WorkbenchException {
  public ConflictException() {
    super();
  }

  public ConflictException(String message) {
    super(message);
  }

  public ConflictException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public ConflictException(Throwable t) {
    super(t);
  }

  public ConflictException(String message, Throwable t) {
    super(message, t);
  }
}
