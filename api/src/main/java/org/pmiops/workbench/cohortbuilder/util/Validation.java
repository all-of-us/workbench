package org.pmiops.workbench.cohortbuilder.util;

import java.text.MessageFormat;
import java.util.function.Predicate;
import org.pmiops.workbench.exceptions.BadRequestException;

public class Validation<K> {

  private Predicate<K> predicate;
  private Boolean throwException;

  public static <K> Validation<K> from(Predicate<K> predicate) {
    return new Validation<K>(predicate);
  }

  private Validation(Predicate<K> predicate) {
    this.predicate = predicate;
  }

  public Validation test(K param) {
    return predicate.test(param) ? throwException() : ok();
  }

  public Validation throwException() {
    this.throwException = true;
    return this;
  }

  public Validation ok() {
    this.throwException = false;
    return this;
  }

  public void throwException(String message) {
    if (throwException) {
      throw new BadRequestException(message);
    }
  }

  public void throwException(String message, Object... args) {
    if (throwException) {
      throw new BadRequestException(new MessageFormat(message).format(args));
    }
  }
}
