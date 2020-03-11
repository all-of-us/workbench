package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.institution.InstitutionMapperImpl;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.InstitutionServiceImpl;
import org.pmiops.workbench.institution.PublicInstitutionDetailsMapperImpl;
import org.pmiops.workbench.model.Institution;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
  InstitutionServiceImpl.class,
  InstitutionMapperImpl.class,
  PublicInstitutionDetailsMapperImpl.class
})
public class LoadInstitutions {

  private static final Logger log = Logger.getLogger(LoadInstitutions.class.getName());

  private static Option importFilename =
      Option.builder()
          .longOpt("import-filename")
          .desc("File containing JSON for institutions to save")
          .required()
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();

  private static Options options = new Options().addOption(importFilename).addOption(dryRunOpt);

  @Bean
  public CommandLineRunner run(InstitutionService institutionService) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());

      try (BufferedReader reader =
          new BufferedReader(new FileReader(opts.getOptionValue(importFilename.getLongOpt())))) {
        ObjectMapper mapper = new ObjectMapper();
        Institution[] institutions = mapper.readValue(reader, Institution[].class);

        for (Institution institution : institutions) {
          if (institutionService.getDbInstitution(institution.getShortName()).isPresent()) {
            log.info("Skipping... Entry already exists for " + institution.getShortName());
            continue;
          }

          if (!dryRun) {
            institutionService.createInstitution(institution);
          }
          dryLog(dryRun, "Saved " + institution.toString());
        }
      }
    };
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
