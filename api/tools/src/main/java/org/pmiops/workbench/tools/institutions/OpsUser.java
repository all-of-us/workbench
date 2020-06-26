package org.pmiops.workbench.tools.institutions;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;

/**
 * Structure the incoming ops user data according to the format of the source CSV:
 *
 * <p>First Name,Last Name,Email,"Workbench Email",Institution,Role,Action
 */
class OpsUser extends UserToAffiliate {
  // common fields from User: firstName, lastName, contactEmail, userName
  final String operationalRole;
  final String action;

  private static final int COLUMN_LENGTH = 6;
  private static final String INSTITUTION_SHORT_NAME = "AouOps";
  private static final String ACTION_FOR_INCLUSION = "To Remain";

  private static Optional<DbInstitution> aouOpsInst = Optional.empty();

  private static DbInstitution getOrInitInst(final InstitutionDao institutionDao) {
    if (!aouOpsInst.isPresent()) {
      aouOpsInst = institutionDao.findOneByShortName(INSTITUTION_SHORT_NAME);
    }

    return aouOpsInst.orElseThrow(
        () ->
            new RuntimeException(
                String.format(
                    "Could not find '%s' Institution in the DB", INSTITUTION_SHORT_NAME)));
  }

  private OpsUser(final String[] userLine) {
    this.firstName = clean(userLine[0]);
    this.lastName = clean(userLine[1]);
    this.contactEmail = clean(userLine[2]);
    this.userName = clean(userLine[3]);
    this.operationalRole = clean(userLine[4]);
    this.action = clean(userLine[5]);
  }

  static List<UserToAffiliate> parseInput(final String filename) throws IOException {
    return UserToAffiliate.readFile(filename, COLUMN_LENGTH).stream()
        .map(OpsUser::new)
        .collect(Collectors.toList());
  }

  @Override
  void preCheck() {
    if (!action.equals(ACTION_FOR_INCLUSION)) {
      throw new RuntimeException(
          String.format(
              "User %s not marked as '%s' in the input CSV", userName, ACTION_FOR_INCLUSION));
    }
  }

  @Override
  DbVerifiedInstitutionalAffiliation toAffiliation(
      final DbUser dbUser, final InstitutionDao institutionDao) {
    return new DbVerifiedInstitutionalAffiliation()
        .setInstitution(getOrInitInst(institutionDao))
        .setUser(dbUser)
        .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
        .setInstitutionalRoleOtherText(operationalRole);
  }
}
