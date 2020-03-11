package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.institution.InstitutionMapperImpl;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.InstitutionServiceImpl;
import org.pmiops.workbench.institution.PublicInstitutionDetailsMapperImpl;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

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
          new BufferedReader(
              new FileReader(opts.getOptionValue(importFilename.getLongOpt())))) {
        ObjectMapper mapper  = new ObjectMapper();
        Institution[] institutions = mapper.readValue(reader, Institution[].class);

        for (Institution institution : institutions) {
          if (!dryRun) {
            if (institutionService.getDbInstitution(institution.getShortName()).isPresent()) {
              log.info("Entry already exisits for " + institution.getShortName());
            } else {
              institutionService.createInstitution(institution);
            }
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
