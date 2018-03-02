package org.pmiops.workbench.exceptions;


import com.ecwid.maleorang.MailchimpException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.weaver.ast.Not;
import org.pmiops.workbench.mailchimp.MailChimpService;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.model.ErrorResponse;
import org.springframework.http.HttpStatus;
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
    Integer statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    Throwable cause = e;

    if (e.getCause() != null) {
      cause = e.getCause();
    }

    // TODO: figure out how to get ErrorCode content from internal errors
//    ResponseStatus responseStatus = null;
//    responseStatus = e.getClass().getAnnotation(ResponseStatus.class);
//    if (responseStatus != null) {
//      errorResponse.setErrorCode(ErrorCode.fromValue(responseStatus.code().toString()));
//    }

    errorResponse.setMessage(cause.getMessage());

    if (cause instanceof MailchimpException) {
      MailchimpException me = (MailchimpException) cause;
      statusCode = me.code;
      errorResponse.setMessage(me.description);
    } else if (cause instanceof BadRequestException) {
      statusCode = HttpStatus.BAD_REQUEST.value();
    } else if (cause instanceof ConflictException) {
      statusCode = HttpStatus.CONFLICT.value();
    } else if (cause instanceof FailedPreconditionException) {
      statusCode = HttpStatus.PRECONDITION_FAILED.value();
    } else if (cause instanceof ForbiddenException) {
      statusCode = HttpStatus.FORBIDDEN.value();
    } else if (cause instanceof NotFoundException) {
      statusCode = HttpStatus.NOT_FOUND.value();
    } else if (cause instanceof ServerUnavailableException) {
      statusCode = HttpStatus.SERVICE_UNAVAILABLE.value();
    }

    errorResponse.setStatusCode(statusCode);
    errorResponse.setErrorClassName(cause.getClass().getSimpleName());

    // different logging levels for different error codes
    Level level = Level.INFO;
    if (statusCode >= 500) {
      level = Level.SEVERE;
    } else if (statusCode >= 400) {
      level = Level.WARNING;
    }
    log.log(level, cause.getClass().getName(), e);
    return ResponseEntity.status(statusCode).body(errorResponse);
  }
}
