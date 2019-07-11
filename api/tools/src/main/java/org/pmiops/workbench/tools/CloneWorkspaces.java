package org.pmiops.workbench.tools;

import org.pmiops.workbench.api.WorkspacesController;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan({"org.pmiops.workbench.db.model", "org.pmiops.workbench.api", "org.pmiops.workbench.workspaces"})
public class CloneWorkspaces {

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, WorkspacesController workspacesController) {
    return (args) -> {
//      workspacesController.cloneWorkspace();

      Workspace workspace =
          workspaceDao.findByWorkspaceNamespaceAndNameAndActiveStatus("aou-rw-test-299", "hasspace", (short) 0);

      System.out.println("" + workspace.getWorkspaceId() +
          " : " + workspace.getName() + " : " + workspace.getWorkspaceNamespace() + " : " + workspace.getFirecloudName());

    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(CloneWorkspaces.class).web(false).run(args);
  }
}
