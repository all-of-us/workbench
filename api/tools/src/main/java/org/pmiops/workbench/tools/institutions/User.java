package org.pmiops.workbench.tools.institutions;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;

public abstract class User {
  private static final Logger log = Logger.getLogger(User.class.getName());

  String firstName;
  String lastName;
  String contactEmail;
  String userName;
  String operationalRole;

  abstract void preCheck();

  abstract DbVerifiedInstitutionalAffiliation toAffiliation(
      final DbUser dbUser, final InstitutionDao institutionDao);

  static Stream<String[]> readFile(final String filename, final int columnLength) throws IOException {
    try (final CSVReader reader = new CSVReader(new FileReader(filename))) {
      // consume and sanity-check header line
      final String[] headerLine = reader.readNext();
      if (headerLine.length != columnLength) {
        throw new RuntimeException(
            String.format(
                "Expected %d columns in input file. Was: %d", columnLength, headerLine.length));
      }

      return StreamSupport.stream(reader.spliterator(), false);
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

  private DbUser dbCheck(
      final UserDao userDao, final VerifiedInstitutionalAffiliationDao affiliationDao) {
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

  DbVerifiedInstitutionalAffiliation prepareAffiliation(
      final UserDao userDao,
      final InstitutionDao institutionDao,
      final VerifiedInstitutionalAffiliationDao affiliationDao) {
    preCheck();
    final DbUser dbUser = dbCheck(userDao, affiliationDao);
    return toAffiliation(dbUser, institutionDao);
  }
}
