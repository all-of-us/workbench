package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.function.Function;

public class PropertyUtils {

  /**
   * Returns a property extractor to transform a member field of an entity object to String or null.
   *
   * <p>For example: `stringOrNull(Profile::getAddress)` will return a function which is equivalent
   * to `profile -> profile.getAddress() == null ? null : profile.getAddress().toString()`.
   *
   * @param entityExtractor a function to extract a member field from an entity object
   * @return a function to transform the extracted field of the entity to String or null
   * @param <T> the type of the entity object
   */
  static <T> Function<T, String> stringOrNull(Function<T, Object> entityExtractor) {
    return entity -> {
      var value = entityExtractor.apply(entity);
      return value == null ? null : value.toString();
    };
  }
}
