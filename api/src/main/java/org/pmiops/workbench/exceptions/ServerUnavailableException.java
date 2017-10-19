package org.pmiops.workbench.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Returns a 503 to the client, indicating that the service temporarily can't handle the request
 * and the client should retry.
 *
 * Use {@link ServerErrorException} instead when there's a bug / condition we haven't figured out
 * how to handle yet.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServerUnavailableException extends RuntimeException {

  public ServerUnavailableException() {
    super();
  }

  public ServerUnavailableException(String message) {
    super(message);
  }

  public ServerUnavailableException(Throwable t) {
    super(t);
  }

  public ServerUnavailableException(String message, Throwable t) {
    super(message, t);
  }
}
