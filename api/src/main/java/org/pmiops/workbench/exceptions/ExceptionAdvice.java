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
    ErrorResponse errorResponse = WorkbenchException.errorResponse();
    // if this error was thrown by another error, get the info from that exception
    Throwable relevantError = e;
    if (e.getCause() != null) {
      relevantError = e.getCause();
    }

    final int statusCode;
    // if exception class has an HTTP status associated with it, grab it
    if (relevantError.getClass().getAnnotation(ResponseStatus.class) != null) {
      statusCode = relevantError.getClass().getAnnotation(ResponseStatus.class).value().value();
    } else {
      statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    if (relevantError instanceof WorkbenchException) {
      // Only include Exception details on Workbench errors.
      errorResponse.setMessage(relevantError.getMessage());
      errorResponse.setErrorClassName(relevantError.getClass().getName());
      WorkbenchException workbenchException = (WorkbenchException) relevantError;
      if (workbenchException.getErrorResponse() != null
          && workbenchException.getErrorResponse().getErrorCode() != null) {
        errorResponse.setErrorCode(workbenchException.getErrorResponse().getErrorCode());
      }
    }

    // only log error if it's a server error
    if (statusCode >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
      final String logMessage = String.format("ErrorId %s: %s",
          errorResponse.getErrorUniqueId(),
          relevantError.getClass().getName());
      log.log(Level.SEVERE, logMessage, e);
    }

    errorResponse.setStatusCode(statusCode);
    return ResponseEntity.status(statusCode).body(errorResponse);
  }
}
