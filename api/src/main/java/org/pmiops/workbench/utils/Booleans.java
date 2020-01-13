package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class Booleans {
  public static final Set<String> VALUE_STRINGS =
      ImmutableSet.of(true, false).stream()
          .map(Object::toString)
          .collect(ImmutableSet.toImmutableSet());
}
