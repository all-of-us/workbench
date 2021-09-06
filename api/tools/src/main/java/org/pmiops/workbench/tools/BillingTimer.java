package org.pmiops.workbench.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

// Records the timing of Billing Project creation and assignment, including Terra endpoints
public class BillingTimer {

  private void timeBillingProjects(boolean dryRun, int count) {
    System.out.println("dry run = " + dryRun);
    System.out.println("count = " + count);
  }

  @Bean
  public CommandLineRunner run() {
    final Option dryRunOpt =
        Option.builder()
            .longOpt("dry-run")
            .desc("If specified, the tool runs in dry run mode; no projects are created.")
            .hasArg()
            .build();
    final Option projectCount =
        Option.builder()
            .longOpt("count")
            .desc("Number of projects to create (default 1)")
            .hasArg()
            .build();
    final Options options = new Options().addOption(projectCount).addOption(dryRunOpt);

    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(options, args);

      // if missing, default to true
      final boolean dryRun =
          !opts.hasOption(dryRunOpt.getLongOpt())
              || Boolean.parseBoolean(opts.getOptionValue(dryRunOpt.getLongOpt()));
      final int count =
          opts.hasOption(projectCount.getLongOpt())
              ? Integer.parseInt(opts.getOptionValue(projectCount.getLongOpt()))
              : 1;

      this.timeBillingProjects(dryRun, count);
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(BillingTimer.class, args);
  }
}
