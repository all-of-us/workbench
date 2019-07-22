package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspacesController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.ResponseEntity;

@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan({"org.pmiops.workbench.db.model"})
public class BulkCloneWorkspacesTool {

  private ProfileApi profileApi;
  private NihApi nihApi;
  private WorkspacesApi workspacesApi;
  private StaticNotebooksApi staticNotebooksApi;

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
  @Primary
  @Qualifier("workspacesApi")
  @Scope("prototype")
  WorkspacesApi workspacesApi(Provider<WorkbenchConfig> configProvider) throws IOException {
//    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
//    GoogleCredential credential =
//        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(BILLING_SCOPES));
//    credential.refreshToken();
//    apiClient.setAccessToken(credential.getAccessToken());
//    WorkspacesApi api = new WorkspacesApi();
//    api.setApiClient(apiClient);
//    return api;
    return workspacesApi;
  }

  @Bean
  @Scope("prototype")
  StaticNotebooksApi staticNotebooksApi() {
    return staticNotebooksApi;
  }

  @Bean
  @Scope("prototype")
  User user() { return providedUser; }

  @Bean
  @Primary
  @Qualifier("workspaceAclsApi")
  WorkspacesApi newApiClient(Provider<WorkbenchConfig> configProvider) throws IOException {
    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(BILLING_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Primary
  BillingApi billingApi(Provider<WorkbenchConfig> configProvider) throws IOException {
    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(BILLING_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    BillingApi api = new BillingApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @Primary
  GroupsApi groupsApi(Provider<WorkbenchConfig> configProvider) throws IOException {
    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(BILLING_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    GroupsApi api = new GroupsApi();
    api.setApiClient(apiClient);
    return api;
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
    workspacesApi = new WorkspacesApi();
    staticNotebooksApi = new StaticNotebooksApi();
  }

  private void impersonateUser(ApiClient apiClient) {
    profileApi.setApiClient(apiClient);
    nihApi.setApiClient(apiClient);
    workspacesApi.setApiClient(apiClient);
    staticNotebooksApi.setApiClient(apiClient);
  }

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao,
      WorkspaceService workspaceService,
      WorkspacesController workspacesController,
      FireCloudService fireCloudService,
      UserService userService,
      DirectoryService directoryService,
      UserDao userDao,
      @Qualifier("workspaceAclsApi") WorkspacesApi saWorkspaceApi) {
    return (args) -> {
      padding();
      initializeApis();
      System.out.println("Apis initialized");

      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 2 args (user). Got " + Arrays.asList(args));
      }

//      int numToProcess = Integer.parseInt(args[0]);
      String targetUser = args[0];

      List<CloneWorkspaceResponse> processedWorkspaces = new ArrayList<>();
      List<org.pmiops.workbench.firecloud.model.WorkspaceResponse> skippedWorkspaces = new ArrayList<>();
      List<org.pmiops.workbench.firecloud.model.WorkspaceResponse> failedWorkspaces = new ArrayList<>();

      for (org.pmiops.workbench.firecloud.model.WorkspaceResponse workspaceResponse : saWorkspaceApi.listWorkspaces()) {
        if (!workspaceResponse.getWorkspace().getCreatedBy().equals(targetUser)) {
          continue;
        }

        Workspace dbWorkspace = workspaceDao.findByFirecloudUuid(workspaceResponse.getWorkspace().getWorkspaceId());

        if (dbWorkspace == null) {
          System.out.println("Found workspace in FC but not recorded in AoU : " + workspaceResponse.getWorkspace());
          continue;
        }

        if (dbWorkspace.getWorkspaceActiveStatusEnum().equals(WorkspaceActiveStatus.DELETED) ||
            !dbWorkspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)) {
          continue;
        }

        if (workspaceResponse.getAccessLevel().equals(WorkspaceAccessLevel.NO_ACCESS)) {
          System.out.println("Found NO ACCESS workspace (" + workspaceResponse.getWorkspace().getNamespace() +
              "  : " + workspaceResponse.getWorkspace().getWorkspaceId() + ")");
          skippedWorkspaces.add(workspaceResponse);
          continue;
        }

        try {
          System.out.println("About to clone " + shorthand(dbWorkspace));

          providedUser = userDao.findUserByEmail(dbWorkspace.getCreator().getEmail());
          impersonateUser(fireCloudService.getApiClientWithImpersonation(providedUser.getEmail()));

          WorkspaceResponse apiWorkspace = workspacesController.getWorkspace(
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName()).getBody();

          org.pmiops.workbench.model.Workspace toWorkspace = new org.pmiops.workbench.model.Workspace();
          toWorkspace.setNamespace(dbWorkspace.getWorkspaceNamespace());
          toWorkspace.setName(dbWorkspace.getName());
          toWorkspace.setResearchPurpose(apiWorkspace.getWorkspace().getResearchPurpose());
          toWorkspace.setCdrVersionId(apiWorkspace.getWorkspace().getCdrVersionId());

          CloneWorkspaceRequest request = new CloneWorkspaceRequest();
          request.setWorkspace(toWorkspace);
          request.setIncludeUserRoles(true);

          System.out.println("Sending clone request");
          CloneWorkspaceResponse cloneResponse = workspacesController.cloneWorkspace(dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(), request).getBody();
          System.out.println("Successful Clone into " + shorthand(cloneResponse.getWorkspace()));

          dbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.MIGRATED);
          workspaceDao.save(dbWorkspace);

          processedWorkspaces.add(cloneResponse);
        } catch (WorkbenchException e) {
          System.out.println("Failed on " + shorthand(dbWorkspace));
          failedWorkspaces.add(workspaceResponse);
        }

      }

      padding();
      System.out.println("Processed Workspaces : " + processedWorkspaces.size());
      System.out.println("Skipped Workspaces : " + skippedWorkspaces.size());
      for (org.pmiops.workbench.firecloud.model.WorkspaceResponse workspaceResponse : skippedWorkspaces) {
        System.out.println(shorthand(workspaceResponse.getWorkspace()));
      }
      System.out.println("Failed Workspaces : " + failedWorkspaces.size());
      for (org.pmiops.workbench.firecloud.model.WorkspaceResponse workspaceResponse : failedWorkspaces) {
        System.out.println(shorthand(workspaceResponse.getWorkspace()));
      }
      padding();
    };
  }

  private String shorthand(Workspace workspace) {
    return "(" + workspace.getWorkspaceNamespace() + " : " + workspace.getFirecloudName() + ")";
  }

  private String shorthand(org.pmiops.workbench.model.Workspace workspace) {
    return "(" + workspace.getNamespace() + " : " + workspace.getId() + ")";
  }

  private String shorthand(org.pmiops.workbench.firecloud.model.Workspace workspace) {
    return "(" + workspace.getNamespace() + " : " + workspace.getName() + ")";
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
