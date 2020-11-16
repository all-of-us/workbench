package org.pmiops.workbench.trackables;

import org.pmiops.workbench.db.model.DbUser;

// TODO: this might make a better bean...
public interface PropertyTrackerService {
  TrackableProperty<DbUser, String> getPropertyService(String propertyLabel);
}
