package org.pmiops.workbench.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspacesController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

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
  @Qualifier("workspacesApi")
  @Scope("prototype")
  WorkspacesApi workspacesApi() {
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
      DirectoryService directoryService) {
    return (args) -> {
      padding();
      initializeApis();
      System.out.println("Apis initialized");

      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 1 arg (numToProcess). Got " + Arrays.asList(args));
      }
      int numToProcess = Integer.parseInt(args[0]);

      int processedUsers = 0;
      int skippedUsers = 0;
      List<User> invalidAccessUsers = new ArrayList<>();
      List<User> failedUsers = new ArrayList<>();

      List<CloneWorkspaceResponse> processedWorkspaces = new ArrayList<>();
      List<WorkspaceResponse> skippedWorkspaces = new ArrayList<>();
      List<WorkspaceResponse> failedWorkspaces = new ArrayList<>();

      for (User user : userService.getAllUsers()) {
        if (user.getDisabled() ||
            user.getFirstSignInTime() == null ||
            directoryService.getUser(user.getEmail()) == null) {
          System.out.println("Skipping " + user.getEmail());
          skippedUsers++;
          continue;
        }

        System.out.println("Impersonating " + user.getEmail());
        ApiClient apiClient = fireCloudService.getApiClientWithImpersonation(user.getEmail());
        impersonateUser(apiClient);
        providedUser = user;

        List<WorkspaceResponse> workspaceResponses;
        try {
          workspaceResponses = workspaceService.getWorkspaces();
          processedUsers++;
        } catch (WorkbenchException e) {
          failedUsers.add(user);
          continue;
        }

        long noAccessCount = workspaceResponses.stream()
            .filter(wr -> wr.getAccessLevel().equals(WorkspaceAccessLevel.NO_ACCESS)).count();
        if (noAccessCount > 0) {
          System.out.println("Found a providedUser with no access : " + user.getEmail());
          invalidAccessUsers.add(user);
        }

        for (WorkspaceResponse workspaceResponse : workspaceResponses) {
          Workspace dbWorkspace = workspaceDao.findByWorkspaceNamespaceAndNameAndActiveStatus(
              workspaceResponse.getWorkspace().getNamespace(),
              workspaceResponse.getWorkspace().getName(),
              (short) 0
          );

          if (dbWorkspace.getCreator().getUserId() != user.getUserId() &&
              dbWorkspace.getBillingMigrationStatusEnum().equals(BillingMigrationStatus.OLD)
          ) {
            // Not counting this as a "skip" since these are duplicates
            continue;
          }

          if (workspaceResponse.getAccessLevel().equals(WorkspaceAccessLevel.NO_ACCESS)) {
            skippedWorkspaces.add(workspaceResponse);
            continue;
          }

          try {
            WorkspaceResponse currentApiWorkspace = workspacesController.getWorkspace(dbWorkspace.getWorkspaceNamespace(),
                dbWorkspace.getFirecloudName()).getBody();

            org.pmiops.workbench.model.Workspace toWorkspace = new org.pmiops.workbench.model.Workspace();
            toWorkspace.setNamespace(dbWorkspace.getWorkspaceNamespace());
            toWorkspace.setName(dbWorkspace.getName());
            toWorkspace.setResearchPurpose(currentApiWorkspace.getWorkspace().getResearchPurpose());
            toWorkspace.setCdrVersionId(currentApiWorkspace.getWorkspace().getCdrVersionId());

            CloneWorkspaceRequest request = new CloneWorkspaceRequest();
            request.setWorkspace(toWorkspace);
            request.setIncludeUserRoles(true);

            System.out.println("Sending clone request");
            CloneWorkspaceResponse cloneResponse = workspacesController.cloneWorkspace(dbWorkspace.getWorkspaceNamespace(),
                dbWorkspace.getFirecloudName(), request).getBody();

            dbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.MIGRATED);
            workspaceDao.save(dbWorkspace);

            System.out.println("Cloned (" + currentApiWorkspace.getWorkspace().getNamespace() + ":" +
                currentApiWorkspace.getWorkspace().getId() + ") into (" + cloneResponse.getWorkspace().getNamespace() + ":" +
                cloneResponse.getWorkspace().getName() + ")");
            processedWorkspaces.add(cloneResponse);

            if (processedWorkspaces.size() == numToProcess) {
              System.out.println("Cloned " + numToProcess + " workspaces as requested. Exiting.");
              return;
            }

          } catch (WorkbenchException e) {
            System.out.println("Failed on " + dbWorkspace.getWorkspaceNamespace() + " : " + dbWorkspace.getFirecloudName());
            failedWorkspaces.add(workspaceResponse);
          }

        }
      }

      padding();

      System.out.println("Processed Users : " + processedUsers);
      System.out.println("Skipped Users : " + skippedUsers);
      System.out.println("Invalid Access Users : " + invalidAccessUsers.size());
      for (User user : invalidAccessUsers) {
        System.out.println(user.getEmail());
      }
      System.out.println("Failed Users : " + failedUsers.size());
      for (User user : failedUsers) {
        System.out.println(user.getEmail());
      }

      System.out.println("Processed Workspaces : " + processedWorkspaces.size());
      System.out.println("Skipped Workspaces : " + skippedWorkspaces.size());
      for (WorkspaceResponse workspaceResponse : skippedWorkspaces) {
        org.pmiops.workbench.model.Workspace workspace = workspaceResponse.getWorkspace();
        System.out.println(workspace.getId() + " : " + workspaceResponse.getAccessLevel() + " : " +
            workspace.getNamespace() + " : " + workspace.getName() + " : " +
            workspace.getCreator() + " : " + workspace.getCreationTime());
      }
      System.out.println("Failed Workspaces : " + failedWorkspaces.size());
      for (WorkspaceResponse workspaceResponse : failedWorkspaces) {
        org.pmiops.workbench.model.Workspace workspace = workspaceResponse.getWorkspace();
        System.out.println(workspace.getId() + " : " + workspaceResponse.getAccessLevel() + " : " +
            workspace.getNamespace() + " : " + workspace.getName() + " : " +
            workspace.getCreator() + " : " + workspace.getCreationTime());
      }

      padding();
    };
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
