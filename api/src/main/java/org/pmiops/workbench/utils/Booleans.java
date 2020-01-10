package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class Booleans {

  /**
   * Get a set of strings for all (both) of the values Boolean can have.
   *
   * @return
   */
  public static Set<String> getValueStrings() {
    return ImmutableSet.of(true, false).stream()
        .map(Object::toString)
        .collect(ImmutableSet.toImmutableSet());
  }
}
