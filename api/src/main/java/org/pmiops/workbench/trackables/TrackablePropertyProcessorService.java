package org.pmiops.workbench.trackables;

import java.rmi.AccessException;
import org.pmiops.workbench.db.model.DbUser;

public interface TrackablePropertyProcessorService {
 <TARGET_TYPE, PROPERTY_TYPE> TARGET_TYPE process(
     TrackableProperty<TARGET_TYPE, PROPERTY_TYPE> propertyService,
      DbUser agentUser,
      TARGET_TYPE target,
      PROPERTY_TYPE newValue) throws IllegalAccessException, AccessException;
}
