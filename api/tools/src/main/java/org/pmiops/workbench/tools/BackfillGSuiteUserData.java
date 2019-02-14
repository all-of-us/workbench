package org.pmiops.workbench.tools;

import com.google.common.cache.LoadingCache;
import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

/**
 * See api/project.rb backfill-gsuite-fields for usage.
 * <p>
 * This command-line tool updates some GSuite data for all known AoU users in the current
 * environment in two ways:
 */
@SpringBootApplication
// Only import the CacheSpringConfiguration bean -- we don't want the rest of the stuff from
// o.p.w.config which includes things specific to the web app.
@ComponentScan(
  basePackageClasses = CacheSpringConfiguration.class,
  useDefaultFilters = false,
  includeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = CacheSpringConfiguration.class)
  })
@EnableJpaRepositories({"org.pmiops.workbench.db.dao"})
@EntityScan("org.pmiops.workbench.db.model")
public class BackfillGSuiteUserData {

  private static final Logger log = Logger.getLogger(BackfillGSuiteUserData.class.getName());

  @Bean
  public CommandLineRunner run(
    UserDao userDao,
    @Qualifier("configCache") LoadingCache<String, Object> configCache) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException(
          "Expected 1 arg (dry_run). Got "
            + Arrays.asList(args));
      }
      boolean dryRun = Boolean.valueOf(args[0]);

      WorkbenchConfig config = CacheSpringConfiguration.lookupWorkbenchConfig(configCache);
      log.log(Level.INFO,"GSuite domain: " + config.googleDirectoryService.gSuiteDomain);

      int userCount = 0;
      for (User user : userDao.findUsers()) {
        userCount += 1;
      }
      log.log(Level.INFO, "Found " + userCount + " users");
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(BackfillGSuiteUserData.class).web(false)
      .run(args);
  }
}