package org.pmiops.workbench.exceptions;


import com.ecwid.maleorang.MailchimpException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.model.ErrorCode;
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
    ErrorResponse errorResponse = new ErrorResponse();
    Integer statusCode = 500;

    // capture application-specific error code
    ResponseStatus responseStatus = null;
    responseStatus = e.getClass().getAnnotation(ResponseStatus.class);
    if (responseStatus != null) {
      errorResponse.setErrorCode(ErrorCode.fromValue(responseStatus.code().toString()));
    }

    // handle the way MailChimp (via maleorang) formats its errors
    if (e.getCause() instanceof MailchimpException) {
      statusCode = ((MailchimpException) e.getCause()).code;
      errorResponse.setMessage(((MailchimpException) e.getCause()).description);
    }
    else {
      errorResponse.setMessage(e.getMessage());
    }

    errorResponse.setStatusCode(statusCode);
    errorResponse.setErrorClassName(e.getCause().getClass().getSimpleName());

    // different logging levels for different error codes
    if (statusCode >= 500) {
      log.log(Level.SEVERE, e.getCause().getClass().getName(), e);
    }
    else if (statusCode >= 400) {
      log.log(Level.WARNING, e.getCause().getClass().getName(), e);
    }
    else {
      log.log(Level.INFO, e.getCause().getClass().getName(), e);
    }
    return ResponseEntity.status(statusCode).body(errorResponse);
  }
}
