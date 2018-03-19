package org.pmiops.workbench.exceptions;


import com.ecwid.maleorang.MailchimpException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;


@ControllerAdvice
public class ExceptionAdvice {

  @ExceptionHandler({Exception.class})
  public ResponseEntity<?> serverError(Exception e) {
    ErrorResponse errorResponse = new ErrorResponse();
    Integer statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

    // if this error was thrown by another error, get the info from that exception
    Throwable relevantError = e;
    if (e.getCause() != null) {
      relevantError = e.getCause();
    }

    errorResponse.setMessage(relevantError.getMessage());
    errorResponse.setErrorClassName(relevantError.getClass().getName());

    // if exception class has an HTTP status associated with it, grab it
    if (relevantError.getClass().getAnnotation(ResponseStatus.class) != null) {
      statusCode = relevantError.getClass().getAnnotation(ResponseStatus.class).value().value();
    }
    if (relevantError instanceof MailchimpException) {
      MailchimpException mailchimpException = (MailchimpException) relevantError;
      statusCode = mailchimpException.code;
      errorResponse.setMessage(mailchimpException.description);
    } else if (relevantError instanceof WorkbenchException) {
      WorkbenchException workbenchException = (WorkbenchException) relevantError;
      if (workbenchException.getErrorResponse() != null && workbenchException.getErrorResponse().getErrorCode() != null) {
        errorResponse.setErrorCode(workbenchException.getErrorResponse().getErrorCode());
      }
    }

    // only log error if it's a server error
    if (statusCode >= 500) {
      Logger log = Logger.getLogger(ExceptionAdvice.class.getName());
      log.log(Level.SEVERE, relevantError.getClass().getName(), e);
    }

    errorResponse.setStatusCode(statusCode);
    return ResponseEntity.status(statusCode).body(errorResponse);
  }
}
