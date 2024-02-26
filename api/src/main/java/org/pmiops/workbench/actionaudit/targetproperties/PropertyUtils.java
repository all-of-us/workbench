package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.function.Function;

public class PropertyUtils {
  static <T> String toStringOrNull(T object) {
    return object == null ? null : object.toString();
  }

  static <T> Function<T, String> stringOrNullExtractor(Function<T, Object> entityExtractor) {
    return entity -> {
      var value = entityExtractor.apply(entity);
      return toStringOrNull(value);
    };
  }
}
