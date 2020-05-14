package org.pmiops.workbench.tools.institutions;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import org.pmiops.workbench.tools.CommandLineToolConfig;
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
                () ->
                    new RuntimeException(
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

