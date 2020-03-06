package org.pmiops.workbench.interceptors;

import io.opencensus.common.Scope;
import java.time.Instant;

/**
 * Since different interceptors may be adjusting request attributes, it's helpful to have a central
 * place to declare keys and value types.
 */
public enum RequestAttribute {
  TRACE("Tracing Span", Scope.class),
  START_INSTANT("Start Instant", Instant.class);

  private final String keyName;
  private final Class valueClass;

  RequestAttribute(String keyName, Class valueClass) {
    this.keyName = keyName;
    this.valueClass = valueClass;
  }

  public String getKeyName() {
    return keyName;
  }

  public Class getValueClass() {
    return valueClass;
  }

  @Override
  public String toString() {
    return keyName;
  }
}
