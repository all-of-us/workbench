package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.function.Function;

public class PropertyUtils {
  static <T> Function<T, String> stringOrNull(Function<T, Object> entityExtractor) {
    return entity -> {
      var value = entityExtractor.apply(entity);
      return value == null ? null : value.toString();
    };
  }
}
