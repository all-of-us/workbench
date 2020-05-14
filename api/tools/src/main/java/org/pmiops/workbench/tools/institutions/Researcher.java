package org.pmiops.workbench.tools.institutions;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;

/**
 * Structure researcher user data according to the format of the source CSV:
 *
 * <p>First Name, Last Name, Institutional Email, WB User Name, Role, Institution, "Institutional
 * DUA Signed?"
 */
class Researcher extends User {
  final String institutionDisplayName;
  final String duaSigned;

  private static final int columnLength = 7;
  private static final String affirmative = "Yes";

  // this mapping is only stored in the UI so we copy it here
  // (see institutionalRoleOptions in account-creation-options.tsx)
  private static final Map<String, InstitutionalRole> roleMap =
      ImmutableMap.<String, InstitutionalRole>builder()
          .put("Undergraduate (Bachelor level) student", InstitutionalRole.UNDERGRADUATE)
          .put(
              "Graduate trainee (Current student in a Masters, PhD, or Medical school training program)",
              InstitutionalRole.TRAINEE)
          .put(
              "Research fellow (a post-doctoral fellow or medical resident in training)",
              InstitutionalRole.FELLOW)
          .put("Early career tenure-track researcher", InstitutionalRole.EARLY_CAREER)
          .put("Mid-career tenured researcher", InstitutionalRole.MID_CAREER)
          .put("Late career tenured researcher", InstitutionalRole.LATE_CAREER)
          .put(
              "Project Personnel (eg: Research Assistant, Data Analyst, Project Manager, Research Coordinator or other roles)",
              InstitutionalRole.PROJECT_PERSONNEL)
          .put("Research Assistant (pre-doctoral)", InstitutionalRole.PRE_DOCTORAL)
          .put(
              "Research associate (post-doctoral; early/mid career)",
              InstitutionalRole.POST_DOCTORAL)
          .put(
              "Senior Researcher (PI/Team Lead, senior scientist)",
              InstitutionalRole.SENIOR_RESEARCHER)
          .put("Teacher/Instructor/Professor", InstitutionalRole.TEACHER)
          .put("Student", InstitutionalRole.STUDENT)
          .put("Administrator", InstitutionalRole.ADMIN)
          .put("Other (free text)", InstitutionalRole.OTHER)
          .build();

  private Researcher(final String[] userLine) {
    this.firstName = userLine[0].trim();
    this.lastName = userLine[1].trim();
    this.contactEmail = userLine[2].trim();
    this.userName = userLine[3].trim();
    this.operationalRole = userLine[4].trim();
    this.institutionDisplayName = userLine[5].trim();
    this.duaSigned = userLine[5].trim();
  }

  static Stream<User> parseInput(final String filename) throws IOException {
    return User.readFile(filename, columnLength).map(Researcher::new);
  }

  @Override
  void preCheck() {
    if (!duaSigned.equals(affirmative)) {
      throw new RuntimeException(
          String.format(
              "User's Institution %s not marked as '%s' in the input CSV", userName, affirmative));
    }
  }

  @Override
  DbVerifiedInstitutionalAffiliation toAffiliation(
      final DbUser dbUser, final InstitutionDao institutionDao) {
    final DbInstitution dbInstitution =
        institutionDao
            .findOneByDisplayName(institutionDisplayName)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        String.format(
                            "Could not find '%s' Institution in the DB", institutionDisplayName)));

    final InstitutionalRole parsedRole = roleMap.get(operationalRole);
    if (parsedRole == null) {
      throw new RuntimeException(
          String.format(
              "Role '%s' for user '%s' not found in the role map", operationalRole, contactEmail));
    }

    return new DbVerifiedInstitutionalAffiliation()
        .setInstitution(dbInstitution)
        .setUser(dbUser)
        .setInstitutionalRoleEnum(parsedRole);
  }
}
