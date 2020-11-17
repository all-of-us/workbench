package org.pmiops.workbench.trackedproperties;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.pmiops.workbench.model.Authority;


public interface TrackedProperty<TARGET_T, PROPERTY_T> {

  PropertyModifiability getModifiability();

  // Authorities to check before applying the change. Typically used
  // to guard properties only updatable by administrators.
  default Set<Authority> getRequiredAuthorities() {
    return Collections.emptySet();
  };

  Function<TARGET_T, PROPERTY_T> getAccessor();

  default PROPERTY_T getValue(TARGET_T target) {
    return getAccessor().apply(target);
  }

  // Return a function that, when called, sets the new value on the provided target.
  default Optional<BiFunction<TARGET_T, PROPERTY_T, TARGET_T>> getMutator() {
    return Optional.empty();
  }

  default TARGET_T setNewValue(TARGET_T target, PROPERTY_T newValue) {
      return getMutator()
        .map(mutator -> mutator.apply(target, newValue))
        .orElse(target);
  }

  // Return a function that commits a change on the target object (e.g. saving to a data store).
  // Works as a silent no-op unless overridden.
  default Function<TARGET_T, TARGET_T> getCommitter() {
    return Function.identity();
  }

  default TARGET_T commit(TARGET_T uncommittedTarget) {
    getCommitter().apply(uncommittedTarget);
    return uncommittedTarget;
  }

  // Return true if the new value is a valid value for this target.
  // Best effort. There could still be condition that cause the value not to save.
  boolean isValid(TARGET_T target, PROPERTY_T newValue);


  default void auditChange(TARGET_T target, Optional<PROPERTY_T> previousValue, Optional<PROPERTY_T> newValue) {
    // no-op if not overridden
  }

  default Set<NotificationType> getNotificationTypes() {
    return Collections.emptySet();
  }

  default void notifyUser(String emailAddress, String text) {
    // no-op if called
  }
}
