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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.api.BillingV2Api;
import org.pmiops.workbench.rawls.model.RawlsCreateRawlsV2BillingProjectFullRequest;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceRequest;
import org.pmiops.workbench.tools.factories.ToolsFirecloudApiClientFactory;
import org.pmiops.workbench.tools.factories.ToolsFirecloudImpersonatedServiceAccountApiClientFactory;
import org.pmiops.workbench.tools.factories.ToolsRawlsApiClientFactory;
import org.pmiops.workbench.tools.factories.ToolsRawlsImpersonatedServiceAccountApiClientFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sets up workspace for Extraction service account 1. Creates Terra BP 2. Create Terra Workspace 3.
 * Share Workspace with given users
 */
@Configuration
public class CreateWgsCohortExtractionBillingProjectWorkspace extends Tool {

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

  public static WorkbenchConfig workbenchConfig(String configJsonFilepath) throws IOException {
    ObjectMapper jackson = new ObjectMapper();
    String rawJson =
        new String(Files.readAllBytes(Paths.get(configJsonFilepath)), Charset.defaultCharset());

    String strippedJson = rawJson.replaceAll("\\s*//.*", "");
    JsonNode newJson = jackson.readTree(strippedJson);

    return (new Gson()).fromJson(newJson.toString(), WorkbenchConfig.class);
  }

  public static ToolsFirecloudImpersonatedServiceAccountApiClientFactory
      wgsCohortExtractionServiceAccountApiClientFactory(WorkbenchConfig config) throws IOException {
    return new ToolsFirecloudImpersonatedServiceAccountApiClientFactory(
        config.wgsCohortExtraction.serviceAccount, config.firecloud.baseUrl);
  }

  public static ToolsRawlsImpersonatedServiceAccountApiClientFactory
      wgsCohortExtractionRawlsServiceAccountApiClientFactory(WorkbenchConfig config)
          throws IOException {
    return new ToolsRawlsImpersonatedServiceAccountApiClientFactory(
        config.wgsCohortExtraction.serviceAccount, config.firecloud.rawlsBaseUrl);
  }

  private String getExtractionPetSa(String googleProject, WorkbenchConfig workbenchConfig)
      throws IOException, InterruptedException {
    String accessToken =
        ToolsFirecloudImpersonatedServiceAccountApiClientFactory.getAccessToken(
            workbenchConfig.wgsCohortExtraction.serviceAccount);
    log.info("Extraction SA Access Token: " + accessToken);

    Request request =
        new Request.Builder()
            .url(
                workbenchConfig.firecloud.samBaseUrl
                    + "/api/google/v1/user/petServiceAccount/"
                    + googleProject)
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
      ToolsFirecloudApiClientFactory firecloudApiClientFactory =
          wgsCohortExtractionServiceAccountApiClientFactory(workbenchConfig);
      ToolsRawlsApiClientFactory rawlsApiClientFactory =
          wgsCohortExtractionRawlsServiceAccountApiClientFactory(workbenchConfig);

      log.info("Creating billing project");
      RawlsCreateRawlsV2BillingProjectFullRequest billingProjectRequest =
          new RawlsCreateRawlsV2BillingProjectFullRequest()
              .billingAccount("billingAccounts/" + workbenchConfig.billing.accountId)
              .projectName(opts.getOptionValue(billingProjectNameOpt.getLongOpt()));
      BillingV2Api billingV2Api = rawlsApiClientFactory.billingV2Api();
      billingV2Api.createBillingProjectFullV2(billingProjectRequest);
      String firecloudName = FireCloudService.toFirecloudName(workspaceName);

      RawlsWorkspaceRequest workspaceIngest =
          new RawlsWorkspaceRequest().namespace(billingProjectName).name(firecloudName);

      log.info(
          String.format("Creating workspace with (%s, %s)", billingProjectName, firecloudName));
      RawlsWorkspaceDetails workspace =
          rawlsApiClientFactory.workspacesApi().createWorkspace(workspaceIngest);

      log.info("Updating Workspace ACL");
      List<RawlsWorkspaceACLUpdate> acls =
          Stream.concat(
                  Arrays.stream(opts.getOptionValue(ownersOpt.getLongOpt()).split(",")),
                  Arrays.stream(new String[] {workspace.getCreatedBy()}))
              .map(email -> FirecloudTransforms.buildAclUpdate(email, WorkspaceAccessLevel.OWNER))
              .collect(Collectors.toList());
      rawlsApiClientFactory
          .workspacesApi()
          .updateACL(acls, workspace.getNamespace(), workspace.getName(), false);

      String proxyGroup =
          firecloudApiClientFactory.profileApi().getProxyGroup(workspace.getCreatedBy());

      log.info(
          "Add the following values to WorkbenchConfig.wgsCohortExtraction"
              + "serviceAccount: "
              + workbenchConfig.wgsCohortExtraction.serviceAccount
              + "\n"
              + "serviceAccountTerraProxyGroup: "
              + proxyGroup
              + "\n"
              + "extractionPetServiceAccount: "
              + getExtractionPetSa(workspace.getGoogleProject(), workbenchConfig)
              + "\n"
              + "operationalTerraWorkspaceNamespace: "
              + workspace.getNamespace()
              + "\n"
              + "operationalTerraWorkspaceName: "
              + workspace.getName()
              + "\n"
              + "operationalTerraWorkspaceBucket: "
              + workspace.getBucketName()
              + "\n");
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(
        CreateWgsCohortExtractionBillingProjectWorkspace.class, args);
  }
}
