package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.BiMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;

@RunWith(Parameterized.class)
public class CommonStorageEnumsTest {
  @Parameters(name = "{0}")
  public static Object[][] data() {
    return new Object[][] {
      {
        DataAccessLevel.class.getSimpleName(),
        DataAccessLevel.values(),
        (Function<Short, DataAccessLevel>) CommonStorageEnums::dataAccessLevelFromStorage,
        (Function<DataAccessLevel, Short>) CommonStorageEnums::dataAccessLevelToStorage
      },
      {
        Domain.class.getSimpleName(),
        Domain.values(),
        (Function<Short, Domain>) CommonStorageEnums::domainFromStorage,
        (Function<Domain, Short>) CommonStorageEnums::domainToStorage
      }
    };
  }

  @Parameter() public String description;

  @Parameter(1)
  public Enum<?>[] enumValues;

  @Parameter(2)
  public Function<Short, Enum<?>> fromStorage;

  @Parameter(3)
  public Function<Enum<?>, Short> toStorage;

  @Test
  public void testBijectiveStorageMapping() {
    for (Enum<?> v : enumValues) {
      Short storageValue = toStorage.apply(v);
      assertWithMessage("unmapped enum value: " + v).that(storageValue).isNotNull();
      assertThat(v).isEqualTo(fromStorage.apply(storageValue));
    }
  }

  // domain ID is stringly-typed so special-case this

  @Test
  public void testDomainIdBijectiveStorageMapping() {
    for (Domain v : Domain.values()) {
      String storageValue = CommonStorageEnums.domainToDomainId(v);
      assertWithMessage("unmapped enum value: " + v).that(storageValue).isNotNull();
      assertThat(v).isEqualTo(CommonStorageEnums.domainIdToDomain(storageValue));
    }
  }

  // copied from api/StorageEnumsTest because the above tests are not comprehensive
  @Test
  public void noMissingMapEntries() throws Exception {
    for (Field f : CommonStorageEnums.class.getDeclaredFields()) {
      if (f.getType() != BiMap.class) {
        continue;
      }

      Class enumClass =
          (Class) ((ParameterizedType) f.getAnnotatedType().getType()).getActualTypeArguments()[0];

      Method enumToShort = null;
      Method shortToEnum = null;
      for (Method m : CommonStorageEnums.class.getDeclaredMethods()) {
        if (m.getParameterTypes()[0].equals(enumClass)) {
          enumToShort = m;
        }

        if (m.getReturnType().equals(enumClass)) {
          shortToEnum = m;
        }
      }

      // stringly typed map - test with testDomainIdBijectiveStorageMapping instead
      if (enumToShort.getName().equals("domainIdToDomain")) {
        continue;
      }

      for (Object e : enumClass.getEnumConstants()) {
        Short shortVal = (Short) enumToShort.invoke(null, e);

        assertThat(shortVal).named(enumClass.getName() + ":" + e.toString()).isNotNull();
        assertThat(shortToEnum.invoke(null, shortVal)).isEqualTo(e);
      }
    }
  }
}
