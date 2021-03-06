package org.pmiops.workbench.tools.institutions;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;

public abstract class UserToAffiliate {
  private static final Logger LOGGER = Logger.getLogger(UserToAffiliate.class.getName());

  String firstName;
  String lastName;
  String contactEmail;
  String userName;

  // sometimes the CSVReader retains double quotes so we remove them manually
  protected String clean(String input) {
    return input.trim().replace("\"", "");
  }

  // check data prerequisites which don't involve DB comparisons
  abstract void preCheck();

  abstract DbVerifiedInstitutionalAffiliation toAffiliation(
      final DbUser dbUser, final InstitutionDao institutionDao);

  static List<String[]> readFile(final String filename, final int columnLength) throws IOException {
    try (final CSVReader reader = new CSVReader(new FileReader(filename))) {
      // consume and sanity-check header line
      final String[] headerLine = reader.readNext();
      if (headerLine.length != columnLength) {
        throw new RuntimeException(
            String.format(
                "Expected %d columns in input file. Was: %d", columnLength, headerLine.length));
      }

      return StreamSupport.stream(reader.spliterator(), false).collect(Collectors.toList());
    }
  }

  private void checkField(String dbValue, String csvValue, String fieldName) {
    if (!dbValue.equals(csvValue)) {
      LOGGER.warning(
          String.format(
              "CSV and DB values do not match for user '%s', field '%s'. CSV = %s, DB = %s",
              userName, fieldName, csvValue, dbValue));
    }
  }

  private DbUser dbCheck(final UserDao userDao) {
    final DbUser dbUser = userDao.findUserByUsername(userName);

    // fatal errors: something is quite wrong and we need to recheck our assumptions!

    if (dbUser == null) {
      throw new RuntimeException(String.format("User %s was not found in the DB", userName));
    }

    // many near-matches in the imput exist: warn only, don't stop
    // example: "Dan" instead of "Daniel"
    // example: ""Beth,"" instead of "Beth"

    checkField(dbUser.getGivenName(), firstName, "First Name");
    checkField(dbUser.getFamilyName(), lastName, "Last Name");
    checkField(dbUser.getContactEmail(), contactEmail, "Contact Email");

    return dbUser;
  }

  void populateAffiliation(
      final boolean dryRun,
      final UserDao userDao,
      final InstitutionDao institutionDao,
      final VerifiedInstitutionalAffiliationDao affiliationDao) {

    preCheck();

    final DbUser dbUser = dbCheck(userDao);
    final DbVerifiedInstitutionalAffiliation newAffiliation = toAffiliation(dbUser, institutionDao);
    final Optional<DbVerifiedInstitutionalAffiliation> existingAffil =
        affiliationDao.findFirstByUser(dbUser);

    if (!existingAffil.isPresent()) {
      if (!dryRun) {
        affiliationDao.save(newAffiliation);
      }

      dryLog(
          dryRun,
          String.format(
              "Saved Affiliation for '%s' with Institution '%s'",
              newAffiliation.getUser().getUsername(),
              newAffiliation.getInstitution().getDisplayName()));
    } else {
      // will always execute since we checked it above
      existingAffil.ifPresent(
          existingAffiliation -> {
            if (existingAffiliation.equals(newAffiliation)) {
              LOGGER.info("No action taken.  Affiliation exists: " + existingAffiliation);
            } else {
              throw new RuntimeException(
                  String.format(
                      "New affiliation differs from affiliation in DB for user '%s':\n"
                          + "New affiliation = %s\nDB affiliation= %s",
                      userName, newAffiliation, existingAffiliation));
            }
          });
    }
  }

  private static void dryLog(boolean dryRun, String msg) {
    String prefix = "";
    if (dryRun) {
      prefix = "[DRY RUN] Would have... ";
    }
    LOGGER.info(prefix + msg);
  }
}
