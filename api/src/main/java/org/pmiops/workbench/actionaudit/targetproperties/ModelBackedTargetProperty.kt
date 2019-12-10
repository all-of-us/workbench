package org.pmiops.workbench.actionaudit.targetproperties

/**
* This interface is a contract to allow any model type T to support
* arbitrary String properties without any changes to T. Intended
* to be implemented by enum classes.
*
* This string is as close as we get to a formal schema for the audit
* log, so these values shouldn't change. They may often match field
* names on the model object, but by design I'm trying to avoid relying
* on those staying the same forever. Most of the schema is supposed to
* use user-friendly names and concepts, without being coupled to our DB
* (with the exception of ID columns).
 * @param propertyName Name of the property as saved in BigQuery
 * @param extractor Function to compute a nullable String property from a T model object
 * @param T type of Model Class used in calculating the string properties
*/
interface ModelBackedTargetProperty<T> : SimpleTargetProperty {
    override val propertyName: String
    val extractor: (T) -> String?
}
