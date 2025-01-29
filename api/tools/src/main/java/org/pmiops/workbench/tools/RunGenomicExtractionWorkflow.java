package org.pmiops.workbench.tools;

import com.google.api.services.oauth2.model.Userinfo;
import jakarta.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.cdr.CdrVersionMapperImpl;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.config.BigQueryConfig;
import org.pmiops.workbench.dataset.GenomicDatasetServiceImpl;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudCacheConfig;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.genomics.GenomicExtractionMapperImpl;
import org.pmiops.workbench.genomics.GenomicExtractionService;
import org.pmiops.workbench.google.GoogleConfig;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.rawls.RawlsApiClientFactory;
import org.pmiops.workbench.rawls.RawlsConfig;
import org.pmiops.workbench.sam.SamApiClientFactory;
import org.pmiops.workbench.sam.SamConfig;
import org.pmiops.workbench.sam.SamRetryHandler;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@Import({
  AccessTierServiceImpl.class,
  BigQueryConfig.class, // injects com.google.cloud.bigquery.BigQuery
  BigQueryService.class,
  CdrVersionMapperImpl.class,
  CdrVersionService.class,
  CohortMapperImpl.class,
  CohortQueryBuilder.class,
  CohortService.class,
  CommonMappers.class,
  FireCloudCacheConfig.class,
  FireCloudConfig.class,
  FireCloudServiceImpl.class,
  FirecloudApiClientFactory.class,
  FirecloudMapperImpl.class,
  GenomicDatasetServiceImpl.class,
  GenomicExtractionMapperImpl.class,
  GenomicExtractionService.class,
  GoogleConfig.class, // injects com.google.cloud.iam.credentials.v1.IamCredentialsClient
  JiraService.class,
  RawlsApiClientFactory.class,
  RawlsConfig.class,
  SamApiClientFactory.class,
  SamConfig.class,
  SamRetryHandler.class,
  WorkspaceAuthService.class,
})
@Configuration
public class RunGenomicExtractionWorkflow extends Tool {
  private static final Logger log = Logger.getLogger(RunGenomicExtractionWorkflow.class.getName());

  private static DbUser dbUser;

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  static DbUser user() {
    // initialized below, from workspace creator
    return dbUser;
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  static UserAuthentication userAuthentication(
      Provider<DbUser> userProvider, FirecloudApiClientFactory factory) throws IOException {

    DbUser user = userProvider.get();
    Userinfo info = new Userinfo(); // unclear if this is used, so leaving empty
    String impersonatedBearerToken =
        factory.getDelegatedUserCredentials(user.getUsername()).getAccessToken().getTokenValue();

    return new UserAuthentication(
        user, info, impersonatedBearerToken, UserAuthentication.UserType.RESEARCHER);
  }

  private static final Option workspaceNamespaceOpt =
      Option.builder()
          .longOpt("namespace")
          .desc("The workspace namespace to run the extraction from")
          .required()
          .hasArg()
          .build();
  private static final Option datasetOpt =
      Option.builder()
          .longOpt("dataset_id")
          .desc("The dataset ID to record in the DB (but not actually use) for the extraction")
          .required()
          .hasArg()
          .build();
  private static final Option personIdsOpt =
      Option.builder()
          .longOpt("person_id_file")
          .desc("The file of person IDs to use in the extraction.  skips header row.")
          .required()
          .hasArg()
          .build();
  private static final Option legacyOpt =
      Option.builder()
          .longOpt("legacy")
          .desc("Use legacy (v7) workflow (true/false)")
          .required()
          .hasArg()
          .build();
  private static final Option filterSetOpt =
      Option.builder().longOpt("filter_set").desc("Filter set name").required().hasArg().build();
  private static final Option bqProjOpt =
      Option.builder()
          .longOpt("cdr_bq_project")
          .desc("The CDR's BigQuery project")
          .required()
          .hasArg()
          .build();
  private static final Option wgsDatasetOpt =
      Option.builder()
          .longOpt("wgs_bq_dataset")
          .desc("The CDR's WGS BigQuery dataset")
          .required()
          .hasArg()
          .build();

  private static final Options options =
      new Options()
          .addOption(workspaceNamespaceOpt)
          .addOption(datasetOpt)
          .addOption(personIdsOpt)
          .addOption(legacyOpt)
          .addOption(filterSetOpt)
          .addOption(bqProjOpt)
          .addOption(wgsDatasetOpt);

  private static void extract(
      String[] args,
      GenomicExtractionService service,
      CdrVersionService cdrVersionService,
      DataSetDao dataSetDao,
      WorkspaceDao workspaceDao)
      throws ParseException, ApiException, IOException {
    CommandLine opts = new DefaultParser().parse(options, args);

    String namespace = opts.getOptionValue(workspaceNamespaceOpt.getLongOpt());
    DbWorkspace workspace =
        workspaceDao
            .getByNamespace(namespace)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format("Workspace namespace %s not found", namespace)));
    dbUser = workspace.getCreator();

    // We need to set this to build the dataset.
    // Therefore, the Workspace still needs to have a valid CDR in the Controlled Tier.
    cdrVersionService.setCdrVersion(workspace.getCdrVersion());

    long datasetId = Long.parseLong(opts.getOptionValue(datasetOpt.getLongOpt()));
    DbDataset dataSet =
        dataSetDao
            .findByDataSetIdAndWorkspaceId(datasetId, workspace.getWorkspaceId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format(
                            "Dataset %d not found in workspace %s", datasetId, namespace)));

    // read file line by line, skipping header row
    final List<String> personIds;
    try (final var idStream =
        Files.lines(Paths.get(opts.getOptionValue(personIdsOpt.getLongOpt())))) {
      personIds = idStream.skip(1).map(String::trim).toList();
    }

    boolean useLegacyWorkflow =
        Boolean.parseBoolean(opts.getOptionValue(legacyOpt.getLongOpt(), "false"));
    String filterSetName = opts.getOptionValue(filterSetOpt.getLongOpt());
    String bigQueryProject = opts.getOptionValue(bqProjOpt.getLongOpt());
    String wgsBigQueryDataset = opts.getOptionValue(wgsDatasetOpt.getLongOpt());

    service.submitGenomicExtractionJob(
        workspace,
        dataSet,
        personIds,
        useLegacyWorkflow,
        filterSetName,
        bigQueryProject,
        wgsBigQueryDataset);
  }

  @Bean
  public CommandLineRunner run(
      GenomicExtractionService service,
      CdrVersionService cdrVersionService,
      DataSetDao dataSetDao,
      WorkspaceDao workspaceDao) {
    return args -> {
      // project.rb swallows exceptions, so we need to catch and log them here
      try {
        extract(args, service, cdrVersionService, dataSetDao, workspaceDao);
      } catch (Exception e) {
        log.severe("Error: " + e.getMessage());
        e.printStackTrace();
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(RunGenomicExtractionWorkflow.class, args);
  }
}
