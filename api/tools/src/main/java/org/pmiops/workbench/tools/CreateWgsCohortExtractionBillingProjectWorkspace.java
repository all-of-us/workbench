package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  private static Option configJsonOpt =
      Option.builder().longOpt("config-json").required().hasArg().build();

  private static Option billingProjectNameOpt =
      Option.builder().longOpt("billing-project-name").required().hasArg().build();

  private static Option workspaceNameOpt =
      Option.builder().longOpt("workspace-name").required().hasArg().build();

  private static Option ownersOpt = Option.builder().longOpt("owners").required().hasArg().build();

  private static Options options =
      new Options()
          .addOption(configJsonOpt)
          .addOption(billingProjectNameOpt)
          .addOption(workspaceNameOpt)
          .addOption(ownersOpt);

  private static final Logger log =
      Logger.getLogger(CreateWgsCohortExtractionBillingProjectWorkspace.class.getName());

  WorkbenchConfig workbenchConfig(String configJsonFilepath) throws IOException {
    ObjectMapper jackson = new ObjectMapper();
    String rawJson =
        new String(Files.readAllBytes(Paths.get(configJsonFilepath)), Charset.defaultCharset());

    String strippedJson = rawJson.replaceAll("\\s*//.*", "");
    JsonNode newJson = jackson.readTree(strippedJson);

    return (new Gson()).fromJson(newJson.toString(), WorkbenchConfig.class);
  }

  ImpersonatedServiceAccountApiClientFactory wgsCohortExtractionServiceAccountApiClientFactory(
      WorkbenchConfig config) throws IOException {
    return new ImpersonatedServiceAccountApiClientFactory(
        config.wgsCohortExtraction.serviceAccount, config.firecloud.baseUrl);
  }

  private String getExtractionPetSa(String workspaceNamespace, WorkbenchConfig workbenchConfig)
      throws IOException, InterruptedException {
    String accessToken =
        ImpersonatedServiceAccountApiClientFactory.getAccessToken(
            workbenchConfig.wgsCohortExtraction.serviceAccount);
    log.info("Extraction SA Access Token: " + accessToken);

    Request request =
        new Request.Builder()
            .url(
                workbenchConfig.firecloud.samBaseUrl
                    + "/api/google/v1/user/petServiceAccount/"
                    + workspaceNamespace)
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();

    final OkHttpClient client = new OkHttpClient();
    Response response = client.newCall(request).execute();

    // The first call seems to always fail for some reason. Just retry a few times
    for (int retries = 0; !response.isSuccessful() && retries < 3; retries++) {
      Thread.sleep(5000);
      response = client.newCall(request).execute();
    }

    if (!response.isSuccessful()) {
      throw new RuntimeException(
          "Could not fetch Pet Service Account. Try fetching manually by querying "
              + request.urlString()
              + "with the Extraction SA Access Token (printed above).");
    }

    return response.body().string();
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      String configJsonFilepath = opts.getOptionValue(configJsonOpt.getLongOpt());
      String billingProjectName = opts.getOptionValue(billingProjectNameOpt.getLongOpt());
      String workspaceName = opts.getOptionValue(workspaceNameOpt.getLongOpt());

      WorkbenchConfig workbenchConfig = workbenchConfig(configJsonFilepath);
      ApiClientFactory apiClientFactory =
          wgsCohortExtractionServiceAccountApiClientFactory(workbenchConfig);

      FirecloudCreateRawlsBillingProjectFullRequest billingProjectRequest =
          new FirecloudCreateRawlsBillingProjectFullRequest()
              .billingAccount("billingAccounts/" + workbenchConfig.billing.accountId)
              .projectName(opts.getOptionValue(billingProjectNameOpt.getLongOpt()));

      log.info("Creating billing project");
      BillingApi billingApi = apiClientFactory.billingApi();
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
          Stream.concat(
                  Arrays.stream(opts.getOptionValue(ownersOpt.getLongOpt()).split(",")),
                  Arrays.stream(new String[] {workspace.getCreatedBy()}))
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
      log.info("Workspace bucket is " + workspace.getBucketName());
      log.info("Proxy group is " + proxyGroup);
      log.info(
          "Pet SA account is " + getExtractionPetSa(workspace.getNamespace(), workbenchConfig));

/* TODO format this
      "serviceAccount": "wgs-cohort-extraction@all-of-us-rw-staging.iam.gserviceaccount.com",
              "serviceAccountTerraProxyGroup": "",
              "operationalTerraWorkspaceNamespace": "",
              "operationalTerraWorkspaceName": "",
              "operationalTerraWorkspaceBucket": "",
              */
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(
        CreateWgsCohortExtractionBillingProjectWorkspace.class, args);
  }
}
