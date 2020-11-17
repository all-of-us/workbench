package org.pmiops.workbench.trackables;

import java.rmi.AccessException;
import java.util.Collections;
import java.util.Objects;
import javax.inject.Provider;
import org.elasticsearch.common.Strings;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.springframework.stereotype.Service;

@Service
public class UserPropertyServiceImpl implements UserPropertyService {

  // TODO(jaycarlton):  I guess we can't rely on the template magic working on a static
  //   class inside a subclass specializing a generic interface. My kingdom for a typedef.
//  private static class StringPropSetter extends PropertySetter<DbUser, String> {};
//  private static class StringPropSetterBuilder extends PropertySetter.Builder<DbUser, String> {};

  private final Provider<DbUser> agentUserProvider;
  private final PropertyProcessorService<DbUser, String> stringPropertyProcessor;
  private final PropertyProcessorService<DbUser, Long> longPropertyProcessor;
  private final PropertyProcessorService<DbUser, Boolean> booleanPropertyProcessor;

  private final UserDao userDao;
  private final UserGivenNamePropertyService userGivenNamePropertyService;
  private final TrackableProperty<DbUser, String> familyNameProperty;
  private final TrackableProperty<DbUser, Boolean> isDisabledProperty;

  public UserPropertyServiceImpl(
      Provider<DbUser> agentUserProvider,
      PropertyProcessorService<DbUser, String> stringPropertyProcessor,
      PropertyProcessorService<DbUser, Long> longPropertyProcessor,
      PropertyProcessorService<DbUser, Boolean> booleanPropertyProcessor,
      UserDao userDao,
      UserGivenNamePropertyService userGivenNamePropertyService) {
    this.agentUserProvider = agentUserProvider;
    this.stringPropertyProcessor = stringPropertyProcessor;
    this.booleanPropertyProcessor = booleanPropertyProcessor;
    this.userDao = userDao;
    this.userGivenNamePropertyService = userGivenNamePropertyService;

    this.familyNameProperty = MutableProperty.<DbUser, String>builder()
        .setGetterFunction(DbUser::getFamilyName)
        .setSetterFunction(DbUser::setFamilyName)
        .setCommitterFunction(userDao::save)
        .setValidateFunction(this::validateString)
        .build();

    this.isDisabledProperty = MutableProperty.<DbUser, Boolean>builder()
        .setGetterFunction(DbUser::getDisabled)
        .setSetterFunction(DbUser::setDisabled)
        .setValidateFunction((t, prop) -> Objects.nonNull(prop))
        .setRequiredAuthorities(Collections.singleton(Authority.ACCESS_CONTROL_ADMIN))
        .build();
    this.longPropertyProcessor = null;
  }

  @Override
  public DbUser setGivenName(DbUser user, String newValue)
      throws AccessException, IllegalAccessException {
    return stringPropertyProcessor
        .process(userGivenNamePropertyService, user, user, newValue);
  }

  @Override
  public DbUser setFamilyName(DbUser target, String newValue) {
    try {
      return stringPropertyProcessor.process(
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
  public DbUser setDisabled(DbUser user, Boolean isDiisabled) {
    try {
      return booleanPropertyProcessor
          .process(isDisabledProperty, agentUserProvider.get(), user, isDiisabled);
    } catch (IllegalAccessException | AccessException e) {
      e.printStackTrace();
    }
    return user;
  }

  private boolean validateString(DbUser targete, String value) {
    return !Strings.isNullOrEmpty(value) && value.length() < 1024; // OR WHATEVER
  }
}
