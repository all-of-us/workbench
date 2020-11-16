package org.pmiops.workbench.trackables;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.pmiops.workbench.model.Authority;

public interface TrackableProperty<TARGET_TYPE, PROPERTY_TYPE> {

  enum NotificationType {
    NONE,
    EMAIL,
    SNAPCHAT,
    IN_APP
  }

  // Authorities to check before applying the change. Typically used
  // to guard properties only updatable by administrators.
  default Set<Authority> getRequiredAuthorities() {
    return Collections.emptySet();
  };

  Function<TARGET_TYPE, PROPERTY_TYPE> getValueGetter();

  // Return a function that, when called, sets the new value on the provivded target.
  BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE> getValueSetter();

  // Return a function that commits a change on the target object (e.g. saving to a data store).
  Function<TARGET_TYPE, TARGET_TYPE> getValueCommitter();

  // Return true if the new value is a valid value for this target.
  // Best effort. There could still be condition that cause the value not to save.
  boolean isValid(TARGET_TYPE target, PROPERTY_TYPE newValue);


  default void auditChange(TARGET_TYPE target, Optional<PROPERTY_TYPE> previousValue, Optional<PROPERTY_TYPE> newValue) {
    // no-op if not overridden
  }

  default NotificationType getNotificationType() {
    return NotificationType.NONE;
  }

  default void notifyUser(String emailAddress, String text) {
    // no-op if called
  }
}
