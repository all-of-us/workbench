package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EmailException extends WorkbenchException {
  public EmailException() {
    super();
  }

  public EmailException(String message) {
    super(message);
  }

  public EmailException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public EmailException(Throwable t) {
    super(t);
  }

  public EmailException(String message, Throwable t) {
    super(message, t);
  }
}
