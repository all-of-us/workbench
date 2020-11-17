package org.pmiops.workbench.trackedproperties;

import java.rmi.AccessException;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiFunction;
import javax.inject.Provider;
import org.elasticsearch.common.Strings;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.stereotype.Service;

@Service
public class UserPropertyServiceImpl implements UserPropertyService {

  // TODO(jaycarlton):  I guess we can't rely on the template magic working on a static
  //   class inside a subclass specializing a generic interface. My kingdom for a typedef.
//  private static class StringPropSetter extends PropertySetter<DbUser, String> {};
//  private static class StringPropSetterBuilder extends PropertySetter.Builder<DbUser, String> {};

  private final Provider<DbUser> agentUserProvider;
  private final PropertyProcessorService<DbUser, String> stringProcessor;
  private final PropertyProcessorService<DbUser, DataAccessLevel> dataAccessLevelProcessor;
  private final PropertyProcessorService<DbUser, Boolean> booleanPropertyProcessor;

  private final UserDao userDao;
  private final UserGivenNamePropertyService userGivenNameProperty;
  private final TrackedProperty<DbUser, String> familyNameProperty;
  private final TrackedProperty<DbUser, Boolean> isDisabledProperty;
  private final TrackedProperty<DbUser, DataAccessLevel> dataAccessLevelProperty;

  public UserPropertyServiceImpl(
      Provider<DbUser> agentUserProvider,
      PropertyProcessorService<DbUser, String> stringProcessor,
      PropertyProcessorService<DbUser, DataAccessLevel> dataAccessLevelProcessor,
      PropertyProcessorService<DbUser, Boolean> booleanProcessor,
      UserDao userDao,
      UserGivenNamePropertyService userGivenNameProperty) {
    this.agentUserProvider = agentUserProvider;
    this.stringProcessor = stringProcessor;
    this.dataAccessLevelProcessor = dataAccessLevelProcessor;
    this.booleanPropertyProcessor = booleanProcessor;
    this.userDao = userDao;
    this.userGivenNameProperty = userGivenNameProperty;

    final BiFunction<DbUser, Boolean, Boolean> validateFunction = (t, prop) -> Objects
        .nonNull(prop);

    this.familyNameProperty = MutableProperty.<DbUser, String>builder()
        .setGetterFunction(DbUser::getFamilyName)
        .setSetterFunction(DbUser::setFamilyName)
        .setCommitterFunction(userDao::save)
        .setValidateFunction(this::validateString)
        .build();
    this.dataAccessLevelProperty = MutableProperty.<DbUser, DataAccessLevel>builder()
        .setSetterFunction(DbUser::setDataAccessLevelEnum)
        .setCommitterFunction()
        .setRequiredAuthorities(Authority.ACCESS_CONTROL_ADMIN)
        .setValidateFunction(validateFunction)
    .build();

    this.isDisabledProperty = MutableProperty.<DbUser, Boolean>builder()
        .setGetterFunction(DbUser::getDisabled)
        .setSetterFunction(DbUser::setDisabled)
        .setValidateFunction(validateFunction)
        .setRequiredAuthorities(Collections.singleton(Authority.ACCESS_CONTROL_ADMIN))
        .build();
  }

  @Override
  public DbUser setGivenName(DbUser user, String newValue) {
    return stringProcessor
        .update(userGivenNameProperty, user, user, newValue);
  }

  @Override
  public DbUser setFamilyName(DbUser target, String newValue) {
    try {
      return stringProcessor.update(
          familyNameProperty,
          agentUserProvider.get(),
          target,
          newValue);
    } catch (IllegalAccessException | AccessException e) {
      e.printStackTrace();
    }
    return target;
  }

  @Override
  public DbUser setDisabled(DbUser user, Boolean isDisabled) {
    try {
      return booleanPropertyProcessor
          .update(isDisabledProperty, agentUserProvider.get(), user, isDisabled);
    } catch (IllegalAccessException | AccessException e) {
      e.printStackTrace();
    }
    return user;
  }

  @Override
  public DbUser setDataAccessLevel(DbUser target, DataAccessLevel dataAccessLevel) {
    try {
      return dataAccessLevelProcessor.update(
          dataAccessLevelProperty, agentUserProvider.get(), target, dataAccessLevel);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean validateString(DbUser target, String value) {
    return !Strings.isNullOrEmpty(value) && value.length() < 1024; // OR WHATEVER
  }
}
