package org.pmiops.workbench.trackedproperties;

import java.rmi.AccessException;
import java.util.Optional;
import java.util.Set;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.springframework.stereotype.Service;

@Service
public class PropertyProcessorServiceImpl<TARGET_T, PROPERTY_T>
    implements PropertyProcessorService<TARGET_T, PROPERTY_T> {

  private final Provider<DbUser> userProvider;

  public PropertyProcessorServiceImpl(Provider<DbUser> userProvider) {
    this.userProvider = userProvider;
  }

  // Do the needful steps in order.
  @Override
  public PropertyUpdateResult<TARGET_T> update(
      TrackedProperty<TARGET_T, PROPERTY_T> property,
      DbUser agentUser,
      TARGET_T target,
      PROPERTY_T newValue) {

    // validate authorities
    if (!userHasRequiredAuthorities(property.getRequiredAuthorities())) {
      return new PropertyUpdateResult<>(target, PropertyUpdateStatus.MISSING_REQUIRED_AUTHORITY);
    }

    // validate new prop
    if (!property.isValid(target, newValue)) {
      return new PropertyUpdateResult<>(target, PropertyUpdateStatus.BAD_ARGUMENT);
    }

    // save old value for audit
    final Optional<PROPERTY_T> previousValue = Optional.ofNullable(
        property.getValue(target));

    // set the value
    final TARGET_T updatedTarget = property.setNewValue(target, newValue);

    // Commit to storage or some other external place that needs updating.
    final TARGET_T committedTarget = property.commit(updatedTarget);

    // audit
    property.auditChange(target, previousValue, Optional.of(newValue));

    // send notification
    property.notifyUser(agentUser.getContactEmail(), "We did the thing for your acct.");

    return new PropertyUpdateResult<>(committedTarget, PropertyUpdateStatus.SUCCEEDED);
  }

  private boolean userHasRequiredAuthorities(Set<Authority> requiredAuthorities) {
    final Set<Authority> userAuthorities = userProvider.get().getAuthoritiesEnum();
    return userAuthorities.containsAll(requiredAuthorities);
  }
}
