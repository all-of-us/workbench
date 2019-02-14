package org.pmiops.workbench.tools;

import com.google.common.cache.LoadingCache;

import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * See api/project.rb backfill-gsuite-fields for usage.
 * <p>
 * This command-line tool updates some GSuite data for all known AoU users in the current
 * environment in two ways:
 */
@SpringBootApplication
@ComponentScan("org.pmiops.workbench.config")
public class BackfillGSuiteUserData implements CommandLineRunner {
  private static final Logger log = Logger.getLogger(BackfillGSuiteUserData.class.getName());

  @Autowired
  private UserDao userDao;

  @Autowired
  @Qualifier("configCache")
  private LoadingCache<String, Object> configCache;

  @Override
  public void run(String... args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException(
        "Expected 1 arg (dry_run). Got "
          + Arrays.asList(args));
    }
    boolean dryRun = Boolean.valueOf(args[0]);

    int userCount = 0;

    WorkbenchConfig config = CacheSpringConfiguration.lookupWorkbenchConfig(configCache);
    System.out.println("GSuite domain: " + config.googleDirectoryService.gSuiteDomain);

    for (User user : userDao.findAll()) {
      userCount++;
    }

    log.info(String.format("Found {0} users.", userCount));
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(org.pmiops.workbench.tools.BackfillGSuiteUserData.class)
      .web(false)
      .bannerMode(Banner.Mode.OFF)
      .run(args);
  }
}
