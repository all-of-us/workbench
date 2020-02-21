package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.BiMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.pmiops.workbench.model.Domain;

public class CommonStorageEnumsTest {
  private final Object INDICATES_STATIC_METHOD = null;

  final Set<Class> enumClasses = getEnumerationClasses();
  final Collection<Method> methods = Arrays.asList(CommonStorageEnums.class.getDeclaredMethods());

  // e.g. public static Short reviewStatusToStorage(ReviewStatus s)
  final Map<Class, Method> enumClassToStorageMethod =
      methods.stream()
          .filter(m -> enumClasses.contains(firstMethodParameterType(m)))
          // domainToDomainId is stringly typed - test with test_domainId
          .filter(m -> !m.getName().equals("domainToDomainId"))
          .collect(Collectors.toMap(this::firstMethodParameterType, m -> m));

  // e.g. public static ReviewStatus reviewStatusFromStorage(Short s)
  final Map<Type, Method> storageMethodToEnumClass =
      methods.stream()
          .filter(m -> enumClasses.contains(m.getReturnType()))
          // domainToDomainId is stringly typed - test with test_domainId
          .filter(m -> !m.getName().equals("domainIdToDomain"))
          .collect(Collectors.toMap(Method::getReturnType, m -> m));

  // domain ID is stringly-typed so special-case this

  @Test
  public void test_domainId() {
    for (Domain v : Domain.values()) {
      String storageValue = CommonStorageEnums.domainToDomainId(v);
      assertWithMessage("unmapped enum value: " + v).that(storageValue).isNotNull();
      assertThat(v).isEqualTo(CommonStorageEnums.domainIdToDomain(storageValue));
    }
  }

  // this is a copy of StorageEnumsTest.test_noMissingMapEntries()

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
    final Object returnValue =
        enumClassToStorageMethod.get(enumClass).invoke(INDICATES_STATIC_METHOD, enumValue);
    assertThat(returnValue instanceof Short).isTrue();
    return (Short) returnValue;
  }

  private Object storageToEnum(Short shortValue, Class enumClass)
      throws InvocationTargetException, IllegalAccessException {
    return storageMethodToEnumClass.get(enumClass).invoke(INDICATES_STATIC_METHOD, shortValue);
  }

  /**
   * Retrieve all Enum classes in CommonStorageEnums
   *
   * <p>our convention for BiMaps in CommonStorageEnums is <Enum, Short> so we retrieve the first
   * parameterized type
   *
   * @return
   */
  private Set<Class> getEnumerationClasses() {
    return Stream.of(CommonStorageEnums.class.getDeclaredFields())
        .filter(field -> field.getType().equals(BiMap.class))
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
}
