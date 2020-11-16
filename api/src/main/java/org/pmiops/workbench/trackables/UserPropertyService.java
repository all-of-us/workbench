package org.pmiops.workbench.trackables;

import java.rmi.AccessException;
import org.pmiops.workbench.db.model.DbUser;

public interface UserPropertyService {
  DbUser setGivenName(DbUser user, String newValue) throws AccessException, IllegalAccessException;
  DbUser setFamilyName(DbUser user, String newValue);
  DbUser setDisabled(DbUser user, Boolean isDiisabled);
  // etc...
}
