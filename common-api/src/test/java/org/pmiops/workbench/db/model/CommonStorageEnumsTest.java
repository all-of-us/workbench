package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

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

  @Parameter()
  public String description;

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
}
