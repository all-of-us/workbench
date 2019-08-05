package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.BiMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import org.junit.Test;

public class StorageEnumsTest {
  @Test
  public void noMissingMapEntries() throws Exception {
    for (Field f : StorageEnums.class.getDeclaredFields()) {
      if (f.getType() != BiMap.class) {
        continue;
      }

      Class enumClass =
          (Class) ((ParameterizedType) f.getAnnotatedType().getType()).getActualTypeArguments()[0];

      Method enumConvertMethod = null;
      for (Method m : StorageEnums.class.getDeclaredMethods()) {
        if (m.getParameterTypes()[0].equals(enumClass)) {
          enumConvertMethod = m;
        }
      }

      Method finalEnumConversionMethod = enumConvertMethod;
      for (Object e : enumClass.getEnumConstants()) {
        assertThat(finalEnumConversionMethod.invoke(null, e))
            .named(enumClass.getName() + ":" + e.toString())
            .isNotNull();
      }
    }
  }
}
