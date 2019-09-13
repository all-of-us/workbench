package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.appengine.repackaged.com.google.common.base.Pair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.cdr.CdrDbConfig;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.compliance.ComplianceServiceImpl;
import org.pmiops.workbench.config.BigQueryConfig;
import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.WorkbenchDbConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspacesController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan({"org.pmiops.workbench.db.model"})
@Import({
    CdrDbConfig.class,
    BigQueryConfig.class,
    CacheSpringConfiguration.class,
    WorkbenchDbConfig.class,
    CohortCloningService.class,
    CohortFactoryImpl.class,
    ConceptService.class,
    ConceptBigQueryService.class,
    BigQueryService.class,
    CdrBigQuerySchemaConfigService.class,
    UserService.class,
    ComplianceServiceImpl.class,
    WorkspacesController.class,
    BillingProjectBufferService.class,
    NotebooksServiceImpl.class,
    UserRecentResourceServiceImpl.class
})
public class BulkCloneWorkspacesTool {

  private ProfileApi profileApi;
  private NihApi nihApi;
  private StaticNotebooksApi staticNotebooksApi;
  private WorkspacesApi workspacesApi;

  private User providedUser;

  @Bean
  @Scope("prototype")
  ProfileApi profileApi() {
    return profileApi;
  }

  @Bean
  @Scope("prototype")
  NihApi nihApi() {
    return nihApi;
  }

  @Bean
  @Scope("prototype")
  StaticNotebooksApi staticNotebooksApi() {
    return staticNotebooksApi;
  }

  @Bean
  @Primary
  @Qualifier("workspacesApi")
  @Scope("prototype")
  WorkspacesApi workspacesApi() {
    return workspacesApi;
  }

