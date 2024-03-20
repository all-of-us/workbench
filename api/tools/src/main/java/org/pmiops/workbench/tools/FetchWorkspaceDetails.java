package org.pmiops.workbench.tools;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.google.GoogleConfig;
import org.pmiops.workbench.rawls.ApiException;
import org.pmiops.workbench.rawls.RawlsApiClientFactory;
import org.pmiops.workbench.rawls.RawlsConfig;
import org.pmiops.workbench.rawls.api.WorkspacesApi;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.sam.SamRetryHandler;
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
@Import({
  FireCloudConfig.class,
  FireCloudServiceImpl.class,
  GoogleConfig.class, // injects com.google.cloud.iam.credentials.v1.IamCredentialsClient
  RawlsApiClientFactory.class,
  RawlsConfig.class,
  SamRetryHandler.class,
})
public class FetchWorkspaceDetails extends Tool {

  private static final Logger log = Logger.getLogger(FetchWorkspaceDetails.class.getName());

  private static Option rawlsBaseUrlOpt =
      Option.builder()
          .longOpt("rawls-base-url")
          .desc("Rawls API base URL")
          .required()
          .hasArg()
          .build();

  private static Option workspaceNamespaceOpt =
      Option.builder()
          .longOpt("workspace-namespace")
          .desc("Workspace namespace to fetch details for")
          .required()
          .hasArg()
          .build();

  private static Options options =
      new Options().addOption(rawlsBaseUrlOpt).addOption(workspaceNamespaceOpt);

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, FireCloudService fireCloudService) {
    return args -> {
      // project.rb swallows exceptions, so we need to catch and log them here
      try {
        getDetails(workspaceDao, fireCloudService, args);
      } catch (Exception e) {
        log.severe("Error fetching workspace details: " + e.getMessage());
        e.printStackTrace();
      }
    };
  }

  public void getDetails(
      WorkspaceDao workspaceDao, FireCloudService fireCloudService, String[] args)
      throws ParseException, IOException, ApiException {
    CommandLine opts = new DefaultParser().parse(options, args);

    String workspaceNamespace = opts.getOptionValue(workspaceNamespaceOpt.getLongOpt());

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

      sb.append("\nWorkspace Name: ").append(workspace.getName()).append("\n");
      sb.append("Workspace Namespace: ").append(workspace.getWorkspaceNamespace()).append("\n");
      sb.append("Google Project: ").append(workspace.getGoogleProject()).append("\n");
      sb.append("Creation time: ").append(workspace.getCreationTime()).append("\n");
      sb.append("Last modified time: ").append(workspace.getLastModifiedTime()).append("\n");
      sb.append("Last modified by: ").append(workspace.getLastModifiedBy()).append("\n");
      sb.append("Creator: ").append(workspace.getCreator().getUsername()).append("\n");
      sb.append("Creator institutional email: ")
          .append(workspace.getCreator().getContactEmail())
          .append("\n");
      sb.append("GCS bucket path: gs://").append(bucketName).append("\n");
      sb.append("Collaborators:\n");
      for (Map.Entry<String, RawlsWorkspaceAccessEntry> aclEntry : acl.entrySet()) {
        sb.append("\t")
            .append(aclEntry.getKey())
            .append(" (")
            .append(aclEntry.getValue().getAccessLevel())
            .append(")\n");
      }

      sb.append(String.join("\n", Collections.nCopies(3, "***")));
    }

    log.info(sb.toString());
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(FetchWorkspaceDetails.class, args);
  }
}
