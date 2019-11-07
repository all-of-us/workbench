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
    for (Field f : DbStorageEnums.class.getDeclaredFields()) {
      if (f.getType() != BiMap.class) {
        continue;
      }

      Class enumClass =
          (Class) ((ParameterizedType) f.getAnnotatedType().getType()).getActualTypeArguments()[0];

      Method enumToShort = null;
      Method shortToEnum = null;
      for (Method m : DbStorageEnums.class.getDeclaredMethods()) {
        if (m.getParameterTypes()[0].equals(enumClass)) {
          enumToShort = m;
        }

        if (m.getReturnType().equals(enumClass)) {
          shortToEnum = m;
        }
      }

      for (Object e : enumClass.getEnumConstants()) {
        Short shortVal = (Short) enumToShort.invoke(null, e);

        assertThat(shortVal).named(enumClass.getName() + ":" + e.toString()).isNotNull();
        assertThat(shortToEnum.invoke(null, shortVal)).isEqualTo(e);
      }
    }
  }
}
