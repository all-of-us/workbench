package org.pmiops.workbench.tools;

import com.google.common.collect.Streams;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.monitoring.MonitoringServiceImpl;
import org.pmiops.workbench.monitoring.MonitoringSpringConfiguration;
import org.pmiops.workbench.monitoring.StackdriverStatsExporterService;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/** A tool that will generate a CSV export of our workspace data */
@Configuration
@Import({
    FireCloudServiceImpl.class,
    FireCloudConfig.class
})
public class DeleteWorkspaces {

  private static final Logger log = Logger.getLogger(DeleteWorkspaces.class.getName());

  private static Option deleteListFilename =
      Option.builder()
          .longOpt("delete-list-filename")
          .desc("File containing list of workspaces to delete. Each line should contain a single workspace's namespace and firecloud name, separated by a comma"
              + "Example: ws-namespace-1,fc-id-1 \n ws-namespace-2,fc-id-2 \n ws-namespace-3, fc-id-3")
          .required()
          .hasArg()
          .build();

  private static Options options = new Options().addOption(deleteListFilename);

  @Bean
  public WorkspaceService workspaceService(FireCloudService fireCloudService,
      Clock clock,
      WorkspaceDao workspaceDao,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      Provider<DbUser> dbUserProvider) {
    return new WorkspaceServiceImpl(
        null, null, clock, null, null, null, fireCloudService, null, dbUserProvider, userRecentWorkspaceDao, null, workspaceDao, null, null);
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

    ApiClient apiClient = fireCloudService.getApiClientWithImpersonation(currentImpersonatedUser.getUsername());
    return new WorkspacesApi(apiClient);
  }

  @Bean
  @Primary
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  ProfileApi profileApi(FireCloudService fireCloudService) throws IOException {
    if (currentImpersonatedUser == null) {
      return null;
    }

    ApiClient apiClient = fireCloudService.getApiClientWithImpersonation(currentImpersonatedUser.getUsername());
    return new ProfileApi(apiClient);
  }


  private WorkspaceDao workspaceDao;
  private UserDao userDao;

  @Bean
  public CommandLineRunner run(
      WorkspaceDao workspaceDao,
      UserDao userDao,
      WorkspaceService workspaceService) {
    this.workspaceDao = workspaceDao;
    this.userDao = userDao;

    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      try (BufferedReader reader = new BufferedReader(new FileReader(opts.getOptionValue(deleteListFilename.getLongOpt())))) {
        reader.lines().forEach(line -> {
          String[] tokens = line.split(",");
          String namespace = tokens[0].trim();
          String fcName = tokens[1].trim();

          DbWorkspace dbWorkspace = workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(namespace, fcName,
              DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));

          if (dbWorkspace == null) {
            log.info("Could not find active workspace with (namespace, fcId) of (" + namespace + ", " + fcName + ")");
            return;
          }

          currentImpersonatedUser = dbWorkspace.getCreator();
          workspaceService.deleteWorkspace(dbWorkspace);
          log.info("Deleted workspace (" + dbWorkspace.getWorkspaceNamespace() + ", " + dbWorkspace.getFirecloudName() + ")");
        });
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(DeleteWorkspaces.class, args);
  }
}

