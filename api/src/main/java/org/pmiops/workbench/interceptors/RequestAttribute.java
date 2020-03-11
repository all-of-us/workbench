package org.pmiops.workbench.interceptors;

/**
 * Since different interceptors may be adjusting the same request attribute map, it's helpful to
 * have a central place to declare keys.
 */
public enum RequestAttribute {
  TRACE("Tracing Span"),
  START_INSTANT("Start Instant");

  private final String keyName;

  RequestAttribute(String keyName) {
    this.keyName = keyName;
  }

  public String getKeyName() {
    return keyName;
  }

  @Override
  public String toString() {
    return keyName;
  }
}
