package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends WorkbenchException {
  public BadRequestException() {
    super();
  }

  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public BadRequestException(Throwable t) {
    super(t);
  }

  public BadRequestException(String message, Throwable t) {
    super(message, t);
  }
}
