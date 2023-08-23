package org.pmiops.workbench.utils;

import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;

public class PresetData {
  public static DbUser createDbUser() {
    return new DbUser();
  }

  public static DbInstitution createDbInstitution() {
    // shortName and displayName must be unique. Additional calls within a single test should
    // override them as-needed
    return new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");
  }

  public static DbVerifiedInstitutionalAffiliation createDbVerifiedInstitutionalAffiliation(
      DbInstitution institution, DbUser user) {
    return new DbVerifiedInstitutionalAffiliation()
        .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
        .setInstitution(institution)
        .setUser(user);
  }
}
