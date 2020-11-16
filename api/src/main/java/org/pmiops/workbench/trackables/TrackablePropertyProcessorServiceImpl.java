package org.pmiops.workbench.trackables;

import java.rmi.AccessException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Authority;
import org.springframework.stereotype.Service;

@Service
public class TrackablePropertyProcessorServiceImpl implements TrackablePropertyProcessorService {

  private final Provider<DbUser> userProvider;

  public TrackablePropertyProcessorServiceImpl(Provider<DbUser> userProvider) {
    this.userProvider = userProvider;
  }

  // Do the needful steps in order.
  @Override
  public <TARGET_TYPE, PROPERTY_TYPE> TARGET_TYPE process(
      TrackableProperty<TARGET_TYPE, PROPERTY_TYPE> propertyService,
      DbUser agentUser,
      TARGET_TYPE target,
      PROPERTY_TYPE newValue) throws IllegalAccessException, AccessException {

    // validate authorities
    if (!userHasRequiredAuthorities(propertyService.getRequiredAuthorities())) {
      throw new AccessException("forbidden");
    }

    // validate new prop
    if (!propertyService.isValid(target, newValue)) {
      throw new IllegalAccessException("Bad argument");
    }

    // save old value for audit
    final Optional<PROPERTY_TYPE> previousValue = Optional.ofNullable(
        propertyService.getValueGetter().apply(target));

    // set the value
    final TARGET_TYPE updatedTarget = propertyService.getValueSetter().apply(target, newValue);

    final TARGET_TYPE committedTarget = propertyService.getValueCommitter().apply(updatedTarget);

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
