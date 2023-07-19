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
import org.pmiops.workbench.access.AccessModuleNameMapperImpl;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.ActionAuditServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditorImpl;
import org.pmiops.workbench.audit.ActionAuditSpringConfiguration;
import org.pmiops.workbench.config.CacheAccessModuleConfiguration;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  AccessModuleNameMapperImpl.class,
  AccessModuleServiceImpl.class,
  ActionAuditServiceImpl.class,
  ActionAuditSpringConfiguration.class, // injects com.google.cloud.logging.Logging
  CacheAccessModuleConfiguration.class, // injects List<DbAccessModule>
  CommonMappers.class,
  UserAccessModuleMapperImpl.class,
  UserServiceAuditorImpl.class,
})
public class SetAccessModuleTimestamps extends Tool {

  private static final Logger LOG = Logger.getLogger(SetAccessModuleTimestamps.class.getName());

  private static final Option PROFILE_USER_OPT =
      Option.builder()
          .longOpt("profile-user")
          .desc("Username (email) of the user to modify for the Profile module")
          .required()
          .hasArg()
          .build();

  private static final Option RAS_USER_OPT =
      Option.builder()
          .longOpt("ras-user")
          .desc("Username (email) of the user to modify for the RAS module")
          .required()
          .hasArg()
          .build();

  private static final Options OPTIONS =
      new Options().addOption(PROFILE_USER_OPT).addOption(RAS_USER_OPT);

  private static final Timestamp PROFILE_CONFIRMATION_TIMESTAMP =
      Timestamp.from(Instant.parse("2011-08-01T00:00:00.00Z"));

  private static DbUser dbProfileUser;
  private static DbUser dbRasUser;

  void updateCompletionTime(
      AccessModuleService accessModuleService,
      DbUser dbUser,
      DbAccessModuleName moduleName,
      @Nullable Timestamp timestamp) {
    accessModuleService.updateCompletionTime(dbUser, moduleName, timestamp);

    final String time = Optional.ofNullable(timestamp).map(Timestamp::toString).orElse("NULL");
    LOG.info(
        String.format(
            "Updating %s completion time for user %s to %s",
            moduleName, dbUser.getUsername(), time));
  }

  @Bean
  public CommandLineRunner run(AccessModuleService accessModuleService, UserDao userDao) {
    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(OPTIONS, args);
      final String profileUsername = opts.getOptionValue(PROFILE_USER_OPT.getLongOpt());
      final String rasUsername = opts.getOptionValue(RAS_USER_OPT.getLongOpt());
      dbProfileUser = userDao.findUserByUsername(profileUsername);
      if (dbProfileUser == null) {
        throw new IllegalArgumentException(
            String.format("Profile user %s not found!", profileUsername));
      }
      dbRasUser = userDao.findUserByUsername(rasUsername);
      if (dbRasUser == null) {
        throw new IllegalArgumentException(String.format("RAS user %s not found!", rasUsername));
      }

      updateCompletionTime(
          accessModuleService,
          dbProfileUser,
          DbAccessModuleName.PROFILE_CONFIRMATION,
          PROFILE_CONFIRMATION_TIMESTAMP);

      updateCompletionTime(accessModuleService, dbRasUser, DbAccessModuleName.IDENTITY, null);
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(SetAccessModuleTimestamps.class, args);
  }
}
