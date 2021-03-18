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
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus;
import org.pmiops.workbench.firecloud.model.FirecloudCreateRawlsBillingProjectFullRequest;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create new Terra method snapshot in Agora
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

  private static Options options =
      new Options()
          .addOption(configJsonOpt)
          .addOption(sourceGitRepoOpt)
          .addOption(sourceGitPathOpt)
          .addOption(sourceGitRefOpt);

  private static final Logger log =
      Logger.getLogger(CreateTerraMethodSnapshot.class.getName());

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

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      String configJsonFilepath = opts.getOptionValue(configJsonOpt.getLongOpt());
      String sourceGitRepo = opts.getOptionValue(sourceGitRepoOpt.getLongOpt());
      String sourceGitPath = opts.getOptionValue(sourceGitPathOpt.getLongOpt());
      String sourceGitRef = opts.getOptionValue(sourceGitRefOpt.getLongOpt());

      WorkbenchConfig workbenchConfig = workbenchConfig(configJsonFilepath);
      ApiClientFactory apiClientFactory =
          wgsCohortExtractionServiceAccountApiClientFactory(workbenchConfig);

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
      System.out.println(response.body().string());

    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(CreateTerraMethodSnapshot.class).web(false).run(args);
  }
}
