package org.pmiops.workbench.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EmailException extends RuntimeException {

  public EmailException() {
    super();
  }

  public EmailException(String message) {
    super(message);
  }

  public EmailException(Throwable t) {
    super(t);
  }

  public EmailException(String message, Throwable t) {
    super(message, t);
  }
}
