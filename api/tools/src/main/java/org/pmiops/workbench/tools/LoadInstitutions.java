package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Optional;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Configuration
@Import(CommandLineToolConfig.class)
@ComponentScan(value = "org.pmiops.workbench.institution")
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
  public CommandLineRunner run(
      InstitutionService institutionService,
      @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());

      // This is mirrors what is done on the API server via "open-session-in-view":
      // https://github.com/spring-projects/spring-framework/blob/master/spring-orm/src/main/java/org/springframework/orm/jpa/support/OpenEntityManagerInViewInterceptor.java#L88-L90
      EntityManagerHolder emHolder = new EntityManagerHolder(emf.createEntityManager());
      TransactionSynchronizationManager.bindResource(emf, emHolder);

      try (BufferedReader reader =
          new BufferedReader(new FileReader(opts.getOptionValue(importFilename.getLongOpt())))) {
        ObjectMapper mapper = new ObjectMapper();
        Institution[] institutions = mapper.readValue(reader, Institution[].class);

        for (Institution institution : institutions) {
          Optional<Institution> fetchedInstitutionMaybe =
              institutionService.getInstitution(institution.getShortName());
          if (fetchedInstitutionMaybe.isPresent()) {
            if (fetchedInstitutionMaybe.get().equals(institution)) {
              log.info("Skipping... Entry already exists for " + institution.getShortName());
            } else {
              if (!dryRun) {
                System.out.println("institution");
                System.out.println(institution);

                System.out.println("fetchedInstitutionMaybe.get()");
                System.out.println(fetchedInstitutionMaybe.get());

                institutionService.updateInstitution(institution.getShortName(), institution);
              }
              dryLog(dryRun, "Updated " + institution.toString());
            }

            continue;
          }

          if (!dryRun) {
            institutionService.createInstitution(institution);
          }
          dryLog(dryRun, "Saved " + institution.toString());
        }
      }
      TransactionSynchronizationManager.unbindResource(emf);
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
    new SpringApplicationBuilder(LoadInstitutions.class).web(false).run(args);
  }
}
