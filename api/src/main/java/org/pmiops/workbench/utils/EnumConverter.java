package org.pmiops.workbench.utils;

import org.mapstruct.TargetType;

public class EnumConverter {

  public static String convert(Enum<?> source) {
    return source == null ? null : source.toString();
  }

  public static <E extends Enum<E>> E convert(String source, @TargetType Class<E> enumType) {
    // You probably need something else here as the methods are not symmetrical
    return source == null ? null : Enum.valueOf( enumType, source );
  }
}