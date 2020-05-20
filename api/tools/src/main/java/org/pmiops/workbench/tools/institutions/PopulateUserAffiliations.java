package org.pmiops.workbench.tools.institutions;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.tools.CommandLineToolConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

/**
 * Populate the verified_institutional_affiliation table for users listed in a CSV input file.
 *
 * <p>NOTE: input file must be located in the current directory or a subdirectory.
 *
 * <p>Example execution:
 *
 * <pre>
 * ./project.rb populate-user-affiliations \
 * --import-filename users.csv \
 * --user-type OPS \
 * --dry-run \
 * --project all-of-us-workbench-test
 * </pre>
 */
public class PopulateUserAffiliations {

  private static final Logger log = Logger.getLogger(PopulateUserAffiliations.class.getName());

  private List<User> parseUsers(final String filename, final String userTypes) throws IOException {
    if (userTypes.equals("OPS")) {
      return OpsUser.parseInput(filename);
    } else if (userTypes.equals("RESEARCHERS")) {
      return Researcher.parseInput(filename);
    } else {
      throw new RuntimeException(
              "Cannot populate affiliations: only valid user types are 'OPS' and 'RESEARCHERS'");
    }
  }

  @Bean
  public CommandLineRunner run(
      final UserDao userDao,
      final InstitutionDao institutionDao,
      final VerifiedInstitutionalAffiliationDao affiliationDao) {

    final Option importFilename =
        Option.builder()
            .longOpt("import-filename")
            .desc("File containing CSV of users")
            .required()
            .hasArg()
            .build();
    final Option userType =
        Option.builder()
            .longOpt("user-type")
            .desc("OPS for ops users and RESEARCHERS for researchers")
            .required()
            .hasArg()
            .build();
    final Option dryRunOpt =
        Option.builder()
            .longOpt("dry-run")
            .desc("If specified, the tool runs in dry run mode; no modifications are made")
            .build();
    final Options options =
        new Options().addOption(importFilename).addOption(userType).addOption(dryRunOpt);

    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(options, args);
      final boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());
      final String filename = opts.getOptionValue(importFilename.getLongOpt());
      final String userTypes = opts.getOptionValue(userType.getLongOpt());

      // process whole file before taking any action
      final List<User> users = parseUsers(filename, userTypes);

      for (final User user : users) {
        user.populateAffiliation(dryRun, userDao, institutionDao, affiliationDao);
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(PopulateUserAffiliations.class, args);
  }
}
