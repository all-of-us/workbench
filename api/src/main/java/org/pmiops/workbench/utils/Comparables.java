package org.pmiops.workbench.utils;

/** A few generic utilities to hopefully make Comparators less ugly */
public class Comparables {

  public static <T extends Comparable<T>> Boolean isLessThan(T one, T other) {
    return one.compareTo(other) < 0;
  }

  public static <T extends Comparable<T>> Boolean isLessThanOrEqualTo(T one, T other) {
    return one.compareTo(other) <= 0;
  }

  public static <T extends Comparable<T>> Boolean isEqualValueTo(T one, T other) {
    return 0 == one.compareTo(other);
  }

  public static <T extends Comparable<T>> Boolean isGreaterThanOrEqualTo(T one, T other) {
    return 0 <= one.compareTo(other);
  }

  public static <T extends Comparable<T>> Boolean isGreaterThan(T one, T other) {
    return 0 < one.compareTo(other);
  }
}
