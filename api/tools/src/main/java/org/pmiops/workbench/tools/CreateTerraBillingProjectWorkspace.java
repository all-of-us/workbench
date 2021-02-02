package org.pmiops.workbench.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.*;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.RandomUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.pmiops.workbench.tools.FixDesynchronizedBillingProjectOwners.FIRECLOUD_LIST_WORKSPACES_REQUIRED_FIELDS;

/**
 * Backfill script to adjust users with improper billing project access. Dry run mode can be used to
 * audit for inconsistent access. Specifically this aims to revoke access for users who were
 * incompletely removed as OWNERs per RW-5013, though this situation can theoretically arise in the
 * event of a normal partial sharing failure (sharing and setting of the billing project role cannot
 * be done transactionally).
 */
@Configuration
public class CreateTerraBillingProjectWorkspace {

  private static Option fcBaseUrlOpt =
      Option.builder()
          .longOpt("fc-base-url")
          .desc("Firecloud API base URL")
          .required()
          .hasArg()
          .build();

  private static Option billingAccountOpt =
      Option.builder()
          .longOpt("billing-account")
          .required()
          .hasArg()
          .build();

  private static Option billingProjectNameOpt =
          Option.builder()
                  .longOpt("billing-project-name")
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
          .addOption(billingAccountOpt)
          .addOption(billingProjectNameOpt)
          .addOption(workspaceNameOpt);

  private static final Logger log =
      Logger.getLogger(CreateTerraBillingProjectWorkspace.class.getName());

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      ServiceAccountAPIClientFactory apiFactory =
              new ServiceAccountAPIClientFactory(opts.getOptionValue(fcBaseUrlOpt.getLongOpt()));


      // This snippet proves that I have the auth to run listBillingProjectMembers
//      List<FirecloudWorkspaceResponse> workspaces =
//              apiFactory.workspacesApi().listWorkspaces(FIRECLOUD_LIST_WORKSPACES_REQUIRED_FIELDS);
//
//      for (FirecloudWorkspaceResponse response : workspaces) {
//        log.info("Querying billing project members");
//        Map<String, String> billingProjectRoles =
//                apiFactory.billingApi().listBillingProjectMembers(response.getWorkspace().getNamespace()).stream()
//                        .collect(
//                                Collectors.toMap(
//                                        FirecloudBillingProjectMember::getEmail,
//                                        FirecloudBillingProjectMember::getRole));
//      }

//      FirecloudCreateRawlsBillingProjectFullRequest billingProjectRequest = new FirecloudCreateRawlsBillingProjectFullRequest()
//              .billingAccount(opts.getOptionValue(billingAccountOpt.getLongOpt()))
//              .projectName(opts.getOptionValue(billingProjectNameOpt.getLongOpt()));
//              .highSecurityNetwork(true)
 //             .enableFlowLogs(true)
  //            .privateIpGoogleAccess(true);

      String billingAccount = opts.getOptionValue(billingProjectNameOpt.getLongOpt());
      String billingProjectName = opts.getOptionValue(billingProjectNameOpt.getLongOpt());
      String workspaceName = opts.getOptionValue(workspaceNameOpt.getLongOpt());

      FirecloudCreateRawlsBillingProjectFullRequest billingProjectRequest =
              new FirecloudCreateRawlsBillingProjectFullRequest()
                      .billingAccount("billingAccounts/" + opts.getOptionValue(billingAccountOpt.getLongOpt()))
                      .projectName(opts.getOptionValue(billingProjectNameOpt.getLongOpt()))
                      .highSecurityNetwork(true)
                      .enableFlowLogs(true)
                      .privateIpGoogleAccess(true);

      log.info("Creating billing project");
      apiFactory.billingApi().createBillingProjectFull(billingProjectRequest);

      FirecloudBillingProjectStatus.CreationStatusEnum status = apiFactory.billingApi().billingProjectStatus(billingProjectRequest.getProjectName()).getCreationStatus();
      while (status != FirecloudBillingProjectStatus.CreationStatusEnum.READY) {
        log.info("Polling status: " + status.getValue());
        Thread.sleep(5000);
        status = apiFactory.billingApi().billingProjectStatus(billingProjectRequest.getProjectName()).getCreationStatus();
      }

      final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz";
      final int NUM_RANDOM_CHARS = 20;

      // Find a unique workspace namespace based off of the provided name.
      String strippedName = workspaceName.toLowerCase().replaceAll("[^0-9a-z]", "");
      // If the stripped name has no chars, generate a random name.
      if (strippedName.isEmpty()) {
        strippedName = RandomUtils.generateRandomChars(RANDOM_CHARS, NUM_RANDOM_CHARS);
      }
      DbWorkspace.FirecloudWorkspaceId workspaceId = new DbWorkspace.FirecloudWorkspaceId(billingProjectName, strippedName);

      FirecloudWorkspaceIngest workspaceIngest =
              new FirecloudWorkspaceIngest()
                      .namespace(workspaceId.getWorkspaceNamespace())
                      .name(workspaceId.getWorkspaceName());
      // might need to add authorization domain

      log.info("Creating workspace with (" + workspaceId.getWorkspaceNamespace() + ", " + workspaceId.getWorkspaceName() + ")");
      FirecloudWorkspace workspace = apiFactory.workspacesApi().createWorkspace(workspaceIngest);

      FirecloudWorkspaceACLUpdate aclUpdate = new FirecloudWorkspaceACLUpdate()
              .email("eric.song@pmi-ops.org")
              .accessLevel(WorkspaceAccessLevel.OWNER.toString())
              .canCompute(true)
              .canShare(true);

      log.info("Updating Workspace ACL");
      apiFactory.workspacesApi().updateWorkspaceACL(workspace.getNamespace(), workspace.getName(), false, Collections.singletonList(aclUpdate));
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(CreateTerraBillingProjectWorkspace.class).web(false).run(args);
  }
}
