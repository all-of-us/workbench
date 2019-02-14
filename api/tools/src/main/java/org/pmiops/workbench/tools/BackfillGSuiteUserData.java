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
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * See api/project.rb backfill-gsuite-fields for usage.
 * <p>
 * This command-line tool updates some GSuite data for all known AoU users in the current
 * environment in two ways:
 */
@SpringBootApplication
// UNRESOLVED ISSUE: I'm having trouble getting DI to work correctly overall. The existing tools use
// the JPA repositories plus entity scan annotations, which seem to work for injecting focused DAO
// classes. But I need to load the Workbench config, so I'm trying to broaden the scan to all
// workbench classes. This ends up giving me errors like
// https://gist.github.com/gjuggler/099a821119309d2d378ee0161ca064cc.
@ComponentScan({"org.pmiops.workbench"})
@EnableJpaRepositories({"org.pmiops.workbench"})
@EntityScan("org.pmiops.workbench")
// We use the Profile annotation to ensure each command-line runner is only instantiated when it
// will be run. Each Gradle task should set the appropriate profile for its associated tool.
@Profile("BackfillGSuiteUserData")
public class BackfillGSuiteUserData {
  private static final Logger log = Logger.getLogger(BackfillGSuiteUserData.class.getName());

  @Autowired
  @Bean
  public CommandLineRunner run(
    UserDao userDao,
    // UNRESOLVED ISSUE: I'm basically trying to find a way to get a WorkbenchConfig instance,
    // so I can read the gSuiteDomain property to know which GSuite domain to connect to for the
    // script.
    //
    // AFAICT, the way for us to load a config is using the CacheSpringConfiguration class, and to
    // pass in a LoadingCache instance. I'm trying to let Spring use DI to inject this, following
    // the example from common-api > CdrDbConfig.java. It never seemed to work though, and I'm not
    // sure how to debug what's going wrong when a bean can't be found.
    @Qualifier("configCache") LoadingCache<String, Object> configCache) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException(
          "Expected 1 arg (dry_run). Got "
            + Arrays.asList(args));
      }
      boolean dryRun = Boolean.valueOf(args[0]);

      int userCount = 0;

      WorkbenchConfig config = CacheSpringConfiguration.lookupWorkbenchConfig(configCache);
      System.out.println("GSuite domain: " + config.googleDirectoryService.gSuiteDomain);

      // UNRESOLVED ISSUE: Even ignoring the above two issues, when I was able to get this line
      // working I never saw more than zero users when connecting to all-of-us-workbench-test. Is
      // there something in my debstart.rb changes or Gradle config that might be messing up?
      for (User user : userDao.findAll()) {
        userCount++;
      }

      log.info(String.format("Found {0} users.", userCount));

      User user = userDao.findUserByEmail("gregory.jordan@fake-research-aou.org");
      if (user == null) {
        log.info("No user found!");
      }
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(BackfillGSuiteUserData.class).web(false).bannerMode(Banner.Mode.OFF)
      .run(args);
  }
}