package org.pmiops.workbench.tools.institutions;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;

/**
 * Structure researcher user data according to the format of the source CSV:
 *
 * <p>First Name, Last Name, Institutional Email, WB User Name, Role, Institution, "Institutional
 * DUA Signed?", "REDCap Complete?"
 */
class Researcher extends UserToAffiliate {
  // common fields from User: firstName, lastName, contactEmail, userName
  final InstitutionalRole institutionalRole;
  final Optional<String> institutionalRoleOther;
  final String institutionDisplayName;
  final String duaSigned;
  final String redCapComplete;

  private static final int COLUMN_LENGTH = 8;
  private static final String AFFIRMATIVE = "Yes";

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
    this.firstName = clean(userLine[0]);
    this.lastName = clean(userLine[1]);
    this.contactEmail = clean(userLine[2]);
    this.userName = clean(userLine[3]);
    this.institutionalRole = parseInstitutionalRole(clean(userLine[4]));
    this.institutionalRoleOther = parseInstitutionalRoleOther(clean(userLine[4]));
    this.institutionDisplayName = parseInstitutionDisplayName(clean(userLine[5]));
    this.duaSigned = clean(userLine[6]);
    this.redCapComplete = clean(userLine[7]);
  }

  // special case a few Other roles

  private boolean isPreApprovedOtherRole(String rawRole) {
    return rawRole.equals("Multispecialty Group Practice")
        || rawRole.equals("Non-tenure track researcher");
  }

  private InstitutionalRole parseInstitutionalRole(final String rawRole) {
    final InstitutionalRole parsedRole = roleMap.get(rawRole);
    if (parsedRole != null) {
      return parsedRole;
    }

    // the roles in this input file have a high number of not-quite-matches
    // special-case those where the intent is clear

    if (rawRole.startsWith("Senior Researcher")) {
      return InstitutionalRole.SENIOR_RESEARCHER;
    }

    if (rawRole.startsWith("Mid-Career")) {
      return InstitutionalRole.MID_CAREER;
    }

    if (rawRole.startsWith("Early Career")) {
      return InstitutionalRole.EARLY_CAREER;
    }

    if (rawRole.startsWith("Late Career")) {
      return InstitutionalRole.LATE_CAREER;
    }

    if (rawRole.startsWith("Project Personnel")) {
      return InstitutionalRole.PROJECT_PERSONNEL;
    }

    if (rawRole.startsWith("Research Fellow")) {
      return InstitutionalRole.FELLOW;
    }

    if (rawRole.startsWith("Graduate Trainee")) {
      return InstitutionalRole.TRAINEE;
    }

    if (rawRole.startsWith("Data Analyst")) {
      return InstitutionalRole.PROJECT_PERSONNEL;
    }

    // special case a few Other roles
    if (isPreApprovedOtherRole(rawRole)) {
      return InstitutionalRole.OTHER;
    }

    throw new RuntimeException(
        String.format("Role '%s' for user '%s' could not be matched", rawRole, contactEmail));
  }

  private Optional<String> parseInstitutionalRoleOther(final String rawRole) {
    // special case a few Other roles
    if (isPreApprovedOtherRole(rawRole)) {
      return Optional.of(rawRole);
    }

    return Optional.empty();
  }

  private String parseInstitutionDisplayName(final String rawName) {
    // special-case this input value because the intent is clear
    if (rawName.equals("Scripps Research Institute")) {
      return "Scripps Research";
    }

    return rawName;
  }

  static List<UserToAffiliate> parseInput(final String filename) throws IOException {
    return UserToAffiliate.readFile(filename, COLUMN_LENGTH).stream()
        .map(Researcher::new)
        .collect(Collectors.toList());
  }

  @Override
  void preCheck() {
    if (!duaSigned.equals(AFFIRMATIVE)) {
      throw new RuntimeException(
          String.format(
              "The Institutional DUA Signature for user '%s' was not marked as '%s' in the input CSV",
              userName, AFFIRMATIVE));
    }
    if (!redCapComplete.equals(AFFIRMATIVE)) {
      throw new RuntimeException(
          String.format(
              "The REDCap completion status for user '%s' was not marked as '%s' in the input CSV",
              userName, redCapComplete));
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

    final DbVerifiedInstitutionalAffiliation affiliation =
        new DbVerifiedInstitutionalAffiliation()
            .setInstitution(dbInstitution)
            .setUser(dbUser)
            .setInstitutionalRoleEnum(institutionalRole);

    institutionalRoleOther.ifPresent(affiliation::setInstitutionalRoleOtherText);

    return affiliation;
  }
}
