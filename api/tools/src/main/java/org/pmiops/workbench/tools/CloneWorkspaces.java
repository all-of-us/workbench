package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.pmiops.workbench.api.WorkspacesController;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan({"org.pmiops.workbench.db.model"})
public class CloneWorkspaces {

  private ProfileApi profileApi;
  private NihApi nihApi;
  private WorkspacesApi workspacesApi;
  private StaticNotebooksApi staticNotebooksApi;

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
      FireCloudService fireCloudService,
      UserService userService,
      DirectoryService directoryService) {
    return (args) -> {
      padding();
      initializeApis();
      System.out.println("Apis initialized");

      int processedUsers = 0;
      int skippedUsers = 0;
      List<User> failedUsers = new ArrayList<>();

      int processedWorkspaces = 0;
      int skippedWorkspaces = 0;
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

        List<WorkspaceResponse> workspaces;
        try {
          workspaces = workspaceService.getWorkspaces();
        } catch (WorkbenchException e) {
          failedUsers.add(user);
          continue;
        }

        processedUsers++;
        for (WorkspaceResponse workspaceResponse : workspaces) {
          Workspace workspace = workspaceDao.findByWorkspaceNamespaceAndNameAndActiveStatus(
              workspaceResponse.getWorkspace().getNamespace(),
              workspaceResponse.getWorkspace().getName(),
              (short) 0
          );

          if (workspace.getCreator().getUserId() != user.getUserId()) {
            // Not counting this as a "skip" since these are duplicates
            continue;
          }

          if (workspaceResponse.getAccessLevel().equals(WorkspaceAccessLevel.NO_ACCESS)) {
            skippedWorkspaces++;
            continue;
          }

          try {
            WorkspaceAccessLevel accessLevel = workspaceService.getWorkspaceAccessLevel(
                workspace.getWorkspaceNamespace(),
                workspace.getFirecloudName());

            System.out.println("Processed " + workspace.getWorkspaceNamespace() + " : " + workspace.getFirecloudName());
            processedWorkspaces++;
          } catch (WorkbenchException e) {
            System.out.println("Failed on " + workspace.getWorkspaceNamespace() + " : " + workspace.getFirecloudName());
            failedWorkspaces.add(workspaceResponse);
          }

        }
      }

      padding();

      System.out.println("Processed Users : " + processedUsers);
      System.out.println("Skipped Users : " + skippedUsers);
      System.out.println("Failed Users : " + failedUsers.size());
      for (User user : failedUsers) {
        System.out.println(user.getEmail());
      }

      System.out.println("Processed Workspaces : " + processedWorkspaces);
      System.out.println("Skipped Workspaces : " + skippedWorkspaces);
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

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(CloneWorkspaces.class).web(false).run(args);
  }
}
