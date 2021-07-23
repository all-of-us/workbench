package org.pmiops.workbench.tools;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.common.collect.ImmutableList;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tool to generate an impersonated oauth token for a research user in an given environment. */
@Configuration
public class GenerateImpersonatedUserTokens {

  private static Option projectId =
      Option.builder()
          .longOpt("project-id")
          .desc("Workbench project id indicating the environment, e.g. all-of-us-rw-perf")
          .required()
          .hasArg()
          .build();
  private static Option impersonatedUsername =
      Option.builder()
          .longOpt("impersonated-username")
          .desc("AoU researcher email to impersonate, e.g. calbach@fake-research-aou.org")
          .required()
          .hasArg()
          .build();
  private static Option outputTokenFilename =
      Option.builder()
          .longOpt("output-token-filename")
          .desc("Path to an output file for the generated impersonated token")
          .required()
          .hasArg()
          .build();

  private static Options options =
      new Options()
          .addOption(projectId)
          .addOption(impersonatedUsername)
          .addOption(outputTokenFilename);

  private static final String ADMIN_SERVICE_ACCOUNT_NAME = "firecloud-admin";

  private static final Logger log =
      Logger.getLogger(GenerateImpersonatedUserTokens.class.getName());
  private static final List<String> AOU_SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/cloud-billing");

  private void writeTokens(String projectId, String[] usernames, String[] filenames)
      throws IOException {
    final String saEmail =
        ServiceAccounts.getServiceAccountEmail(ADMIN_SERVICE_ACCOUNT_NAME, projectId);
    final IamCredentialsClient credsClient = IamCredentialsClient.create();
    final HttpTransport transport = new ApacheHttpTransport();

    for (int i = 0; i < usernames.length; i++) {
      final String username = usernames[i];
      final String filename = filenames[i];

      final String logMsg =
          String.format("Writing impersonated user credential for %s to %s", username, filename);
      log.info(logMsg);

      final DelegatedUserCredentials creds =
          new DelegatedUserCredentials(saEmail, username, AOU_SCOPES, credsClient, transport);
      creds.refresh();
      final String token = creds.getAccessToken().getTokenValue();

      try (FileWriter w = new FileWriter(filename)) {
        w.write(token);
      }
    }
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      final String project = opts.getOptionValue(projectId.getLongOpt());
      final String[] usernames = opts.getOptionValues(impersonatedUsername.getLongOpt());
      final String[] filenames = opts.getOptionValues(outputTokenFilename.getLongOpt());

      if (usernames.length != filenames.length) {
        final String errorMsg =
            String.format(
                "Username and filename arguments must have the same length. "
                    + "Given: %d usernames and %d filenames",
                usernames.length, filenames.length);
        throw new IllegalArgumentException(errorMsg);
      }

      writeTokens(project, usernames, filenames);
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(GenerateImpersonatedUserTokens.class)
        .web(WebApplicationType.NONE)
        .run(args)
        .close();
  }
}
