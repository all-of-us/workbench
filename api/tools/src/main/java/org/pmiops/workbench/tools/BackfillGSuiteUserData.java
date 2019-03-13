package org.pmiops.workbench.tools;

import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.google.DirectoryServiceImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * See api/project.rb backfill-gsuite-fields for usage.
 * <p>
 * This command-line tool updates some GSuite data for all existing AoU users to match the data
 * we now populate for new users.
 *
 * Once the backfill is completed for all environments, this script may be deleted.
 *
 * TODO(RW-2032): Before this script goes away, but after recoveryEmail becomes available
 * to the directory API, we should augment this script & run a backfill for recovery emails.
 */
@SpringBootApplication
// Load the DBA and DB model classes required for UserDao.
@EnableJpaRepositories({"org.pmiops.workbench.db.dao"})
@EntityScan("org.pmiops.workbench.db.model")
public class BackfillGSuiteUserData {
  private static final Logger log = Logger.getLogger(BackfillGSuiteUserData.class.getName());


  @Bean
  public CommandLineRunner run(
    UserDao userDao,
    DirectoryService directoryService) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException(
          "Expected 1 arg (dry_run). Got "
            + Arrays.asList(args));
      }
      boolean dryRun = Boolean.valueOf(args[0]);

      int updateCount = 0;
      int skipCount = 0;
      int errorCount = 0;
      for (User user : userDao.findAll()) {
        com.google.api.services.admin.directory.model.User gSuiteUser =
          directoryService.getUser(user.getEmail());
        if (gSuiteUser == null) {
          log.warning(String.format("AoU user %s (%s) not found in GSuite! Skipping.",
            user.getEmail(), user.getContactEmail()));
          skipCount++;
          continue;
        }

        com.google.api.services.admin.directory.model.User origGSuiteUser = gSuiteUser.clone();
        DirectoryServiceImpl.addCustomSchemaAndEmails(
          gSuiteUser, user.getEmail(), user.getContactEmail());
        if (gSuiteUser.getCustomSchemas().equals(origGSuiteUser.getCustomSchemas())) {
          log.info("User " + user.getEmail() + " already has correct GSuite data");
          skipCount++;
          continue;
        }

        if (dryRun) {
          log.info(
            String.format(
              "DRY RUN: Would update user %s / %s.", user.getEmail(), user.getContactEmail()));
          updateCount++;
        } else {
          try {
            directoryService.updateUser(gSuiteUser);
            updateCount++;
            log.info("Backfilled data for " + user.getEmail());
          } catch (Exception e) {
            log.severe("Error backfilling data for " + user.getEmail());
            log.severe(e.getMessage());
            errorCount++;
          }
        }
      }

      log.info(String.format("%s Backfill complete. Updated %d, skipped %d, failed %d.",
        dryRun ? "DRY RUN: " : "", updateCount, skipCount, errorCount));
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(BackfillGSuiteUserData.class).web(false)
      .run(args);
  }
}
