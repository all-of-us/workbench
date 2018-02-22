package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends WorkbenchException {
  public NotFoundException() {
    super();
  }

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public NotFoundException(Throwable t) {
    super(t);
  }

  public NotFoundException(String message, Throwable t) {
    super(message, t);
  }
}
