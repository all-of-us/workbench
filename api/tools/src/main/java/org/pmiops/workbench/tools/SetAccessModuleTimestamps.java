package org.pmiops.workbench.tools;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessModule;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SetAccessModuleTimestamps {

  private static final Logger log = Logger.getLogger(SetAccessModuleTimestamps.class.getName());

  // very crude POC implementation - only handles ProfileConfirmation, completion (not bypass),
  // and old-style (DbUser fields) modules
  void applyTimestampUpdateToUser(
      UserDao userDao,
      String username,
      AccessModule module,
      @Nullable Timestamp timestamp,
      boolean isBypass) {
    final DbUser dbUser = userDao.findUserByUsername(username);
    if (dbUser == null) {
      throw new IllegalArgumentException(String.format("User %s not found!", username));
    }

    if (module == AccessModule.PROFILE_CONFIRMATION && !isBypass) {
      final String logMsg =
          String.format(
              "Updating %s %s time for user %s to %s",
              module, isBypass ? "bypass" : "completion", username, timestamp.toString());
      log.info(logMsg);
      dbUser.setProfileLastConfirmedTime(timestamp);
      userDao.save(dbUser);
    } else {
      throw new IllegalArgumentException("Not implemented!");
    }
  }

  @Bean
  public CommandLineRunner run(UserDao userDao) {
    return (args) -> {
      // the only change needed for the POC test
      applyTimestampUpdateToUser(
          userDao,
          "joel@fake-research-aou.org",
          AccessModule.PROFILE_CONFIRMATION,
          Timestamp.from(Instant.parse("2020-07-20T00:00:00.00Z")),
          false);
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(SetAccessModuleTimestamps.class, args);
  }
}
