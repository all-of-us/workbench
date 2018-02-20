package org.pmiops.workbench.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionAdvice {

  private static final Logger log = Logger.getLogger(ExceptionAdvice.class.getName());

  @ExceptionHandler({Exception.class})
  public ResponseEntity<?> serverError(Exception e) {
    int statusCode = 500;
    if (e.getClass().getPackage().getName().equals(
        ExceptionAdvice.class.getPackage().getName())) {
      ResponseStatus responseStatus = e.getClass().getAnnotation(ResponseStatus.class);
      if (responseStatus != null) {
        statusCode = responseStatus.value().value();
        log.log(Level.WARNING, "[{0}] {1}: {2}",
            new Object[]{statusCode, e.getClass().getSimpleName(), e.getMessage()});
        if (statusCode < 500) {
          if (e instanceof WorkbenchException) {
            ErrorResponse errorResponse = ((WorkbenchException) e).getErrorResponse();
            return ResponseEntity.status(statusCode).body(
                ((WorkbenchException) e).getErrorResponse());
          }
          return ResponseEntity.status(statusCode).body(ExceptionUtils.errorResponse(
              e.getMessage()));
        }
      }
    }
    log.log(Level.SEVERE, "Server error", e);
    String message = statusCode == 503 ? "The server is unavailable."
        : "An unexpected error occurred.";
    return ResponseEntity.status(statusCode).body(ExceptionUtils.errorResponse(message));
  }
}
