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
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.rawls.api.WorkspacesApi;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.tools.factories.ToolsRawlsServiceAccountApiClientFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * A tool that takes a Workspace namespace / Firecloud Project ID and returns details for any
 * workspaces found.
 */
@Configuration
@Import({FireCloudServiceImpl.class, FireCloudConfig.class})
public class FetchWorkspaceDetails extends Tool {

  private static final Logger log = Logger.getLogger(FetchWorkspaceDetails.class.getName());

  private static Option rawlsBaseUrlOpt =
      Option.builder()
          .longOpt("rawls-base-url")
          .desc("Rawls API base URL")
          .required()
          .hasArg()
          .build();

  private static Option workspaceProjectIdOpt =
      Option.builder()
          .longOpt("workspace-project-id")
          .desc("Workspace billing project ID to fetch details for")
          .required()
          .hasArg()
          .build();

  private static Options options =
      new Options().addOption(rawlsBaseUrlOpt).addOption(workspaceProjectIdOpt);

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, FireCloudService fireCloudService) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      String workspaceNamespace = opts.getOptionValue(workspaceProjectIdOpt.getLongOpt());

      WorkspacesApi workspacesApi =
          (new ToolsRawlsServiceAccountApiClientFactory(
                  opts.getOptionValue(rawlsBaseUrlOpt.getLongOpt())))
              .workspacesApi();

      StringBuilder sb = new StringBuilder();
      sb.append(String.join("\n", Collections.nCopies(10, "***")));

      for (DbWorkspace workspace : workspaceDao.findAllByWorkspaceNamespace(workspaceNamespace)) {
        Map<String, RawlsWorkspaceAccessEntry> acl =
            FirecloudTransforms.extractAclResponse(
                workspacesApi.getACL(
                    workspace.getWorkspaceNamespace(), workspace.getFirecloudName()));

        String bucketName =
            fireCloudService
                .getWorkspaceAsService(workspaceNamespace, workspace.getFirecloudName())
                .getWorkspace()
                .getBucketName();

        sb.append("\nWorkspace Name: " + workspace.getName() + "\n");
        sb.append("Workspace Namespace: " + workspace.getWorkspaceNamespace() + "\n");
        sb.append("Creator: " + workspace.getCreator().getUsername() + "\n");
        sb.append("GCS bucket path: gs://" + bucketName + "\n");
        sb.append("Collaborators:\n");
        for (Map.Entry<String, RawlsWorkspaceAccessEntry> aclEntry : acl.entrySet()) {
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
