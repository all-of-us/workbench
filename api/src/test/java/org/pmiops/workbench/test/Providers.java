package org.pmiops.workbench.test;

import javax.inject.Provider;

public class Providers {

  public static <T> Provider<T> of(final T t) {
    return new Provider<T>() {
      @Override
      public T get() {
        return t;
      }
    };
  }
}
