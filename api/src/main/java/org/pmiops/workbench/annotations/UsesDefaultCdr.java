package org.pmiops.workbench.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.pmiops.workbench.config.WorkbenchConfig;

/**
 * Annotation indicating that the method or class uses the default CDR version (as specified
 * in {@link WorkbenchConfig#cdr}, rather than specifying a particular one in the request.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UsesDefaultCdr {

}
