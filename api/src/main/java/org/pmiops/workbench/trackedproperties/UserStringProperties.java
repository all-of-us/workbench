package org.pmiops.workbench.trackedproperties;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.utils.EmailAddressUtils;

/**
 * We can implement all the processing logic in an enum like this one, but it's tricky to handle
 * multiplie properaty types.
 */
@Deprecated
public enum UserStringProperties implements TrackedProperty<DbUser, String> {
  CONTACT_EMAIL(
      "Contact Email",
      PropertyModifiability.USER_WRITEABLE,
      ImmutableSet.of(Authority.ACCESS_CONTROL_ADMIN),
      DbUser::getContactEmail,
      (DbUser user, String email) -> { user.setContactEmail(email); return user; },
      (DbUser unused, String email) -> EmailAddressUtils.isValidAddress(email),
      Collections.emptySet()),
  USERNAME(
      "Username",
      DbUser::getUsername)
  ;

  private final String displayName;
  private final PropertyModifiability modifiability;
  private final Set<Authority> authorities;
  private final Function<DbUser, String> accessor;
  private final BiFunction<DbUser, String, DbUser> mutator;
  private final BiFunction<DbUser, String, Boolean> validator;
  private final Set<NotificationType> notificationTypes;

  UserStringProperties(
      String displayName,
      PropertyModifiability propertyModifiability,
      Set<Authority> authorities,
      Function<DbUser, String> accessor,
      BiFunction<DbUser, String, DbUser> mutator,
      BiFunction<DbUser, String, Boolean> validator,
      Set<NotificationType> notificationTypes
      ) {
    this.displayName = displayName;
    this.modifiability = propertyModifiability;
    this.authorities = authorities;
    this.accessor = accessor;
    this.mutator = mutator;
    this.validator = validator;
    this.notificationTypes = notificationTypes;
  }

  /**
   * Read-only property constructor
   */
  UserStringProperties(
      String displayName,
      Function<DbUser,
          String> accessor
  ) {
    this(displayName,
        PropertyModifiability.READ_ONLY,
        Collections.emptySet(),
        accessor,
        (user, prop) -> user,
        (user, prop) -> true,
        Collections.emptySet());
  }

  @Override
  public PropertyModifiability getModifiability() {
    return modifiability;
  }

  @Override
  public Set<Authority> getRequiredAuthorities() {
    return authorities;
  }

  @Override
  public Function<DbUser, String> getAccessor() {
    return accessor;
  }

  @Override
  public Optional<BiFunction<DbUser, String, DbUser>> getMutator() {
    return Optional.ofNullable(mutator);
  }

  @Override
  public Function<DbUser, DbUser> getCommitter() {
    return null;
  }

  @Override
  public boolean isValid(DbUser target, String newValue) {
    return validator.apply(target, newValue);
  }
}
