package org.pmiops.workbench.tools.institutions;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbUser;

/**
 * Structure the incoming ops user data according to the format of the source CSV:
 *
 * <p>First Name,Last Name,Email,"Workbench Email",Institution,Role,Action
 */
class OpsUser {
  final String firstName;
  final String lastName;
  final String contactEmail; // "Email" to DRC admin staff
  final String userName; // "Workbench Email" to DRC admin staff
  final String operationalRole;
  final String action;

  static final int COLUMNS = 6;

  private static final Logger log = Logger.getLogger(OpsUser.class.getName());

  private OpsUser(final String[] userLine) {
    this.firstName = userLine[0].trim();
    this.lastName = userLine[1].trim();
    this.contactEmail = userLine[2].trim();
    this.userName = userLine[3].trim();
    this.operationalRole = userLine[4].trim();
    this.action = userLine[5].trim();
  }

  static List<OpsUser> parseInput(final String filename) throws IOException {
    try (final CSVReader reader = new CSVReader(new FileReader(filename))) {
      // consume and sanity-check header line
      final String[] headerLine = reader.readNext();
      if (headerLine.length != COLUMNS) {
        throw new RuntimeException(
            String.format(
                "Expected %d columns in input file. Was: %d", COLUMNS, headerLine.length));
      }

      return StreamSupport.stream(reader.spliterator(), false)
          .map(OpsUser::new)
          .collect(Collectors.toList());
    }
  }

  private void checkField(String dbValue, String csvValue, String fieldName) {
    if (!dbValue.equals(csvValue)) {
      log.warning(
          String.format(
              "CSV and DB values do not match for user '%s', field '%s'. CSV = %s, DB = %s",
              userName, fieldName, csvValue, dbValue));
    }
  }

  public DbUser dbCheck(UserDao userDao, VerifiedInstitutionalAffiliationDao affiliationDao) {
    final DbUser dbUser = userDao.findUserByUsername(userName);

    // fatal errors: something is quite wrong and we need to recheck our assumptions!

    if (dbUser == null) {
      throw new RuntimeException(String.format("User %s was not found in the DB", userName));
    }

    affiliationDao
        .findFirstByUser(dbUser)
        .ifPresent(
            affiliation -> {
              throw new RuntimeException(
                  String.format(
                      "User %s is already affiliated with institution '%s'",
                      userName, affiliation.getInstitution().getDisplayName()));
            });

    // some of these are acceptable mismatches, like "Dan" instead of "Daniel"
    // warn only, don't stop

    checkField(dbUser.getGivenName(), firstName, "First Name");
    checkField(dbUser.getFamilyName(), lastName, "Last Name");
    checkField(dbUser.getContactEmail(), contactEmail, "Contact Email");

    return dbUser;
  }
}
