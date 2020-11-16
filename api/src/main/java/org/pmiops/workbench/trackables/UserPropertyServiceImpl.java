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
  private final TrackablePropertyProcessorService processor;
  private UserDao userDao;
  private final UserGivenNamePropertyService userGivenNamePropertyService;
  private final TrackableProperty<DbUser, String> familyNameProperty;
  private final TrackableProperty<DbUser, Boolean> isDisabledProperty;

  public UserPropertyServiceImpl(
      Provider<DbUser> agentUserProvider,
      TrackablePropertyProcessorService processor,
      UserDao userDao,
      UserGivenNamePropertyService userGivenNamePropertyService) {
    this.agentUserProvider = agentUserProvider;
    this.processor = processor;
    this.userDao = userDao;
    this.userGivenNamePropertyService = userGivenNamePropertyService;

    this.familyNameProperty = PropertySetter.<DbUser, String>builder()
        .setGetterFunction(DbUser::getFamilyName)
        .setSetterFunction(DbUser::setFamilyName)
        .setCommitterFunction(userDao::save)
        .setValidateFunction(this::validateString)
        .build();

    this.isDisabledProperty = PropertySetter.<DbUser, Boolean>builder()
        .setGetterFunction(DbUser::getDisabled)
        .setSetterFunction(DbUser::setDisabled)
        .setValidateFunction((t, p) -> p != null)
        .setRequiredAuthorities(Collections.singleton(Authority.ACCESS_CONTROL_ADMIN))
        .build();

    // Show bean and non-bean styles
    // TODO: this map could itself be provided as a Bean, but really no other classes need these
    //   instances of PropertySetter<>...
//    this.labelToSetter = ImmutableMap.of(
//        TrackedProperty.USER_GIVEN_NAME, userGivenNamePropertyService,
//        TrackedProperty.USER_DISABLED, PropertySetter.<DbUser, String>builder()
//        .setGetterFunction(DbUser::getFamilyName)
//        .build());
  }

  @Override
  public DbUser setGivenName(DbUser user, String newValue)
      throws AccessException, IllegalAccessException {
    return processor.process(userGivenNamePropertyService, user, user, newValue);
  }

  @Override
  public DbUser setFamilyName(DbUser target, String newValue) {
    try {
      return processor.process(
          familyNameProperty,
          target,
          agentUserProvider.get(),
          newValue);
    } catch (IllegalAccessException e) {
      e.printStackTrace(); // FIXME
    } catch (AccessException e) {
      e.printStackTrace();
    }
    return target;
  }

  @Override
  public DbUser setDisabled(DbUser user, Boolean isDiisabled) {
    try {
      return processor.process(isDisabledProperty, agentUserProvider.get(), user, isDiisabled);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (AccessException e) {
      e.printStackTrace();
    }
    return user;
  }

  private boolean validateString(DbUser targete, String value) {
    return !Strings.isNullOrEmpty(value) && value.length() < 1024; // OR WHATEVER
  }
}
