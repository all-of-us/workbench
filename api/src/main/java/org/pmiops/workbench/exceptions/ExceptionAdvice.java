package org.pmiops.workbench.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ExceptionAdvice {

  private static final Logger log = Logger.getLogger(ExceptionAdvice.class.getName());

  private static final String DEFAULT_ERROR_VIEW = "error";

  @ExceptionHandler({Exception.class})
  public String serverError(Exception e) {
    if (e.getClass().getPackage().getName().equals(
        ExceptionAdvice.class.getPackage().getName())) {
      ResponseStatus responseStatus = e.getClass().getAnnotation(ResponseStatus.class);
      if (responseStatus != null) {
        log.log(Level.WARNING, "[{0}] {1}: {2}",
            new Object[]{responseStatus.code().value(), e.getClass().getSimpleName(),
                e.getMessage()});
        if (responseStatus.code().value() < 500) {
          return DEFAULT_ERROR_VIEW;
        }
      }
    }
    log.log(Level.SEVERE, "Server error", e);
    return DEFAULT_ERROR_VIEW;
  }
}
