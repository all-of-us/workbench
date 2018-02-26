package org.pmiops.workbench.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ecwid.maleorang.MailchimpException;
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

    ResponseStatus responseStatus = null;
    responseStatus = e.getClass().getAnnotation(ResponseStatus.class);
    if (responseStatus != null) {
      statusCode = responseStatus.code().value();
    }

    // because MailChimp is special
    if (e.getCause() instanceof MailchimpException) {
      statusCode = ((MailchimpException) e.getCause()).code;
      errorResponse.setMessage(((MailchimpException) e.getCause()).description);
    }
    else {
      errorResponse.setMessage(e.getMessage());
    }

    errorResponse.setStatusCode(statusCode);
    errorResponse.setErrorClassName(e.getCause().getClass().getSimpleName());

    log.log(Level.SEVERE, e.getCause().getClass().getName(), e);
    return ResponseEntity.status(statusCode).body(errorResponse);
  }
}
