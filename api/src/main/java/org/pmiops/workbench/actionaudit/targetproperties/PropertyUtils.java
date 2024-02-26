package org.pmiops.workbench.actionaudit.targetproperties;

public class PropertyUtils {
  static <T> String toStringOrNull(T object) {
    return object == null ? null : object.toString();
  }
}
