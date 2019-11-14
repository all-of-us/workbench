package org.pmiops.workbench.tools;

import static org.pmiops.workbench.tools.BackfillBillingProjectUsers.extractAclResponse;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * A tool that takes an AoU username (e.g. gjordan) and fetches the FireCloud profile associated
 * with that AoU user.
 *
 * <p>This is intended mostly for demonstration / testing purposes, to show how we leverage
 * domain-wide delegation to make FireCloud API calls impersonating other users.
 */
@SpringBootApplication
// Load the DBA and DB model classes required for UserDao.
@EnableJpaRepositories({"org.pmiops.workbench.db.dao"})
@EntityScan("org.pmiops.workbench.db.model")
public class FetchWorkspaceDetails {
  private static final Logger log = Logger.getLogger(FetchWorkspaceDetails.class.getName());

  private static Option fcBaseUrlOpt =
      Option.builder()
          .longOpt("fc-base-url")
          .desc("Firecloud API base URL")
          .required()
          .hasArg()
          .build();

  private static Option projectIdOpt =
      Option.builder().longOpt("projectId").desc("Billing project ID").required().hasArg().build();

  private static Options options = new Options().addOption(fcBaseUrlOpt).addOption(projectIdOpt);

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      WorkspacesApi workspacesApi =
          (new ServiceAccountAPIClientFactory(opts.getOptionValue(fcBaseUrlOpt.getLongOpt())))
              .workspacesApi();

      StringBuilder sb = new StringBuilder();
      sb.append(String.join("\n", Collections.nCopies(10, "***")));

      for (DbWorkspace workspace :
          workspaceDao.findAllByWorkspaceNamespace(
              opts.getOptionValue(projectIdOpt.getLongOpt()))) {
        Map<String, WorkspaceAccessEntry> acl =
            extractAclResponse(
                workspacesApi.getWorkspaceAcl(
                    workspace.getWorkspaceNamespace(), workspace.getFirecloudName()));

        sb.append("\nWorkspace Name: " + workspace.getName() + "\n");
        sb.append("Workspace Namespace: " + workspace.getWorkspaceNamespace() + "\n");
        sb.append("Creator: " + workspace.getCreator().getEmail() + "\n");
        sb.append("Collaborators:\n");
        for (Map.Entry<String, WorkspaceAccessEntry> aclEntry : acl.entrySet()) {
          sb.append("\t" + aclEntry.getKey() + " (" + aclEntry.getValue().getAccessLevel() + ")\n");
        }

        sb.append(String.join("\n", Collections.nCopies(3, "***")));
      }

      log.info(sb.toString());
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(FetchWorkspaceDetails.class).web(false).run(args);
  }
}
