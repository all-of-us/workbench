package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class GatewayTimeoutException extends WorkbenchException {
  public GatewayTimeoutException() {
    super();
  }

  public GatewayTimeoutException(String message) {
    super(message);
  }

  public GatewayTimeoutException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public GatewayTimeoutException(Throwable t) {
    super(t);
  }

  public GatewayTimeoutException(String message, Throwable t) {
    super(message, t);
  }
}
