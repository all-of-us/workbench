package org.pmiops.workbench.trackedproperties;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ReadOnlyProperty<TARGET_TYPE, PROPERTY_TYPE> implements
    TrackedProperty<TARGET_TYPE, PROPERTY_TYPE> {

  @Override
  public PropertyModifiability getModifiability() {
    return null;
  }

  @Override
  public Function<TARGET_TYPE, PROPERTY_TYPE> getAccessor() {
    return null;
  }

  @Override
  public PROPERTY_TYPE getValue(TARGET_TYPE target) {
    return null;
  }

  @Override
  public Optional<BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE>> getMutator() {
    return Optional.empty();
  }

  @Override
  public TARGET_TYPE setNewValue(TARGET_TYPE target, PROPERTY_TYPE newValue) {
    return null;
  }

  @Override
  public Function<TARGET_TYPE, TARGET_TYPE> getCommitter() {
    return null;
  }

  @Override
  public boolean isValid(TARGET_TYPE target, PROPERTY_TYPE newValue) {
    return false;
  }

  @Override
  public Set<NotificationType> getNotificationTypes() {
    return null;
  }

  @Override
  public void notifyUser(String emailAddress, String text) {

  }
}
