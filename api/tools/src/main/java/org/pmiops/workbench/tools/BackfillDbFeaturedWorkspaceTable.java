package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import jakarta.inject.Provider;
import jakarta.persistence.Entity;
import org.apache.commons.cli.*;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.ActionAuditServiceImpl;
import org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditorImpl;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.config.FileConfigs;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.*;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.google.GoogleConfig;
import org.pmiops.workbench.rawls.RawlsApiClientFactory;
import org.pmiops.workbench.rawls.RawlsConfig;
import org.pmiops.workbench.sam.SamRetryHandler;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Import({
        FireCloudConfig.class,
        FireCloudServiceImpl.class,
        GoogleConfig.class, // injects com.google.cloud.iam.credentials.v1.IamCredentialsClient
        RawlsApiClientFactory.class,
        RawlsConfig.class,
        SamRetryHandler.class,
        WorkspaceServiceImpl.class,
        AccessTierServiceImpl.class,
        BillingProjectAuditorImpl.class,
        ActionAuditSpringConfiguration.class,
        ActionAuditServiceImpl.class,
        CohortCloningService.class,
        CohortFactoryImpl.class,
        ConceptSetService.class,
        ConceptBigQueryService.class,
        BigQueryService.class,
        CohortBuilderServiceImpl.class,
        CohortQueryBuilder.class
})
@EntityScan(basePackages = {"org.pmiops.workbench.cdr.model"})
@EnableJpaRepositories(basePackages = {"org.pmiops.workbench.cdr.dao"})
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

  @Bean
  public Logging getCloudLogging() {
    return LoggingOptions.getDefaultInstance().getService();
  }

  @Bean
  public BigQuery bigQuery() {
    return BigQueryOptions.getDefaultInstance().getService();
  }

  @Bean
  public Duration defaultBigQueryTimeout() {
    return Duration.ofMinutes(5);
  }

  @Bean
  public DbCriteriaAttribute dbCriteriaAttribute() {
    return new DbCriteriaAttribute();
  };

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, FeaturedWorkspaceDao featuredWorkspaceDao,
                               FireCloudService fireCloudService){
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
            if (!dryRun) {
              fireCloudService.updateWorkspaceAclForPublishing(
                      publishedWorkspace.getWorkspaceNamespace(),
                      publishedWorkspace.getFirecloudName(),
                      true);

              featuredWorkspaceDao.save(dbFeaturedWorkspace);
            }
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
