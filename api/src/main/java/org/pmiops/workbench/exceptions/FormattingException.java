package org.pmiops.workbench.exceptions;

import java.util.Formatter;

/**
 * Base class for exceptions with printf-style formatting in the message. Example:
 *   throw new FormattingException("Expected %d but got %s!", 2, "bad value");
 */
class FormattingException extends RuntimeException {
  public FormattingException() {
    super();
  }

  public FormattingException(String message) {
    super(message);
  }

  public FormattingException(String message, Object... formatArgs) {
    super(new Formatter().format(message, formatArgs).toString());
  }
}
