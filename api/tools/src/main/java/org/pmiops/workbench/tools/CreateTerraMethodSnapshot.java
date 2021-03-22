package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus;
import org.pmiops.workbench.firecloud.model.FirecloudCreateRawlsBillingProjectFullRequest;
import org.pmiops.workbench.firecloud.model.FirecloudMethodID;
import org.pmiops.workbench.firecloud.model.FirecloudMethodIO;
import org.pmiops.workbench.firecloud.model.FirecloudMethodInput;
import org.pmiops.workbench.firecloud.model.FirecloudMethodQuery;
import org.pmiops.workbench.firecloud.model.FirecloudMethodResponse;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceIngest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create new Terra method snapshot in Agora
 *
 * Assumptions
 *   - The provided Agora method namespace does not exist yet OR the cohort extraction service
 *   account has write permission to it if it exists.
 */
@Configuration
public class CreateTerraMethodSnapshot {

  private static Option configJsonOpt =
      Option.builder().longOpt("config-json").required().hasArg().build();

  private static Option sourceGitRepoOpt =
      Option.builder().longOpt("source-git-repo").required().hasArg().build();

  private static Option sourceGitPathOpt =
          Option.builder().longOpt("source-git-path").required().hasArg().build();

  private static Option sourceGitRefOpt =
          Option.builder().longOpt("source-git-ref").required().hasArg().build();

  private static Option methodNamespaceOpt =
          Option.builder().longOpt("method-namespace").required().hasArg().build();

  private static Option methodNameOpt =
          Option.builder().longOpt("method-name").required().hasArg().build();

  private static Options options =
      new Options()
          .addOption(configJsonOpt)
          .addOption(sourceGitRepoOpt)
          .addOption(sourceGitPathOpt)
          .addOption(sourceGitRefOpt)
          .addOption(methodNamespaceOpt)
          .addOption(methodNameOpt);

  private static final Logger log =
      Logger.getLogger(CreateTerraMethodSnapshot.class.getName());

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      String configJsonFilepath = opts.getOptionValue(configJsonOpt.getLongOpt());
      String sourceGitRepo = opts.getOptionValue(sourceGitRepoOpt.getLongOpt());
      String sourceGitPath = opts.getOptionValue(sourceGitPathOpt.getLongOpt());
      String sourceGitRef = opts.getOptionValue(sourceGitRefOpt.getLongOpt());
      String methodNamespace = opts.getOptionValue(methodNamespaceOpt.getLongOpt());
      String methodName = opts.getOptionValue(methodNameOpt.getLongOpt());

      WorkbenchConfig workbenchConfig = CreateWgsCohortExtractionBillingProjectWorkspace.workbenchConfig(configJsonFilepath);
      ApiClientFactory apiClientFactory =
          CreateWgsCohortExtractionBillingProjectWorkspace.wgsCohortExtractionServiceAccountApiClientFactory(workbenchConfig);

      String sourceFileContents = getGithubFileContents(sourceGitRepo, sourceGitPath, sourceGitRef);

      List<FirecloudMethodResponse> existingMethods = apiClientFactory.methodRepositoryApi().listMethodRepositoryMethods(
              methodNamespace,
              methodName,
              null, null, null, null, null, null, null
      );

      DateFormat formatter = new SimpleDateFormat
              ("EEE MMM dd HH:mm:ss zzz yyyy");
      formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));

      String snapshotComment = "Sourced from Github (repo, ref, path) (" + sourceGitRepo + ", " + sourceGitRef + ", " + sourceGitPath + ") on " + formatter.format(new Date());
      FirecloudMethodQuery newMethodQuery = new FirecloudMethodQuery()
              .namespace(methodNamespace)
              .name(methodName)
              .entityType("Workflow")
              .snapshotComment(snapshotComment)
              .payload(sourceFileContents);

      FirecloudMethodResponse methodResponse;
      if (existingMethods.isEmpty()) {
        log.info("No existing methods found with given namespace/name; creating new method.");
        try {
          methodResponse = apiClientFactory.methodRepositoryApi().createMethod(newMethodQuery);
        } catch (ApiException e) {
          log.warning(e.getResponseBody());
          throw e;
        }
      } else {
        log.info("Method already exists. Creating new snapshot.");
        int latestSnapshotId = existingMethods.stream().map(method -> method.getSnapshotId()).max(Integer::compare).get();

        try {
          methodResponse = apiClientFactory.methodRepositoryApi()
                  .createMethodSnapshot(methodNamespace, methodName, Integer.toString(latestSnapshotId), newMethodQuery, "false");
        } catch (ApiException e) {
          log.warning(e.getResponseBody());
          throw e;
        }
      }

      FirecloudMethodIO methodIO = apiClientFactory.methodRepositoryApi().getMethodIO(
              new FirecloudMethodID()
              .methodNamespace(methodResponse.getNamespace())
              .methodName(methodResponse.getName())
              .methodVersion(methodResponse.getSnapshotId())
      );

      log.info("\n\n\n" +
              "Environment: " + workbenchConfig.server.projectId + "\n" +
              "New snapshot namespace/name/version: " +
              methodResponse.getNamespace() + "/" +
              methodResponse.getName() + "/" +
              methodResponse.getSnapshotId() + "\n\n" +
              "New snapshot inputs: \n" +
              methodIO.getInputs().stream().map(input ->
                      input.getInputType() + " " + input.getName() + " " + (input.getOptional() ? "(optional)" : "(required)")
              ).collect(Collectors.joining("\n")) +
              "\n\n\n");
    };
  }

  private String getGithubFileContents(String sourceGitRepo, String sourceGitPath, String sourceGitRef) throws IOException {
    Request request =
            new Request.Builder()
                    .url(
                            "https://api.github.com/repos/" +
                                    sourceGitRepo +
                                    "/contents/" +
                                    sourceGitPath +
                                    "?ref=" + sourceGitRef)
                    .addHeader("Accept", "application/vnd.github.v3.raw")
                    .build();

    final OkHttpClient client = new OkHttpClient();
    Response response = client.newCall(request).execute();

    if (response.code() != 200) {
      log.warning(response.body().string());
      throw new RuntimeException("Could not fetch source file from Github. Response Code: " + response.code());
    }

    return response.body().string();
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(CreateTerraMethodSnapshot.class).web(false).run(args);
  }
}
