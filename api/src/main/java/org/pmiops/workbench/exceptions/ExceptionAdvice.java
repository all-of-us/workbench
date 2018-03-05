package org.pmiops.workbench.exceptions;


import com.ecwid.maleorang.MailchimpException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionAdvice {

  @ExceptionHandler({Exception.class})
  public ResponseEntity<?> serverError(Exception e) {
    ErrorResponse errorResponse = new ErrorResponse();
    Integer statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

    // if this error was thrown by another error, get the info from that exception
    Throwable cause = e;
    if (e.getCause() != null) {
      cause = e.getCause();
    }

    errorResponse.setMessage(cause.getMessage());
    errorResponse.setErrorClassName(cause.getClass().getSimpleName());

    // get properties based on class of error thrown
    if (cause instanceof DefinesHttpResponseCode) {
      DefinesHttpResponseCode exceptionWithHttpStatus = (DefinesHttpResponseCode) cause;
      statusCode = exceptionWithHttpStatus.statusCode().value();
    }
    if (cause instanceof MailchimpException) {
      MailchimpException mailchimpException = (MailchimpException) cause;
      statusCode = mailchimpException.code;
      errorResponse.setMessage(mailchimpException.description);
    } else if (cause instanceof WorkbenchException) {
      WorkbenchException workbenchException = (WorkbenchException) cause;
      if (workbenchException.getErrorResponse() != null && workbenchException.getErrorResponse().getErrorCode() != null) {
        errorResponse.setErrorCode(workbenchException.getErrorResponse().getErrorCode());
      }
    }

    // only log error if it's a server error
    if (statusCode >= 500) {
      Logger log = Logger.getLogger(ExceptionAdvice.class.getName());
      log.log(Level.SEVERE, cause.getClass().getName(), e);
    }

    errorResponse.setStatusCode(statusCode);
    return ResponseEntity.status(statusCode).body(errorResponse);
  }
}
