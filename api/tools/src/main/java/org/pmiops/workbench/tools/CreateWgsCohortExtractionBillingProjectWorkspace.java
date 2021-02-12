package org.pmiops.workbench.tools;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.model.*;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sets up workspace for Extraction service account 1. Creates Terra BP 2. Create Terra Workspace 3.
 * Share Workspace with given users
 */
@Configuration
public class CreateWgsCohortExtractionBillingProjectWorkspace {

  private static Option billingAccountOpt =
      Option.builder().longOpt("billing-account").required().hasArg().build();

  private static Option billingProjectNameOpt =
      Option.builder().longOpt("billing-project-name").required().hasArg().build();

  private static Option workspaceNameOpt =
      Option.builder().longOpt("workspace-name").required().hasArg().build();

  private static Option ownersOpt = Option.builder().longOpt("owners").required().hasArg().build();

  private static Options options =
      new Options()
          .addOption(billingAccountOpt)
          .addOption(billingProjectNameOpt)
          .addOption(workspaceNameOpt)
          .addOption(ownersOpt);

  private static final Logger log =
      Logger.getLogger(CreateWgsCohortExtractionBillingProjectWorkspace.class.getName());

  @Bean
  ImpersonatedServiceAccountApiClientFactory apiClientFactory(WorkbenchConfig config) {
    // TODO : replace string with `config.auth.extractionServiceAccount` once that value is in the
    // test db
    return new ImpersonatedServiceAccountApiClientFactory(
        "wgs-cohort-extraction@all-of-us-workbench-test.iam.gserviceaccount.com", config);
  }

  @Bean
  public CommandLineRunner run(ImpersonatedServiceAccountApiClientFactory apiClientFactory) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      BillingApi billingApi = apiClientFactory.billingApi();
      String billingAccount = opts.getOptionValue(billingAccountOpt.getLongOpt());
      String billingProjectName = opts.getOptionValue(billingProjectNameOpt.getLongOpt());
      String workspaceName = opts.getOptionValue(workspaceNameOpt.getLongOpt());

      FirecloudCreateRawlsBillingProjectFullRequest billingProjectRequest =
          new FirecloudCreateRawlsBillingProjectFullRequest()
              .billingAccount("billingAccounts/" + billingAccount)
              .projectName(opts.getOptionValue(billingProjectNameOpt.getLongOpt()));

      log.info("Creating billing project");
      billingApi.createBillingProjectFull(billingProjectRequest);

      while (billingApi
              .billingProjectStatus(billingProjectRequest.getProjectName())
              .getCreationStatus()
          != FirecloudBillingProjectStatus.CreationStatusEnum.READY) {
        log.info("Billing project is not ready yet");
        Thread.sleep(5000);
      }

      String strippedName = workspaceName.toLowerCase().replaceAll("[^0-9a-z]", "");
      DbWorkspace.FirecloudWorkspaceId workspaceId =
          new DbWorkspace.FirecloudWorkspaceId(billingProjectName, strippedName);

      FirecloudWorkspaceIngest workspaceIngest =
          new FirecloudWorkspaceIngest()
              .namespace(workspaceId.getWorkspaceNamespace())
              .name(workspaceId.getWorkspaceName());

      log.info(
          "Creating workspace with ("
              + workspaceId.getWorkspaceNamespace()
              + ", "
              + workspaceId.getWorkspaceName()
              + ")");
      FirecloudWorkspace workspace =
          apiClientFactory.workspacesApi().createWorkspace(workspaceIngest);

      log.info("Updating Workspace ACL");
      List<FirecloudWorkspaceACLUpdate> acls =
          Arrays.stream(opts.getOptionValue(ownersOpt.getLongOpt()).split(","))
              .map(
                  email ->
                      new FirecloudWorkspaceACLUpdate()
                          .email(email)
                          .accessLevel(WorkspaceAccessLevel.OWNER.toString())
                          .canCompute(true)
                          .canShare(true))
              .collect(Collectors.toList());
      apiClientFactory
          .workspacesApi()
          .updateWorkspaceACL(workspace.getNamespace(), workspace.getName(), false, acls);

      log.info("Workspace created by " + workspace.getCreatedBy());
      String proxyGroup = apiClientFactory.profileApi().getProxyGroup(workspace.getCreatedBy());
      Matcher m = Pattern.compile("PROXY_(\\d+)@.*").matcher(proxyGroup);
      m.matches();
      String proxyGroupId = m.group(1);
      log.info(workspace.getNamespace());
      String petSaAccount =
          "pet-" + proxyGroupId + "@" + billingProjectName + ".iam.gserviceaccount.com";

      log.info("Proxy group is " + proxyGroup);
      log.info("Pet SA account is " + petSaAccount);
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(CreateWgsCohortExtractionBillingProjectWorkspace.class, args);
  }
}
