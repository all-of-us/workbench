package org.pmiops.workbench.exceptions;

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
}
