package org.pmiops.workbench.tools;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.google.DirectoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * See api/project.rb backfill-gsuite-fields for usage.
 *
 * This command-line tool handles RW-
 */
@ComponentScan
@EnableAutoConfiguration
@EntityScan("org.pmiops.workbench.db.model")
public class BackfillGSuiteUserData {
  private static final Logger log = Logger.getLogger(BackfillGSuiteUserData.class.getName());

  @Bean
  public CommandLineRunner run(UserDao userDao, WorkbenchConfig workbenchConfig, DirectoryService directoryService) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException(
          "Expected 1 arg (dry_run). Got "
            + Arrays.asList(args));
      }
      boolean dryRun = Boolean.valueOf(args[0]);

      int userCount = 0;

      for (User user : userDao.findAll()) {
        userCount++;
      }

      log.info(String.format("Found {0} users.", userCount));
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(org.pmiops.workbench.tools.BackfillGSuiteUserData.class).web(false).run(args);
  }
}
