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
public class GenerateImpersonatedUserToken {

  private static Option outputTokenFilename =
      Option.builder()
          .longOpt("output-token-filename")
          .desc("Path to an output file for the generated impersonated token")
          .required()
          .hasArg()
          .build();
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

  private static Options options =
      new Options()
          .addOption(outputTokenFilename)
          .addOption(projectId)
          .addOption(impersonatedUsername);

  private static final String ADMIN_SERVICE_ACCOUNT_NAME = "firecloud-admin";

  private static final Logger log = Logger.getLogger(GenerateImpersonatedUserToken.class.getName());
  private static final List<String> AOU_SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/cloud-billing");

  private static String generateToken(
      String userEmail,
      String projectId,
      IamCredentialsClient iamCredentialsClient,
      HttpTransport httpTransport)
      throws IOException {
    DelegatedUserCredentials creds =
        new DelegatedUserCredentials(
            ServiceAccounts.getServiceAccountEmail(ADMIN_SERVICE_ACCOUNT_NAME, projectId),
            userEmail,
            AOU_SCOPES,
            iamCredentialsClient,
            httpTransport);
    creds.refresh();
    return creds.getAccessToken().getTokenValue();
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      try (FileWriter w = new FileWriter(opts.getOptionValue(outputTokenFilename.getLongOpt()))) {
        w.write(
            generateToken(
                opts.getOptionValue(impersonatedUsername.getLongOpt()),
                opts.getOptionValue(projectId.getLongOpt()),
                IamCredentialsClient.create(),
                new ApacheHttpTransport()));
      }
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(GenerateImpersonatedUserToken.class)
        .web(WebApplicationType.NONE)
        .run(args);
  }
}
