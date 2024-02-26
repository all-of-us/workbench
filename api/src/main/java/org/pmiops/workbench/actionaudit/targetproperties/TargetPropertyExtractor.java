package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.HashMap;
import java.util.Map;

public class TargetPropertyExtractor {
  private TargetPropertyExtractor() {}

  public static <T, E extends ModelBackedTargetProperty<T>>
      Map<String, String> getPropertyValuesByName(E[] enumValues, T target) {
    Map<String, String> result = new HashMap<>();
    for (E e : enumValues) {
      if (e.getExtractor().apply(target) != null) {
        result.put(e.getPropertyName(), e.getExtractor().apply(target));
      }
    }
    return result;
  }

  public static <T, E extends ModelBackedTargetProperty<T>>
      Map<String, PreviousNewValuePair> getChangedValuesByName(
          E[] enumValues, T previousTarget, T newTarget) {
    Map<String, PreviousNewValuePair> result = new HashMap<>();
    for (E e : enumValues) {
      PreviousNewValuePair pair =
          new PreviousNewValuePair(
              e.getExtractor().apply(previousTarget), e.getExtractor().apply(newTarget));
      if (pair.hasValueChanged()) {
        result.put(e.getPropertyName(), pair);
      }
    }
    return result;
  }
}
