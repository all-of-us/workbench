package org.pmiops.workbench.profile;

import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Profile;

public interface ProfileService {

  Profile getProfile(DbUser user);

  void validateInstitutionalAffiliation(Profile profile);
}
