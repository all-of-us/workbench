package org.pmiops.workbench.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ServerErrorException extends RuntimeException {

  public ServerErrorException() {
    super();
  }

  public ServerErrorException(String message) {
    super(message);
  }

  public ServerErrorException(Throwable t) {
    super(t);
  }

  public ServerErrorException(String message, Throwable t) {
    super(message, t);
  }
}
