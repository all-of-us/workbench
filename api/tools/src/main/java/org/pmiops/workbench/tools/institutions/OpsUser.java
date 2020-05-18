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
class OpsUser extends User {
  final String action;

  private static final int columnLength = 6;
  private static final String institutionShortName = "AouOps";
  private static final String actionForInclusion = "To Remain";

  private static Optional<DbInstitution> aouOpsInst = Optional.empty();

  private static DbInstitution getOrInitInst(final InstitutionDao institutionDao) {
    if (!aouOpsInst.isPresent()) {
      aouOpsInst = institutionDao.findOneByShortName(institutionShortName);
    }

    return aouOpsInst.orElseThrow(
        () ->
            new RuntimeException(
                String.format("Could not find '%s' Institution in the DB", institutionShortName)));
  }

  private OpsUser(final String[] userLine) {
    this.firstName = userLine[0].trim();
    this.lastName = userLine[1].trim();
    this.contactEmail = userLine[2].trim();
    this.userName = userLine[3].trim();
    this.operationalRole = userLine[4].trim();
    this.action = userLine[5].trim();
  }

  static List<User> parseInput(final String filename) throws IOException {
    return User.readFile(filename, columnLength).stream()
        .map(OpsUser::new)
        .collect(Collectors.toList());
  }

  @Override
  void preCheck() {
    if (!action.equals(actionForInclusion)) {
      throw new RuntimeException(
          String.format(
              "User %s not marked as '%s' in the input CSV", userName, actionForInclusion));
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
