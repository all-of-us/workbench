package org.pmiops.workbench.tools;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A tool that takes a Workspace namespace / Firecloud Project ID and returns details for any
 * workspaces found.
 *
 * <p>Details currently include... - Name - Creator Email - Collaborator Emails and Access Levels
 */
@Configuration
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
        Map<String, FirecloudWorkspaceAccessEntry> acl =
            FirecloudTransforms.extractAclResponse(
                workspacesApi.getWorkspaceAcl(
                    workspace.getWorkspaceNamespace(), workspace.getFirecloudName()));

        sb.append("\nWorkspace Name: " + workspace.getName() + "\n");
        sb.append("Workspace Namespace: " + workspace.getWorkspaceNamespace() + "\n");
        sb.append("Creator: " + workspace.getCreator().getUsername() + "\n");
        sb.append("Collaborators:\n");
        for (Map.Entry<String, FirecloudWorkspaceAccessEntry> aclEntry : acl.entrySet()) {
          sb.append("\t" + aclEntry.getKey() + " (" + aclEntry.getValue().getAccessLevel() + ")\n");
        }

        sb.append(String.join("\n", Collections.nCopies(3, "***")));
      }

      log.info(sb.toString());
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(FetchWorkspaceDetails.class, args);
  }
}
