package org.pmiops.workbench.tools.institutions;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.institution.InstitutionEmailAddressMapperImpl;
import org.pmiops.workbench.institution.InstitutionEmailDomainMapperImpl;
import org.pmiops.workbench.institution.InstitutionMapperImpl;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.InstitutionServiceImpl;
import org.pmiops.workbench.institution.InstitutionTierRequirementMapperImpl;
import org.pmiops.workbench.institution.InstitutionUserInstructionsMapperImpl;
import org.pmiops.workbench.institution.PublicInstitutionDetailsMapperImpl;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.tools.CommandLineToolConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Import({
  AccessTierServiceImpl.class,
  InstitutionServiceImpl.class,
  InstitutionMapperImpl.class,
  InstitutionUserInstructionsMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class,
  InstitutionEmailDomainMapperImpl.class,
  InstitutionEmailAddressMapperImpl.class,
  InstitutionTierRequirementMapperImpl.class,
})
@EnableTransactionManagement
public class LoadInstitutions {

  private static final Logger log = Logger.getLogger(LoadInstitutions.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final Option importFilename =
      Option.builder()
          .longOpt("import-filename")
          .desc("File containing JSON for institutions to save")
          .required()
          .hasArg()
          .build();
  private static final Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();

  private static final Options options =
      new Options().addOption(importFilename).addOption(dryRunOpt);

  @Bean
  public CommandLineRunner run(InstitutionService institutionService) {
    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());

      for (final Institution institution : read(opts.getOptionValue(importFilename.getLongOpt()))) {
        if (!dryRun) {
          institutionService
              .updateInstitution(institution.getShortName(), institution)
              // here I mean "ifNotPresent()"
              .orElseGet(() -> institutionService.createInstitution(institution));
        }
        dryLog(dryRun, "Updated or Created " + institution.toString());
      }
    };
  }

  private static Institution[] read(final String filename) throws IOException {
    try (final BufferedReader reader = new BufferedReader(new FileReader(filename))) {
      return mapper.readValue(reader, Institution[].class);
    }
  }

  private static void dryLog(boolean dryRun, String msg) {
    String prefix = "";
    if (dryRun) {
      prefix = "[DRY RUN] Would have... ";
    }
    log.info(prefix + msg);
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(LoadInstitutions.class, args);
  }
}
