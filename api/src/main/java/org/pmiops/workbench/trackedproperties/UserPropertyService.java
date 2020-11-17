package org.pmiops.workbench.trackedproperties;

import java.rmi.AccessException;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;

public interface UserPropertyService {
  PropertyUpdateResult<DbUser> setGivenName(DbUser user, String newValue) throws AccessException, IllegalAccessException;
  PropertyUpdateResult<DbUser> setFamilyName(DbUser user, String newValue);
  PropertyUpdateResult<DbUser> setDisabled(DbUser user, Boolean isDisabled);
  PropertyUpdateResult<DbUser> setDataAccessLevel(DbUser user, DataAccessLevel dataAccessLevel);
}
