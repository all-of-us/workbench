package org.pmiops.workbench.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMember;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

      System.out.println(opts.getOptionValue(fcBaseUrlOpt.getLongOpt()));
      System.out.println(opts.getOptionValue(billingAccountOpt.getLongOpt()));
      System.out.println(opts.getOptionValue(billingProjectNameOpt.getLongOpt()));
      System.out.println(opts.getOptionValue(workspaceNameOpt.getLongOpt()));
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(CreateTerraBillingProjectWorkspace.class).web(false).run(args);
  }
}
