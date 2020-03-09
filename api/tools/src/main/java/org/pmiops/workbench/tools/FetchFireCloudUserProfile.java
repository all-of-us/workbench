package org.pmiops.workbench.tools;

import java.util.Arrays;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * A tool that takes an AoU username (e.g. gjordan) and fetches the FireCloud profile associated
 * with that AoU user.
 *
 * <p>This is intended mostly for demonstration / testing purposes, to show how we leverage
 * domain-wide delegation to make FireCloud API calls impersonating other users.
 */
@Configuration
@Import({FireCloudServiceImpl.class, FireCloudConfig.class})
public class FetchFireCloudUserProfile {
  private static final Logger log =
      Logger.getLogger(org.pmiops.workbench.tools.FetchFireCloudUserProfile.class.getName());

  @Bean
  public CommandLineRunner run(UserDao userDao, FireCloudService fireCloudService) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 1 arg (username). Got " + Arrays.asList(args));
      }

      String userEmail = args[0];
      DbUser user = userDao.findUserByUsername(userEmail);
      if (user == null) {
        throw new RuntimeException(
            String.format("Error fetching AoU user with email %s", userEmail));
      }
      log.info("Fetching data for " + userEmail);

      ApiClient apiClient = fireCloudService.getApiClientWithImpersonation(userEmail);

      ProfileApi profileApi = new ProfileApi(apiClient);
      FirecloudMe me = profileApi.me();
      log.info(
          String.format(
              "Email: %s, subject ID: %s",
              me.getUserInfo().getUserEmail(), me.getUserInfo().getUserSubjectId()));

      NihApi nihApi = new NihApi(apiClient);
      FirecloudNihStatus nihStatus = nihApi.nihStatus();
      log.info(String.format("NIH linked user: %s", nihStatus.getLinkedNihUsername()));
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(FetchFireCloudUserProfile.class, args);
  }
}
