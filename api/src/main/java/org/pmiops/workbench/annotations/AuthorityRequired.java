package org.pmiops.workbench.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.pmiops.workbench.model.Authority;

// Annotations on methods are never inherited, even with the @Inherited annotation.
@Retention(RetentionPolicy.RUNTIME)  // By default (CLASS), the VM may discard annotations.
public @interface AuthorityRequired {
  Authority[] value();
}
