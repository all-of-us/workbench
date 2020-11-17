package org.pmiops.workbench.trackables;

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
  public TARGET_T process(
      TrackableProperty<TARGET_T, PROPERTY_T> propertyService,
      DbUser agentUser,
      TARGET_T target,
      PROPERTY_T newValue) throws IllegalAccessException, AccessException {

    // validate authorities
    if (!userHasRequiredAuthorities(propertyService.getRequiredAuthorities())) {
      throw new AccessException("forbidden");
    }

    // validate new prop
    if (!propertyService.isValid(target, newValue)) {
      throw new IllegalAccessException("Bad argument");
    }

    // save old value for audit
    final Optional<PROPERTY_T> previousValue = Optional.ofNullable(
        propertyService.getValue(target));

    // set the value
    final TARGET_T updatedTarget = propertyService.setNewValue(target, newValue);

    // Commit to storage or some other external place that needs updating.
    final TARGET_T committedTarget = propertyService.commit(updatedTarget);

    // audit
    propertyService.auditChange(target, previousValue, Optional.of(newValue));

    // send notification
    propertyService.notifyUser(agentUser.getContactEmail(), "We did the thing for your acct.");

    return committedTarget;
  }

  private boolean userHasRequiredAuthorities(Set<Authority> requiredAuthorities) {
    final Set<Authority> userAuthorities = userProvider.get().getAuthoritiesEnum();
    return userAuthorities.containsAll(requiredAuthorities);
  }
}
