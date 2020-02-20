package org.pmiops.workbench.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
@Import({FireCloudServiceImpl.class, FireCloudConfig.class})
public class DeleteWorkspaces {

  private static final Logger log = Logger.getLogger(DeleteWorkspaces.class.getName());

  private static Option deleteListFilename =
      Option.builder()
          .longOpt("delete-list-filename")
          .desc(
              "File containing list of workspaces to delete. Each line should contain a single workspace's namespace and firecloud name, separated by a comma"
                  + "Example: ws-namespace-1,fc-id-1 \n ws-namespace-2,fc-id-2 \n ws-namespace-3, fc-id-3")
          .required()
          .hasArg()
          .build();

  private static Options options = new Options().addOption(deleteListFilename);

  @Bean
  public WorkspaceService workspaceService(
      FireCloudService fireCloudService,
      Clock clock,
      WorkspaceDao workspaceDao,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      Provider<DbUser> dbUserProvider) {
    return new WorkspaceServiceImpl(
        null,
        null,
        clock,
        null,
        null,
        null,
        fireCloudService,
        null,
        dbUserProvider,
        userRecentWorkspaceDao,
        null,
        workspaceDao,
        null,
        null);
  }

  static DbUser currentImpersonatedUser;

  @Bean
  @Primary
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  DbUser user() {
    return currentImpersonatedUser;
  }

  @Bean
  @Primary
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Qualifier(FireCloudConfig.END_USER_WORKSPACE_API)
  WorkspacesApi workspaceApi(FireCloudService fireCloudService) throws IOException {
    if (currentImpersonatedUser == null) {
      return null;
    }

    ApiClient apiClient =
        fireCloudService.getApiClientWithImpersonation(currentImpersonatedUser.getUsername());
    return new WorkspacesApi(apiClient);
  }

  @Bean
  @Primary
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ProfileApi profileApi(FireCloudService fireCloudService) throws IOException {
    if (currentImpersonatedUser == null) {
      return null;
    }

    ApiClient apiClient =
        fireCloudService.getApiClientWithImpersonation(currentImpersonatedUser.getUsername());
    return new ProfileApi(apiClient);
  }

  @Bean
  public CommandLineRunner run(
      WorkspaceDao workspaceDao, UserDao userDao, WorkspaceService workspaceService) {

    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      try (BufferedReader reader =
          new BufferedReader(
              new FileReader(opts.getOptionValue(deleteListFilename.getLongOpt())))) {
        reader
            .lines()
            .forEach(
                line -> {
                  String[] tokens = line.split(",");
                  String namespace = tokens[0].trim();
                  String fcName = tokens[1].trim();

                  DbWorkspace dbWorkspace =
                      workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
                          namespace,
                          fcName,
                          DbStorageEnums.workspaceActiveStatusToStorage(
                              WorkspaceActiveStatus.ACTIVE));

                  if (dbWorkspace == null) {
                    log.info(
                        "Could not find active workspace with (namespace, fcId) of ("
                            + namespace
                            + ", "
                            + fcName
                            + ")");
                    return;
                  }

                  currentImpersonatedUser = dbWorkspace.getCreator();
                  try {
                    workspaceService.deleteWorkspace(dbWorkspace);
                  } catch (Exception e) {
                    log.log(
                        Level.WARNING,
                        "Could not delete workspace (" + namespace + ", " + fcName + ")",
                        e);
                  }

                  log.info(
                      "Deleted workspace ("
                          + dbWorkspace.getWorkspaceNamespace()
                          + ", "
                          + dbWorkspace.getFirecloudName()
                          + ")");
                });
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(DeleteWorkspaces.class, args);
  }
}
