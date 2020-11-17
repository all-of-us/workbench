package org.pmiops.workbench.utils;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class
FunctionUtils {

  /**
   * Given a property mutator that returns void, such as DbUser.setContactEmail(), we need a method
   * that returns the updated object. This helper simply wraps such a mutator.
   * @param mutator - function that sets a value on the target object and returns void
   * @param <TARGET_T> - type parameter for target object
   * @param <PROPERTY_T> - type parameter for value being set
   * @return
   */
  public static <TARGET_T, PROPERTY_T> BiFunction<TARGET_T, PROPERTY_T, TARGET_T>
  makeMutatorComposable(BiConsumer<TARGET_T, PROPERTY_T> mutator) {
    return (TARGET_T t, PROPERTY_T u) -> {
      mutator.accept(t, u);
      return t; };
  }
}
