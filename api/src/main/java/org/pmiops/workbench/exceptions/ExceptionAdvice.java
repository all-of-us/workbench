package org.pmiops.workbench.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionAdvice {
  private static final Logger log = Logger.getLogger(ExceptionAdvice.class.getName());

  @ExceptionHandler({HttpMessageNotReadableException.class})
  public ResponseEntity<?> messageNotReadableError(Exception e) {
    log.log(Level.INFO, "failed to parse HTTP request message, returning 400", e);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            WorkbenchException.errorResponse("failed to parse valid JSON request message")
                .statusCode(HttpStatus.BAD_REQUEST.value()));
  }

  @ExceptionHandler({Exception.class})
  public ResponseEntity<?> serverError(Exception e) {
    final int statusCode;
    // if exception class has an HTTP status associated with it, grab it
    if (e.getClass().getAnnotation(ResponseStatus.class) != null) {
      statusCode = e.getClass().getAnnotation(ResponseStatus.class).value().value();
    } else {
      statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    ErrorResponse errorResponse = WorkbenchException.errorResponse().statusCode(statusCode);

    // Only include Exception details on Workbench errors.
    if (e instanceof WorkbenchException) {
      errorResponse.setErrorClassName(e.getClass().getName());
      ErrorResponse thrownErrorResponse = ((WorkbenchException) e).getErrorResponse();
      if (thrownErrorResponse != null) {
        errorResponse
            .message(thrownErrorResponse.getMessage())
            .errorCode(thrownErrorResponse.getErrorCode())
            .parameters(thrownErrorResponse.getParameters());
      }
    }

    // only log error if it's a server error
    if (statusCode >= HttpStatus.INTERNAL_SERVER_ERROR.value()
        || statusCode == HttpStatus.BAD_REQUEST.value()) {
      final String logMessage =
          String.format("ErrorId %s: %s", errorResponse.getErrorUniqueId(), e.getClass().getName());
      log.log(Level.SEVERE, logMessage, e);
    }

    return ResponseEntity.status(statusCode).body(errorResponse);
  }
}
