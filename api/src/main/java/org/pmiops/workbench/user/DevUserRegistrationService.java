package org.pmiops.workbench.user;

import com.google.api.services.oauth2.model.Userinfoplus;
import org.pmiops.workbench.db.model.DbUser;

public interface DevUserRegistrationService {
  DbUser createUserFromUserInfo(Userinfoplus userInfo);
}
