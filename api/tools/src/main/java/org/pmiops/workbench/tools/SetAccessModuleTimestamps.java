package org.pmiops.workbench.tools;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.ActionAuditServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditorImpl;
import org.pmiops.workbench.audit.ActionAuditSpringConfiguration;
import org.pmiops.workbench.config.CacheAccessModuleConfiguration;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  AccessModuleServiceImpl.class,
  ActionAuditServiceImpl.class,
  ActionAuditSpringConfiguration.class, // injects com.google.cloud.logging.Logging
  CacheAccessModuleConfiguration.class, // injects List<DbAccessModule>
  CommonMappers.class,
  UserAccessModuleMapperImpl.class,
  UserServiceAuditorImpl.class,
})
public class SetAccessModuleTimestamps {

  private static final Logger LOG = Logger.getLogger(SetAccessModuleTimestamps.class.getName());

  private static final Option USER_OPT =
      Option.builder()
          .longOpt("user")
          .desc("Username (email) of the user to modify")
          .required()
          .hasArg()
          .build();

  private static final Options OPTIONS = new Options().addOption(USER_OPT);

  // very crude POC implementation - only handles ProfileConfirmation and completion (not bypass)
  private final AccessModuleName moduleName = AccessModuleName.PROFILE_CONFIRMATION;
  private final Timestamp timestamp = Timestamp.from(Instant.parse("2011-08-01T00:00:00.00Z"));

  void applyTimestampUpdateToUser(
      UserDao userDao,
      AccessModuleService accessModuleService,
      String username,
      AccessModuleName moduleName,
      @Nullable Timestamp timestamp,
      boolean isBypass) {
    DbUser dbUser = userDao.findUserByUsername(username);
    if (dbUser == null) {
      throw new IllegalArgumentException(String.format("User %s not found!", username));
    }

    if (moduleName == AccessModuleName.PROFILE_CONFIRMATION && !isBypass) {
      final String field = isBypass ? "bypass" : "completion";
      final String time = Optional.ofNullable(timestamp).map(Timestamp::toString).orElse("NULL");
      LOG.info(
          String.format(
              "Updating %s %s time for user %s to %s", moduleName, field, username, time));

      // dual-write to DbUser and AccessModuleService
      // we will remove the module fields in DbUser soon
      // see the Access Module Update epic
      // https://precisionmedicineinitiative.atlassian.net/browse/RW-6237

      dbUser.setProfileLastConfirmedTime(timestamp);
      dbUser = userDao.save(dbUser);
      accessModuleService.updateCompletionTime(dbUser, moduleName, timestamp);
    } else {
      throw new IllegalArgumentException("Not implemented!");
    }
  }

  @Bean
  public CommandLineRunner run(AccessModuleService accessModuleService, UserDao userDao) {
    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(OPTIONS, args);
      final String username = opts.getOptionValue(USER_OPT.getLongOpt());

      applyTimestampUpdateToUser(
          userDao, accessModuleService, username, moduleName, timestamp, false);
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(SetAccessModuleTimestamps.class, args);
  }
}
