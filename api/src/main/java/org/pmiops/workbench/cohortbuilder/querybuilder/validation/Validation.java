package org.pmiops.workbench.cohortbuilder.querybuilder.validation;

import org.pmiops.workbench.exceptions.BadRequestException;

import java.util.function.Predicate;

public class Validation<K> {

  private Predicate<K> predicate;
  private Boolean throwException;

  public static <K> Validation<K> check(Predicate<K> predicate) {
    return new Validation<K>(predicate);
  }

  private Validation(Predicate<K> predicate) {
    this.predicate = predicate;
  }

  public Validation validate(K param) {
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
    throwException(message, null);
  }

  public void throwException(String message, String arg) {
    if (throwException) {
      if (arg == null) {
        throw new BadRequestException(message);
      } else {
        throw new BadRequestException(String.format(message, arg));
      }
    }
  }

}


