package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Returns a 500 to the user. Indicates there's a bug somewhere in our code / some condition
 * that we haven't figured out how to handle properly.
 *
 * Use {@link ServerUnavailableException} instead for expected conditions where our server in
 * unable to handle a request temporarily.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ServerErrorException extends WorkbenchException {
  public ServerErrorException() {
    super();
  }

  public ServerErrorException(String message) {
    super(message);
  }

  public ServerErrorException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public ServerErrorException(Throwable t) {
    super(t);
  }

  public ServerErrorException(String message, Throwable t) {
    super(message, t);
  }
}
