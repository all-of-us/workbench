package org.pmiops.workbench.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.model.*;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.RandomUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Backfill script to adjust users with improper billing project access. Dry run mode can be used to
 * audit for inconsistent access. Specifically this aims to revoke access for users who were
 * incompletely removed as OWNERs per RW-5013, though this situation can theoretically arise in the
 * event of a normal partial sharing failure (sharing and setting of the billing project role cannot
 * be done transactionally).
 */
@Configuration
public class CreateTerraSubmission {

  private static Option fcBaseUrlOpt =
      Option.builder()
          .longOpt("fc-base-url")
          .desc("Firecloud API base URL")
          .required()
          .hasArg()
          .build();

  private static Option workspaceNamespaceOpt =
      Option.builder()
          .longOpt("workspace-namespace")
          .required()
          .hasArg()
          .build();

  private static Option workspaceNameOpt =
          Option.builder()
                  .longOpt("workspace-name")
                  .required()
                  .hasArg()
                  .build();

  private static Options options =
      new Options()
          .addOption(fcBaseUrlOpt)
          .addOption(workspaceNamespaceOpt)
          .addOption(workspaceNameOpt);

  private static final Logger log =
      Logger.getLogger(CreateTerraSubmission.class.getName());

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      ServiceAccountAPIClientFactory apiFactory =
              new ServiceAccountAPIClientFactory(opts.getOptionValue(fcBaseUrlOpt.getLongOpt()));

      log.info(apiFactory.profileApi().getProxyGroup("all-of-us-workbench-test@appspot.gserviceaccount.com"));

      String workspaceNamespace = opts.getOptionValue(workspaceNamespaceOpt.getLongOpt());
      String workspaceName = opts.getOptionValue(workspaceNameOpt.getLongOpt());

      FirecloudWorkspaceACLUpdate aclUpdate = new FirecloudWorkspaceACLUpdate()
              .email("all-of-us-workbench-test@appspot.gserviceaccount.com")
              .accessLevel(WorkspaceAccessLevel.OWNER.toString())
              .canCompute(true)
              .canShare(true);

      FirecloudWorkspaceACLUpdate aclUpdate2 = new FirecloudWorkspaceACLUpdate()
              .email("songe@broadinstitute.org")
              .accessLevel(WorkspaceAccessLevel.OWNER.toString())
              .canCompute(true)
              .canShare(true);

      FirecloudWorkspaceACLUpdate aclUpdate3 = new FirecloudWorkspaceACLUpdate()
              .email("wgs-cohort-extraction@all-of-us-workbench-test.iam.gserviceaccount.com")
              .accessLevel(WorkspaceAccessLevel.OWNER.toString())
              .canCompute(true)
              .canShare(true);

      List<FirecloudWorkspaceACLUpdate> acls = Arrays.asList(aclUpdate2, aclUpdate3);
      apiFactory.workspacesApi().updateWorkspaceACL(workspaceNamespace, workspaceName, true, acls);

      FirecloudWorkspaceACL acl = apiFactory.workspacesApi().getWorkspaceAcl(workspaceNamespace, workspaceName);
      for (Map.Entry<String, FirecloudWorkspaceAccessEntry> entry : acl.getAcl().entrySet()) {
        log.info("ACL");
        log.info(entry.getKey());
        log.info(entry.getValue().toString());
      }

      try {
        for (FirecloudMethodConfiguration methodConfiguration : apiFactory.methodconfigsApi().listMethodConfigurations(workspaceNamespace, workspaceName, true)) {
          log.info(methodConfiguration.toString());
        }

        FirecloudMethodConfiguration methodConfiguration = apiFactory.methodconfigsApi().listMethodConfigurations(workspaceNamespace, workspaceName, true).get(0);

        apiFactory.submissionsApi().createSubmission(workspaceNamespace, workspaceName,
                new FirecloudSubmissionRequest()
                        .useCallCache(false)
                        .deleteIntermediateOutputFiles(false)
                        .methodConfigurationNamespace(methodConfiguration.getNamespace())
                        .methodConfigurationName(methodConfiguration.getName())
        );

//        for (FirecloudSubmission submission : apiFactory.submissionsApi().listSubmissions(workspaceNamespace, workspaceName)) {
//          log.info(submission.toString());
//        }
      } catch (ApiException e) {
        log.warning(e.getResponseBody());
        log.warning(e.getMessage());
        log.warning(e.getStackTrace().toString());
        e.printStackTrace();
      }
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(CreateTerraSubmission.class).web(false).run(args);
  }
}
