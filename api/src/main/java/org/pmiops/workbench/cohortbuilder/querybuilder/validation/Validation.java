package org.pmiops.workbench.cohortbuilder.querybuilder.validation;

import org.pmiops.workbench.exceptions.BadRequestException;

import java.util.List;
import java.util.function.Predicate;

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
    throwExceptionWithArgs(message, null);
  }

  public void throwExceptionWithArgs(String message, List<String> args) {
    if (throwException) {
      if (args == null) {
        throw new BadRequestException(message);
      } else {
        throw new BadRequestException(String.format(message, String.join(", ", args)));
      }
    }
  }

}


