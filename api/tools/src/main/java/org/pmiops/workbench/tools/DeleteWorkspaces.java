package org.pmiops.workbench.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
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
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();

  private static Options options = new Options().addOption(deleteListFilename).addOption(dryRunOpt);

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
  WorkspacesApi workspaceApi(WorkbenchConfig config) {
    if (currentImpersonatedUser == null) {
      return null;
    }
    return new WorkspacesApi(buildFirecloudServiceAccountApiClient(config));
  }

  @Bean
  @Primary
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ProfileApi profileApi(WorkbenchConfig config) {
    if (currentImpersonatedUser == null) {
      return null;
    }
    return new ProfileApi(buildFirecloudServiceAccountApiClient(config));
  }

  private static ApiClient buildFirecloudServiceAccountApiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = FireCloudConfig.buildApiClient(workbenchConfig);
    try {
      apiClient.setAccessToken(
          ServiceAccounts.getScopedServiceAccessToken(FireCloudConfig.BILLING_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean
  public CommandLineRunner run(
      WorkspaceDao workspaceDao, UserDao userDao, WorkspaceService workspaceService) {

    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());

      AtomicInteger successes = new AtomicInteger();
      AtomicInteger fails = new AtomicInteger();
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

                  try {
                    currentImpersonatedUser = dbWorkspace.getCreator();
                    if (!dryRun) {
                      workspaceService.deleteWorkspace(dbWorkspace);
                    }

                    successes.getAndIncrement();
                    dryLog(
                        dryRun,
                        "Deleted workspace ("
                            + dbWorkspace.getWorkspaceNamespace()
                            + ", "
                            + dbWorkspace.getFirecloudName()
                            + ")");
                  } catch (Exception e) {
                    fails.getAndIncrement();
                    log.log(
                        Level.WARNING,
                        "Could not delete workspace (" + namespace + ", " + fcName + ")",
                        e);
                  }
                });
      }
      dryLog(
          dryRun,
          String.format(
              "Deleted %d workspaces, failed to delete %d", successes.get(), fails.get()));
    };
  }

  private static void dryLog(boolean dryRun, String msg) {
    String prefix = "";
    if (dryRun) {
      prefix = "[DRY RUN] Would have... ";
    }
    log.info(prefix + msg);
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(DeleteWorkspaces.class, args);
  }
}
