package org.pmiops.workbench.trackedproperties;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.pmiops.workbench.model.Authority;

// A non-Bean approach for working with property setters. No direct dependencies,
// but those can be pulled in via lambda capture I think.
// TODO(jaycarlton): make this an abstract class so that it's easier to ues for both writeable and readable
//    properties
public class MutableProperty<TARGET_TYPE, PROPERTY_TYPE>
    implements TrackedProperty<TARGET_TYPE, PROPERTY_TYPE> {

  private final Set<Authority> requiredAuthorities;
  private final BiFunction<TARGET_TYPE, PROPERTY_TYPE, Boolean> validator;
  private final Function<TARGET_TYPE, PROPERTY_TYPE> accessor;
  private final BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE> mutator;
  private final Function<TARGET_TYPE, TARGET_TYPE> committerFunction;

  public MutableProperty(Set<Authority> requiredAuthorities,
      BiFunction<TARGET_TYPE, PROPERTY_TYPE, Boolean> validateFunction,
      Function<TARGET_TYPE, PROPERTY_TYPE> accessor,
      BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE> mutator,
      Function<TARGET_TYPE, TARGET_TYPE> committerFunction) {
    this.requiredAuthorities = requiredAuthorities;
    this.validator = validateFunction;
    this.accessor = accessor;
    this.mutator = mutator;
    this.committerFunction = committerFunction;
  }

  @Override
  public Set<NotificationType> getNotificationTypes() {
    return null;
  }

  @Override
  public PropertyModifiability getModifiability() {
    return PropertyModifiability.USER_WRITEABLE;
  }

  @Override
  public Set<Authority> getRequiredAuthorities() {
    return requiredAuthorities;
  }

  @Override
  public Function<TARGET_TYPE, PROPERTY_TYPE> getAccessor() {
    return accessor;
  }

  @Override
  public Optional<BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE>> getMutator() {
    return Optional.ofNullable(mutator);
  }

  @Override
  public Function<TARGET_TYPE, TARGET_TYPE> getCommitter() {
    return committerFunction;
  }

  @Override
  public boolean isValid(TARGET_TYPE target, PROPERTY_TYPE newValue) {
    return validator.apply(target, newValue);
  }

  @Override
  public void auditChange(TARGET_TYPE target, Optional<PROPERTY_TYPE> previousValue,
      Optional<PROPERTY_TYPE> newValue) {

  }

  public static <TARGET_TYPE, PROPERTY_TYPE>
  Builder<TARGET_TYPE, PROPERTY_TYPE> builder() {
    return new Builder<>();
  }

  public static class Builder<TARGET_TYPE, PROPERTY_TYPE> {
    public static <TARGET_TYPE, PROPERTY_TYPE>
    Builder<TARGET_TYPE, PROPERTY_TYPE> builder() {
      return new Builder<>();
    }

    private Set<Authority> requiredAuthorities = Collections.emptySet();
    // default to no validation
    private BiFunction<TARGET_TYPE, PROPERTY_TYPE, Boolean> validateFunction =
        (target, newValue) ->  true;

    private Function<TARGET_TYPE, PROPERTY_TYPE> getterFunction;
    private BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE> setterFunction;
    private Function<TARGET_TYPE, TARGET_TYPE> committerFunction;

    public Builder<TARGET_TYPE, PROPERTY_TYPE> setRequiredAuthorities(Set<Authority> requiredAuthorities) {
      this.requiredAuthorities = requiredAuthorities;
      return this;
    }

    public Builder<TARGET_TYPE, PROPERTY_TYPE> setValidateFunction(
        BiFunction<TARGET_TYPE, PROPERTY_TYPE, Boolean> validateFunction) {
      this.validateFunction = validateFunction;
      return this;
    }

    public Builder<TARGET_TYPE, PROPERTY_TYPE> setGetterFunction(
        Function<TARGET_TYPE, PROPERTY_TYPE> getterFunction) {
      this.getterFunction = getterFunction;
      return this;
    }

    // Argument to function to set takes a target and property and returns
    // updated property.
    public Builder<TARGET_TYPE, PROPERTY_TYPE> setSetterFunction(
        BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE> setterFunction) {
      this.setterFunction = setterFunction;
      return this;
    }

    public Builder<TARGET_TYPE, PROPERTY_TYPE> setCommitterFunction(
        Function<TARGET_TYPE, TARGET_TYPE> committerFunction) {
      this.committerFunction = committerFunction;
      return this;
    }

    // We want setters that return the updated target for convenience and
    // consistency. Many Db entity class
    // setters don't do this, so this wrapper is helpful.
    // Usage: builder.setSetterWithReturn(DbUser::setFamilyName);
    public Builder<TARGET_TYPE, PROPERTY_TYPE> setSetterFunction(
        BiConsumer<TARGET_TYPE, PROPERTY_TYPE> setter) {
      this.setterFunction = (t, p) -> {
        setter.accept(t, p);
        return t;
      };
      return this;
    }

    public MutableProperty<TARGET_TYPE, PROPERTY_TYPE> build() {
      return new MutableProperty<>(
          requiredAuthorities,
          validateFunction,
          getterFunction,
          setterFunction,
          committerFunction);
    }
  }
}
