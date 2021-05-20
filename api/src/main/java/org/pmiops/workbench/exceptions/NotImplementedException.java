package org.pmiops.workbench.exceptions;

import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
public class NotImplementedException extends WorkbenchException {
  public NotImplementedException() {
    super();
  }

  public NotImplementedException(String message) {
    super(message);
  }

  public NotImplementedException(ErrorResponse errorResponse) {
    super(errorResponse);
  }

  public NotImplementedException(Throwable t) {
    super(t);
  }

  public NotImplementedException(String message, Throwable t) {
    super(message, t);
  }
}
