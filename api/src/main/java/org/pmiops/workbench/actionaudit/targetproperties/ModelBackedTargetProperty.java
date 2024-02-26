package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.function.Function;

/**
 * This interface is a contract to allow any model type T to support arbitrary String properties
 * without any changes to T. Intended to be implemented by enum classes.
 *
 * <p>This string is as close as we get to a formal schema for the audit log, so these values
 * shouldn't change. They may often match field names on the model object, but by design I'm trying
 * to avoid relying on those staying the same forever. Most of the schema is supposed to use
 * user-friendly names and concepts, without being coupled to our DB (with the exception of ID
 * columns).
 *
 * @param getPropertyName() Name of the property as saved in BigQuery
 * @param getExtractor() Function to compute a nullable String property from a T model object
 * @param T type of Model Class used in calculating the string properties
 */
public interface ModelBackedTargetProperty<T> extends SimpleTargetProperty {
  String getPropertyName();

  Function<T, String> getExtractor();
}