  @Bean
  @Primary
  @Qualifier("workspaceAclsApi")
  WorkspacesApi workspaceAclsApi(@Qualifier("saApiClient") ApiClient apiClient) {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Primary
  BillingApi billingApi(@Qualifier("saApiClient") ApiClient apiClient) {
    BillingApi api = new BillingApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Primary
  GroupsApi groupsApi(@Qualifier("saApiClient") ApiClient apiClient) {
    GroupsApi api = new GroupsApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Scope("prototype")
  User user() {
    return providedUser;
  }

  @Bean
  @Qualifier("saApiClient")
  ApiClient saApiClient(Provider<WorkbenchConfig> configProvider) throws IOException {
    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(BILLING_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    return apiClient;
  }

  private static final String[] BILLING_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/cloud-billing"
      };

  private void initializeApis() {
    profileApi = new ProfileApi();
    nihApi = new NihApi();
    staticNotebooksApi = new StaticNotebooksApi();
    workspacesApi = new WorkspacesApi();
  }

  private void impersonateUser(ApiClient apiClient) {
    profileApi.setApiClient(apiClient);
    nihApi.setApiClient(apiClient);
    staticNotebooksApi.setApiClient(apiClient);
    workspacesApi.setApiClient(apiClient);
  }

  private CloneWorkspaceRequest createCloneRequest(
      org.pmiops.workbench.model.WorkspaceResponse fromWorkspace) {
    org.pmiops.workbench.model.Workspace toWorkspace = new org.pmiops.workbench.model.Workspace();
    toWorkspace.setNamespace(fromWorkspace.getWorkspace().getNamespace());
    toWorkspace.setName(fromWorkspace.getWorkspace().getName());
    toWorkspace.setResearchPurpose(fromWorkspace.getWorkspace().getResearchPurpose());
    toWorkspace.setCdrVersionId(fromWorkspace.getWorkspace().getCdrVersionId());

    CloneWorkspaceRequest request = new CloneWorkspaceRequest();
    request.setWorkspace(toWorkspace);
    request.setIncludeUserRoles(true);

    return request;
  }

  private boolean checkUserExistsInGoogle(DirectoryService directoryService, String email) {
    try {
      return directoryService.getUser(email) != null;
    } catch (Exception e) {
      System.out.println("Skipping " + email + " because user is deleted in either AoU or Google");
      return false;
    }
  }

  @Bean
  public CommandLineRunner run(
      CdrVersionDao cdrVersionDao,
      WorkspaceDao workspaceDao,
      WorkspacesController workspacesController,
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      UserDao userDao,
      BillingProjectBufferService billingProjectBufferService,
      DirectoryService directoryService,
      @Qualifier("workspaceAclsApi") WorkspacesApi saWorkspaceApi) {
    return (args) -> {
      padding();
      initializeApis();
      System.out.println("Apis initialized");

      int numToProcess = Integer.parseInt(args[0]);

      boolean dryRun = false;
      if (args.length > 1 && args[1].equals("true")) {
        dryRun = true;
      }

      final CdrVersion defaultCdr = cdrVersionDao.findByIsDefault(true);

      List<WorkspaceResponse> processed = new ArrayList<>();
      List<Pair<WorkspaceResponse, String>> failedWorkspaces = new ArrayList<>();

      for (WorkspaceResponse workspaceResponse : saWorkspaceApi.listWorkspaces()) {
        List<Workspace> dbWorkspaces =
            workspaceDao.findAllByFirecloudUuidIn(
                Collections.singleton(workspaceResponse.getWorkspace().getWorkspaceId()));

        // Workspace that exists in FC but not in AoU
        if (dbWorkspaces.isEmpty()) {
          continue;
        }

        Workspace dbWorkspace = dbWorkspaces.get(0);

        // Skip over inactive workspaces and workspaces that are already on the
        if (dbWorkspace.getWorkspaceActiveStatusEnum().equals(WorkspaceActiveStatus.DELETED)) {
          continue;
        }

        // Only process workspaces that need migration
        if (!dbWorkspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)) {
          continue;
        }

        if (workspaceResponse.getAccessLevel().equals("NO ACCESS")) {
          System.out.println(
              "Found a workspace that the SA account cannot access : "
                  + shorthand(workspaceResponse.getWorkspace()));
          failedWorkspaces.add(Pair.of(workspaceResponse, "NO ACCESS"));
          continue;
        }

        final double bufferThresholdPercentage = 25;
        final int sleepIntervalSeconds = 30;
        while (billingProjectBufferService.availableProportion()
            < bufferThresholdPercentage / 100) {
          System.out.println(
              "Less than "
                  + bufferThresholdPercentage
                  + "% of the buffer is available ("
                  + billingProjectBufferService.availableProportion() * 100
                  + "%)... Sleeping for "
                  + sleepIntervalSeconds
                  + " seconds");
          Thread.sleep(sleepIntervalSeconds * 1000);
        }

        providedUser = userDao.findUserByEmail(workspaceResponse.getWorkspace().getCreatedBy());
        if (providedUser == null
            || providedUser.getDisabled()
            || !checkUserExistsInGoogle(directoryService, providedUser.getEmail())) {
          continue;
        }

        impersonateUser(fireCloudService.getApiClientWithImpersonation(providedUser.getEmail()));
        System.out.println("Impersonated " + providedUser.getEmail());

        try {
          System.out.println("About to clone " + shorthand(dbWorkspace));

          org.pmiops.workbench.model.WorkspaceResponse apiWorkspace =
              workspaceService.getWorkspace(
                  dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
          CloneWorkspaceRequest request = createCloneRequest(apiWorkspace);
          request.getWorkspace().setCdrVersionId(String.valueOf(defaultCdr.getCdrVersionId()));

          if (!dryRun) {
            System.out.println("Sending clone request");

            CloneWorkspaceResponse cloneResponse =
                workspacesController
                    .cloneWorkspace(
                        dbWorkspace.getWorkspaceNamespace(),
                        dbWorkspace.getFirecloudName(),
                        request)
                    .getBody();
            System.out.println("Successful Clone into " + shorthand(cloneResponse.getWorkspace()));

            dbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.MIGRATED);
            workspaceDao.save(dbWorkspace);
          }

          processed.add(workspaceResponse);
        } catch (Exception e) {
          System.out.println("Failed on " + shorthand(dbWorkspace));
          System.out.println(workspaceResponse);
          e.printStackTrace();

          if (e instanceof NotFoundException) {
            System.out.println(providedUser.getDataAccessLevelEnum());
            failedWorkspaces.add(
                Pair.of(
                    workspaceResponse,
                    "Resource not found. User has Data Access Level of "
                        + providedUser.getDataAccessLevelEnum()));
          } else {
            failedWorkspaces.add(Pair.of(workspaceResponse, e.getMessage()));
          }
        }

        if (processed.size() + failedWorkspaces.size() == numToProcess) {
          break;
        }
      }

      padding();
      System.out.println("Processed Workspaces : " + processed.size());
      for (WorkspaceResponse workspaceResponse : processed) {
        System.out.println(shorthand(workspaceResponse.getWorkspace()));
      }
      System.out.println("Failed Workspaces : " + failedWorkspaces.size());
      for (Pair<WorkspaceResponse, String> failedWorkspace : failedWorkspaces) {
        System.out.println(shorthand(failedWorkspace.first.getWorkspace()));
        System.out.println(failedWorkspace.second);
      }
      padding();
    };
  }

  private String shorthand(Workspace workspace) {
    return "("
        + workspace.getCreator().getEmail()
        + " : "
        + workspace.getWorkspaceNamespace()
        + " : "
        + workspace.getFirecloudName()
        + ")";
  }

  private String shorthand(org.pmiops.workbench.model.Workspace workspace) {
    return "("
        + workspace.getCreator()
        + " : "
        + workspace.getNamespace()
        + " : "
        + workspace.getId()
        + ")";
  }

  private String shorthand(org.pmiops.workbench.firecloud.model.Workspace workspace) {
    return "("
        + workspace.getCreatedBy()
        + " : "
        + workspace.getNamespace()
        + " : "
        + workspace.getName()
        + ")";
  }

  private void padding() {
    for (int i = 0; i < 15; i++) {
      System.out.println("***");
    }
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(BulkCloneWorkspacesTool.class).web(false).run(args);
  }
}
