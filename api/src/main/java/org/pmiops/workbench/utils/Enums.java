package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Set;

public class Enums {
  public static <T extends Enum<T>> Set<String> getValueStrings(Class<T> enumType) {
    return Arrays.stream(enumType.getEnumConstants())
        .map(T::toString)
        .collect(ImmutableSet.toImmutableSet());
  }
}
