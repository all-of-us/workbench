package org.pmiops.workbench.tools;

import java.sql.Timestamp;
import java.time.Instant;
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

  private static final Logger log = Logger.getLogger(SetAccessModuleTimestamps.class.getName());

  // very crude POC implementation - only handles ProfileConfirmation and completion (not bypass)
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
      final String logMsg =
          String.format(
              "Updating %s %s time for user %s to %s",
              moduleName, isBypass ? "bypass" : "completion", username, timestamp.toString());
      log.info(logMsg);

      // dual-write to DbUser and AccessModuleService
      // we will remove the module fields in DbUser soon

      dbUser.setProfileLastConfirmedTime(timestamp);
      dbUser = userDao.save(dbUser);
      accessModuleService.updateCompletionTime(dbUser, moduleName, timestamp);
    } else {
      throw new IllegalArgumentException("Not implemented!");
    }
  }

  private static final Option userOpt =
      Option.builder()
          .longOpt("user")
          .desc("Username (email) of the user to modify")
          .required()
          .hasArg()
          .build();

  private static final Options options = new Options().addOption(userOpt);

  @Bean
  public CommandLineRunner run(AccessModuleService accessModuleService, UserDao userDao) {
    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(options, args);
      final String username = opts.getOptionValue(userOpt.getLongOpt());

      applyTimestampUpdateToUser(
          userDao,
          accessModuleService,
          username,
          AccessModuleName.PROFILE_CONFIRMATION,
          Timestamp.from(Instant.parse("2020-07-20T00:00:00.00Z")),
          false);
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(SetAccessModuleTimestamps.class, args);
  }
}
