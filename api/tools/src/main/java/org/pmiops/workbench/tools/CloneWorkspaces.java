package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import java.util.ArrayList;
import java.util.List;
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

      int processedUsers = 0;
      List<User> failedUsers = new ArrayList<>();

      int processedWorkspaces = 0;
      List<Workspace> failedWorkspaces = new ArrayList<>();

      System.out.println("Apis initialized");

      for (User user : userService.getAllUsers()) {
        if (user.getDisabled() ||
            user.getFirstSignInTime() == null ||
            directoryService.getUser(user.getEmail()) == null) {
          System.out.println("Skipping " + user.getEmail());
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

          try {
            WorkspaceAccessLevel accessLevel = workspaceService.getWorkspaceAccessLevel(
                workspace.getWorkspaceNamespace(),
                workspace.getFirecloudName());

            System.out.println(workspace.getWorkspaceNamespace() + " : " + workspace.getFirecloudName() + " : " + accessLevel + " : " + workspaceResponse.getAccessLevel().equals(accessLevel));
            processedWorkspaces++;
          } catch (WorkbenchException e) {
            failedWorkspaces.add(workspace);
          }

        }
      }

      padding();

      System.out.println("Processed Users : " + processedUsers);
      System.out.println("Failed Users : " + failedUsers.size());
      for (User user : failedUsers) {
        System.out.println(user.getEmail() + " : " + user.getFirstSignInTime());
      }

      System.out.println("Processed Workspaces : " + processedWorkspaces);
      System.out.println("Failed Workspaces : " + failedWorkspaces.size());
      for (Workspace workspace : failedWorkspaces) {
        System.out.println(workspace.getWorkspaceId() + " : " + workspace.getFirecloudUuid() + " : " +
            workspace.getWorkspaceNamespace() + " : " + workspace.getFirecloudName() + " : " +
            workspace.getCreator().getEmail() + " : " + workspace.getLastAccessedTime());
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
