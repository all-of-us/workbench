package org.pmiops.workbench.tools;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.InstitutionalRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

/**
 * Populate the verified_institutional_affiliation table for Ops users from a CSV input file.
 *
 * <p>NOTE: input file must be located in the current directory or a subdirectory.
 *
 * <p>Example execution:
 *
 * <pre>
 * ./project.rb populate-ops-user-affiliations \
 * --import-filename users.csv \
 * --dry-run \
 * --project all-of-us-workbench-test
 * </pre>
 */
public class PopulateOpsUserAffiliations {

  private static final Logger log = Logger.getLogger(PopulateOpsUserAffiliations.class.getName());

  private static final String institutionShortName = "AouOps";
  private static final String actionForInclusion = "To Remain";

  private List<DbVerifiedInstitutionalAffiliation> prepareAffiliations(
      final String filename,
      UserDao userDao,
      InstitutionDao institutionDao,
      VerifiedInstitutionalAffiliationDao affiliationDao)
      throws IOException {

    final DbInstitution aouOps =
        institutionDao
            .findOneByShortName(institutionShortName)
            .orElseThrow(
                () -> new RuntimeException(
                        String.format(
                                "Could not find '%s' Institution in the DB", institutionShortName)));

    return OpsUser.parseInput(filename).stream()
        .map(
            opsUser -> {
              if (!opsUser.action.equals(actionForInclusion)) {
                throw new RuntimeException(
                    String.format(
                        "User %s not marked as '%s' in the input CSV",
                            opsUser.userName, actionForInclusion));
              }

              final DbUser dbUser = opsUser.dbCheck(userDao, affiliationDao);

              return new DbVerifiedInstitutionalAffiliation()
                  .setInstitution(aouOps)
                  .setUser(dbUser)
                  .setInstitutionalRoleEnum(InstitutionalRole.OTHER)
                  .setInstitutionalRoleOtherText(opsUser.operationalRole);
            })
        .collect(Collectors.toList());
  }

  @Bean
  public CommandLineRunner run(
      UserDao userDao,
      InstitutionDao institutionDao,
      VerifiedInstitutionalAffiliationDao affiliationDao) {

    final Option importFilename =
        Option.builder()
            .longOpt("import-filename")
            .desc("File containing CSV of ops users")
            .required()
            .hasArg()
            .build();
    final Option dryRunOpt =
        Option.builder()
            .longOpt("dry-run")
            .desc("If specified, the tool runs in dry run mode; no modifications are made")
            .build();
    final Options options = new Options().addOption(importFilename).addOption(dryRunOpt);

    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());

      // process whole file before taking any action
      final List<DbVerifiedInstitutionalAffiliation> affiliations =
          prepareAffiliations(
              opts.getOptionValue(importFilename.getLongOpt()),
              userDao,
              institutionDao,
              affiliationDao);

      affiliations.forEach(
          affiliation -> {
            if (!dryRun) {
              affiliationDao.save(affiliation);
            }

            dryLog(
                dryRun,
                String.format(
                    "Saved AouOps Institutional Affiliation for %s",
                    affiliation.getUser().getUsername()));
          });
    };
  }

  private static void dryLog(boolean dryRun, String msg) {
    String prefix = "";
    if (dryRun) {
      prefix = "[DRY RUN] Would have... ";
    }
    log.info(prefix + msg);
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(PopulateOpsUserAffiliations.class, args);
  }
}

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

    if (dbUser == null) {
      throw new RuntimeException(String.format("User %s was not found in the DB", userName));
    }

    checkField(dbUser.getGivenName(), firstName, "First Name");
    checkField(dbUser.getFamilyName(), lastName, "Last Name");
    checkField(dbUser.getContactEmail(), contactEmail, "Contact Email");

    affiliationDao
        .findFirstByUser(dbUser)
        .ifPresent(
            affiliation -> {
              throw new RuntimeException(
                  String.format(
                      "User %s is already affiliated with institution '%s'",
                      userName, affiliation.getInstitution().getDisplayName()));
            });

    return dbUser;
  }
}
