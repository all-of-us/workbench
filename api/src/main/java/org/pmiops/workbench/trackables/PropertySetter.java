package org.pmiops.workbench.trackables;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.pmiops.workbench.model.Authority;

// A non-Bean approach for working wiht property setters. No direct dependencies,
// but those can be pulled in via lambda capture I think.
public class PropertySetter<TARGET_TYPE, PROPERTY_TYPE>
    implements TrackableProperty<TARGET_TYPE, PROPERTY_TYPE> {

  // TODO: am I just reimplementing Mockito now?
  private Set<Authority> requiredAuthorities = Collections.emptySet();

  private BiFunction<TARGET_TYPE, PROPERTY_TYPE, Boolean> validateFunction =
      (t, p) -> true;

  // todo: try this DbWorkspace dbWorkspace, FirecloudWorkspaceResponse firecloudWorkspaceResponse)
  private Function<TARGET_TYPE, PROPERTY_TYPE> getterFunction = t -> null;

  private BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE> setterFunction =
      (t, p) -> t;
  private Function<TARGET_TYPE, TARGET_TYPE> committerFunction =
      t -> t;

  public PropertySetter() {
    // default ctor to allow subclassing
  }

  public PropertySetter(Set<Authority> requiredAuthorities,
      BiFunction<TARGET_TYPE, PROPERTY_TYPE, Boolean> validateFunction,
      Function<TARGET_TYPE, PROPERTY_TYPE> getterFunction,
      BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE> setterFunction,
      Function<TARGET_TYPE, TARGET_TYPE> committerFunction) {
    this.requiredAuthorities = requiredAuthorities;
    this.validateFunction = validateFunction;
    this.getterFunction = getterFunction;
    this.setterFunction = setterFunction;
    this.committerFunction = committerFunction;
  }

  @Override
  public Set<Authority> getRequiredAuthorities() {
    return requiredAuthorities;
  }

  @Override
  public Function<TARGET_TYPE, PROPERTY_TYPE> getValueGetter() {
    return getterFunction;
  }

  @Override
  public BiFunction<TARGET_TYPE, PROPERTY_TYPE, TARGET_TYPE>
  getValueSetter() {
    return setterFunction;
  }

  @Override
  public Function<TARGET_TYPE, TARGET_TYPE> getValueCommitter() {
    return committerFunction;
  }

  @Override
  public boolean isValid(TARGET_TYPE target, PROPERTY_TYPE newValue) {
    return validateFunction.apply(target, newValue);
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

    public PropertySetter<TARGET_TYPE, PROPERTY_TYPE> build() {
      return new PropertySetter<>(
          requiredAuthorities,
          validateFunction,
          getterFunction,
          setterFunction,
          committerFunction);
    }
  }
}
