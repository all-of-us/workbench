package org.pmiops.workbench.tools;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.pmiops.workbench.api.WorkspacesController;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.workspaces.WorkspaceService;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan({"org.pmiops.workbench.db.model", "org.pmiops.workbench.workspaces", "org.pmiops.workbench.db.dao"})
public class CloneWorkspaces {

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, WorkspaceService workspaceService) {
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
