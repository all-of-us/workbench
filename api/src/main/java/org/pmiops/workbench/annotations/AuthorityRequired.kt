package org.pmiops.workbench.annotations

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import org.pmiops.workbench.model.Authority

/**
 * Define granular permission requirements. Annotate controller methods and list Authority values
 * which a user must have (all of). Enforced by AuthInterceptor.
 */
// Annotations on methods are never inherited, even with the @Inherited annotation.
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME) // By default (CLASS), the VM may discard annotations.
annotation class AuthorityRequired(vararg val value: Authority)
