package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.*;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.config.FileConfigs;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class BackfillDbFeaturedWorkspaceTable extends Tool {

  private static final Logger log =
      Logger.getLogger(BackfillDbFeaturedWorkspaceTable.class.getName());

  private static final Option RW_PROJ_OPT =
      Option.builder()
          .longOpt("project")
          .desc("The AoU project (environment) to access.")
          .required()
          .hasArg()
          .build();

  private static final Option DRY_OPT =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made.")
          .hasArg()
          .build();

  private static final Option ENV_SUFFIX =
      Option.builder().longOpt("env").desc("The environment for config file.").hasArg().build();

  private static final Options OPTIONS =
      new Options().addOption(RW_PROJ_OPT).addOption(DRY_OPT).addOption(ENV_SUFFIX);

  @Bean
  public IamCredentialsClient iamCredentialsClient() throws IOException {
    return IamCredentialsClient.create();
  }

  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public BackfillDbFeaturedWorkspaceTable(
      FeaturedWorkspaceDao featuredWorkspaceDao, WorkspaceDao workspaceDao) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.workspaceDao = workspaceDao;
  }

  @Bean
  public CommandLineRunner run() {
    return args -> {
      try {
        CommandLine opts = new DefaultParser().parse(OPTIONS, args);
        String rwEnvOpt = opts.getOptionValue(RW_PROJ_OPT.getLongOpt());
        String dryRunArg = opts.getOptionValue(DRY_OPT.getLongOpt());
        String envSuffix = opts.getOptionValue(ENV_SUFFIX.getLongOpt());

        boolean dryRun = dryRunArg != null && dryRunArg.equalsIgnoreCase("true");
        log.info(String.format("Found %s project", rwEnvOpt));
        log.info(String.format("Dry run: %s", dryRun));

        String featuredConfigFileName = "config/featured_workspaces_" + envSuffix + ".json";
        log.info(String.format("Reading file: %s", featuredConfigFileName));

        JsonNode newJson =
            FileConfigs.loadConfig(featuredConfigFileName, FeaturedWorkspacesConfig.class);
        ObjectMapper mapper = new ObjectMapper();
        FeaturedWorkspacesConfig config =
            mapper.convertValue(newJson, FeaturedWorkspacesConfig.class);

        log.info(
            "Number of featured worksapce for the environment: "
                + config.featuredWorkspaces.size());

        for (var featuredWorkspace : config.featuredWorkspaces) {
          List<DbWorkspace> workspaceList =
              workspaceDao.findAllByWorkspaceNamespace(featuredWorkspace.getNamespace());
          // We are assuming there is just one workspace for a given namespace
          DbWorkspace publishedWorkspace = workspaceList.get(0);

          if (publishedWorkspace.getPublished()) {
            log.info(
                String.format(
                    "Read featured workspaces %s from db size %d",
                    publishedWorkspace.getWorkspaceNamespace(), workspaceList.size()));
            DbFeaturedWorkspace dbFeaturedWorkspace =
                new DbFeaturedWorkspace()
                    .setWorkspace(publishedWorkspace)
                    .setCategory(
                        DbFeaturedCategory.valueOf(featuredWorkspace.getCategory().toString()));
            featuredWorkspaceDao.save(dbFeaturedWorkspace);
          } else {
            log.info(
                String.format(
                    "Workspace %s is not published", publishedWorkspace.getFirecloudName()));
          }
        }
      } catch (Exception e) {
        log.severe("Error reading featured workspaces from file: " + e.getMessage());
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(BackfillDbFeaturedWorkspaceTable.class, args);
  }
}
