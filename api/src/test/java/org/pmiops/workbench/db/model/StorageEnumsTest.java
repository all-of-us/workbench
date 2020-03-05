package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.pmiops.workbench.model.Domain;

public class StorageEnumsTest {
  private final Object INDICATES_STATIC_METHOD = null;

  final Set<Class> enumClasses = getEnumerationClasses();
  final Set<Method> methods =
      arraysToSet(
          DbStorageEnums.class.getDeclaredMethods(), DbStorageEnums.class.getDeclaredMethods());

  // e.g. public static Short reviewStatusToStorage(ReviewStatus s)
  final Map<Class, Method> enumClassToStorageMethod =
      methods.stream()
          // domainToDomainId is stringly typed - test with test_domainId
          .filter(m -> !m.getName().equals("domainToDomainId"))
          .filter(m -> enumClasses.contains(firstMethodParameterType(m)))
          .collect(Collectors.toMap(this::firstMethodParameterType, m -> m));

  // e.g. public static ReviewStatus reviewStatusFromStorage(Short s)
  final Map<Type, Method> storageMethodToEnumClass =
      methods.stream()
          // domainIdToDomain is stringly typed - test with test_domainId
          .filter(m -> !m.getName().equals("domainIdToDomain"))
          .filter(m -> enumClasses.contains(m.getReturnType()))
          .collect(Collectors.toMap(Method::getReturnType, m -> m));

  // special-case test for domain ID round trip

  @Test
  public void test_domainId() {
    for (Domain domainValue : Domain.values()) {
      String storageValue = DbStorageEnums.domainToDomainId(domainValue);
      assertWithMessage("unmapped enum value: " + domainValue).that(storageValue).isNotNull();
      assertThat(DbStorageEnums.domainIdToDomain(storageValue)).isEqualTo(domainValue);
    }
  }

  @Test
  public void test_noMissingMapEntries() throws InvocationTargetException, IllegalAccessException {
    for (final Class enumClass : enumClasses) {
      for (final Object enumValue : enumClass.getEnumConstants()) {
        Short shortValue = enumToStorage(enumValue, enumClass);
        assertThat(shortValue).named(enumClass.getName() + ":" + enumValue.toString()).isNotNull();
        assertThat(storageToEnum(shortValue, enumClass)).isEqualTo(enumValue);
      }
    }
  }

  private Short enumToStorage(Object enumValue, Class enumClass)
      throws InvocationTargetException, IllegalAccessException {
    final Method enumToStorageMethod = enumClassToStorageMethod.get(enumClass);
    final Object returnValue = enumToStorageMethod.invoke(INDICATES_STATIC_METHOD, enumValue);
    assertThat(returnValue instanceof Short).isTrue();
    return (Short) returnValue;
  }

  private Object storageToEnum(Short shortValue, Class enumClass)
      throws InvocationTargetException, IllegalAccessException {
    final Method storageToEnumMethod = storageMethodToEnumClass.get(enumClass);
    return storageToEnumMethod.invoke(INDICATES_STATIC_METHOD, shortValue);
  }

  /**
   * Retrieve all Enum classes in DbStorageEnums and DbStorageEnums
   *
   * <p>our convention for BiMaps in DbStorageEnums/DbStorageEnums is <Enum, Short> so we retrieve
   * the first parameterized type
   *
   * @return
   */
  private Set<Class> getEnumerationClasses() {
    return arraysToSet(
            DbStorageEnums.class.getDeclaredFields(), DbStorageEnums.class.getDeclaredFields())
        .stream()
        .map(
            field -> {
              // gets the specific BiMap type including annotations
              final Type biMapType = field.getAnnotatedType().getType();

              // need a ParameterizedType to access the type arguments
              assertThat(biMapType instanceof ParameterizedType).isTrue();
              final ParameterizedType pType = (ParameterizedType) biMapType;

              // our convention is for the enum type to come first
              final Type enumType = pType.getActualTypeArguments()[0];

              // needed for the isEnum() check
              final Class enumClass = (Class) enumType;
              assertThat(enumClass.isEnum()).isTrue();
              return enumClass;
            })
        .collect(Collectors.toSet());
  }

  // convenience method describing what is retrieved here
  private Class firstMethodParameterType(Method method) {
    return method.getParameterTypes()[0];
  }

  private <T> Set<T> arraysToSet(T[]... arrays) {
    return Stream.of(arrays).flatMap(Stream::of).collect(Collectors.toSet());
  }
}
