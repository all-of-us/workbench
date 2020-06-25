package org.pmiops.workbench.user;

import com.google.api.services.oauth2.model.Userinfoplus;
import org.pmiops.workbench.db.model.DbUser;

public interface DevUserRegistrationService {
  // Creates a user row in the RW system based on OAuth user info from GSuite. This method fetches
  // additional details from GSuite and the RW system, and calls UserService to create the
  // actual user database entry.
  DbUser createUser(Userinfoplus userInfo);
}
