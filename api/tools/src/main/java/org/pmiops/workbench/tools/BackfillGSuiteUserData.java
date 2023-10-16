package org.pmiops.workbench.tools;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.google.DirectoryServiceImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DirectoryServiceImpl.class})
public class BackfillGSuiteUserData extends Tool {
  private static final Logger log = Logger.getLogger(BackfillGSuiteUserData.class.getName());

  private static final Option RW_PROJ_OPT =
      Option.builder()
          .longOpt("project")
          .desc("The AoU project (environment) to access.")
          .required()
          .hasArg()
          .build();

  private static final Option DRY_OPT =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .hasArg()
          .build();
  private static final Options OPTIONS = new Options().addOption(RW_PROJ_OPT).addOption(DRY_OPT);

  @Bean
  public IamCredentialsClient iamCredentialsClient() throws IOException {
    return IamCredentialsClient.create();
  }

  @Bean
  public CommandLineRunner run(UserDao userDao, DirectoryServiceImpl directoryService) {
    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(OPTIONS, args);
      final String rwEnvOpt = opts.getOptionValue(RW_PROJ_OPT.getLongOpt());
      String dryRun_arg = opts.getOptionValue(DRY_OPT.getLongOpt());
      boolean dry_run = false;
      if (dryRun_arg.isEmpty() || dryRun_arg.equals("true")) {
        dry_run = true;
      }
      log.info(String.format("Found %s project", rwEnvOpt));
      log.info(String.format("Dry run : %s", dry_run));
      try {
        int updateCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        // Using userDao.getUsers was very very slow on test, on PROD with ~13k it will be even more
        // slower hence get ids and iterate on each one by one
        List<Long> userIds = userDao.findUserIds();
        for (var userId : userIds) {
          DbUser user = userDao.findUserByUserId(userId);
          log.info("This is for user " + user.getUsername());
          var gSuiteUser = directoryService.getUser(user.getUsername());
          try {
            if (gSuiteUser.isEmpty()) {
              log.warning(
                  String.format(
                      "AoU user %s (%s) not found in GSuite! Skipping.",
                      user.getUsername(), user.getContactEmail()));
            }
            var origGSuiteUser = gSuiteUser.get().clone();
            directoryService.addCustomSchemaAndEmails(
                gSuiteUser.get(), user.getUsername(), user.getContactEmail());
            if (gSuiteUser.get().getCustomSchemas().equals(origGSuiteUser.getCustomSchemas())) {
              log.info("User " + user.getUsername() + " already has correct GSuite data");
              skipCount++;
              continue;
            }

            if (dry_run) {
              log.info(
                  String.format(
                      "DRY RUN: Would update user %s / %s.",
                      user.getUsername(), user.getContactEmail()));
              updateCount++;
            } else {
              try {
                directoryService.updateUser(gSuiteUser.get());
                updateCount++;
                log.info("Backfilled data for " + user.getUsername());
              } catch (Exception e) {
                log.severe("Error backfilling data for " + user.getUsername());
                log.severe(e.getMessage());
                errorCount++;
              }
            }
          } catch (Exception e) {
            log.warning(
                String.format(
                    "AoU user %s (%s) Gave the following exception %s.",
                    user.getUsername(), user.getContactEmail(), e.getMessage()));
          }
        }
        ;
        log.info("Count: " + userIds.size());
      } catch (Exception ex) {
        log.warning("something went wrong" + ex.getLocalizedMessage());
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(BackfillGSuiteUserData.class, args);
  }
}
