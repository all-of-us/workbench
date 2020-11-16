package org.pmiops.workbench.trackables;

import java.rmi.AccessException;
import org.pmiops.workbench.db.model.DbUser;

/**
 * TODO(jaycarlton) I believe this can be done in TrackableProperty (partially)
 * @param <TARGET_T>
 * @param <PROPERTY_T>
 */
public interface PropertyProcessorService<TARGET_T, PROPERTY_T> {
 TARGET_T process(
     TrackableProperty<TARGET_T, PROPERTY_T> property,
      DbUser agentUser,
      TARGET_T target,
      PROPERTY_T newValue) throws IllegalAccessException, AccessException;
}
