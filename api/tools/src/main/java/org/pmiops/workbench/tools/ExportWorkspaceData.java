package org.pmiops.workbench.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.appengine.AppEngineMetadataSpringConfiguration;
import org.pmiops.workbench.audit.ActionAuditSpringConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FirecloudRetryHandler;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceImpl;
import org.pmiops.workbench.monitoring.MonitoringServiceImpl;
import org.pmiops.workbench.monitoring.MonitoringSpringConfiguration;
import org.pmiops.workbench.monitoring.StackdriverStatsExporterService;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceFakeImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** A tool that will generate a CSV export of our workspace data */
@Configuration
@Import({
  AccessTierServiceImpl.class,
  ActionAuditSpringConfiguration.class,
  AppEngineMetadataSpringConfiguration.class,
  LogsBasedMetricServiceImpl.class,
  MonitoringServiceImpl.class,
  MonitoringSpringConfiguration.class,
  NotebooksServiceImpl.class,
  WorkspaceAuthService.class,
  StackdriverStatsExporterService.class,
  UserRecentResourceServiceImpl.class
})
@ComponentScan(
    value = "org.pmiops.workbench",
    excludeFilters =
        // The base CommandlineToolConfig also imports the retry handler, which causes conflicts.
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FirecloudRetryHandler.class))
public class ExportWorkspaceData {

  private static final Logger log = Logger.getLogger(ExportWorkspaceData.class.getName());

  private static Option exportFilenameOpt =
      Option.builder()
          .longOpt("export-filename")
          .desc("Filename of export")
          .required()
          .hasArg()
          .build();
  private static Option includeDemographicsOpt =
      Option.builder()
          .longOpt("include-demographics")
          .desc("Whether to include researcher demographics in the export (sensitive)")
          .build();

  private static Options options =
      new Options().addOption(exportFilenameOpt).addOption(includeDemographicsOpt);

  // Short circuit the DI wiring here with a "mock" WorkspaceService
  // Importing the real one requires importing a large subtree of dependencies
  @Bean
  public WorkspaceService workspaceService() {
    return new WorkspaceServiceFakeImpl();
  }

  @Bean
  ServiceAccountAPIClientFactory serviceAccountAPIClientFactory(WorkbenchConfig config)
      throws IOException {
    return new ServiceAccountAPIClientFactory(config.firecloud.baseUrl);
  }

  @Bean
  @Primary
  @Qualifier(FireCloudConfig.END_USER_WORKSPACE_API)
  WorkspacesApi workspaceApi(ServiceAccountAPIClientFactory factory) throws IOException {
    return factory.workspacesApi();
  }

  private WorkspaceDao workspaceDao;
  private CohortDao cohortDao;
  private ConceptSetDao conceptSetDao;
  private DataSetDao dataSetDao;
  private NotebooksService notebooksService;
  private WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private UserDao userDao;
  private WorkspacesApi workspacesApi;
  private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private AccessTierService accessTierService;
  private SimpleDateFormat dateFormat;

  @Bean
  public CommandLineRunner run(
      @Qualifier("entityManagerFactory") EntityManagerFactory emf,
      WorkspaceDao workspaceDao,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      NotebooksService notebooksService,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      UserDao userDao,
      WorkspacesApi workspacesApi,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      AccessTierService accessTierService) {
    this.workspaceDao = workspaceDao;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.notebooksService = notebooksService;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.userDao = userDao;
    this.workspacesApi = workspacesApi;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.accessTierService = accessTierService;

    this.dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    // Just pick a consistent Time Zone. Most of the reporting is handled in Central Time, so we
    // just use that here.
    this.dateFormat.setTimeZone(TimeZone.getTimeZone("CST"));

    return (args) -> {
      // Binding the EntityManager allows us to use lazy lookups. This simulates what is done on
      // the server, where an entity manager is bound for the duration of a request.
      EntityManagerHolder emHolder = new EntityManagerHolder(emf.createEntityManager());
      TransactionSynchronizationManager.bindResource(emf, emHolder);

      CommandLine opts = new DefaultParser().parse(options, args);
      boolean includeDemographics = opts.hasOption(includeDemographicsOpt.getLongOpt());

      log.info("collecting all users");
      List<WorkspaceExportRow> rows = new ArrayList<>();
      Set<DbUser> usersWithoutWorkspaces =
          Streams.stream(userDao.findAll()).collect(Collectors.toSet());

      log.info("collecting / converting all workspaces");
      for (DbWorkspace workspace : this.workspaceDao.findAll()) {
        rows.add(toWorkspaceExportRow(workspace, includeDemographics));
        usersWithoutWorkspaces.remove(workspace.getCreator());

        if (rows.size() % 10 == 0) {
          log.info("Processed " + rows.size() + "/" + this.workspaceDao.count() + " rows");
        }
      }

      log.info("converting users without workspaces");
      for (DbUser user : usersWithoutWorkspaces) {
        rows.add(toWorkspaceExportRow(user, includeDemographics));
      }

      final CustomMappingStrategy mappingStrategy = new CustomMappingStrategy();
      mappingStrategy.setType(WorkspaceExportRow.class);

      log.info("writing the output CSV");
      try (FileWriter writer =
          new FileWriter(opts.getOptionValue(exportFilenameOpt.getLongOpt()))) {
        new StatefulBeanToCsvBuilder(writer)
            .withMappingStrategy(mappingStrategy)
            .build()
            .write(rows);
      }
    };
  }

  private WorkspaceExportRow toWorkspaceExportRow(
      DbWorkspace workspace, boolean includeDemographics) {
    WorkspaceExportRow row = toWorkspaceExportRow(workspace.getCreator(), includeDemographics);

    row.setProjectId(workspace.getWorkspaceNamespace());
    row.setWorkspaceName(workspace.getName());
    row.setCreatedDate(dateFormat.format(workspace.getCreationTime()));

    try {
      row.setCollaborators(
          FirecloudTransforms.extractAclResponse(
                  workspacesApi.getWorkspaceAcl(
                      workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
              .entrySet().stream()
              .map(entry -> entry.getKey() + " (" + entry.getValue().getAccessLevel() + ")")
              .collect(Collectors.joining("\n")));
    } catch (ApiException e) {
      row.setCollaborators("Error: Not Found");
    }

    Collection<DbCohort> cohorts = cohortDao.findByWorkspaceId(workspace.getWorkspaceId());
    row.setCohortNames(cohorts.stream().map(DbCohort::getName).collect(Collectors.joining(",\n")));
    row.setCohortCount(String.valueOf(cohorts.size()));

    Collection<DbConceptSet> conceptSets =
        conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
    row.setConceptSetNames(
        conceptSets.stream().map(DbConceptSet::getName).collect(Collectors.joining(",\n")));
    row.setConceptSetCount(String.valueOf(conceptSets.size()));

    Collection<DbDataset> datasets = dataSetDao.findByWorkspaceId(workspace.getWorkspaceId());
    row.setDatasetNames(
        datasets.stream().map(DbDataset::getName).collect(Collectors.joining(",\n")));
    row.setDatasetCount(String.valueOf(datasets.size()));

    try {
      Collection<FileDetail> notebooks =
          notebooksService.getNotebooks(
              workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
      row.setNotebookNames(
          notebooks.stream().map(FileDetail::getName).collect(Collectors.joining(",\n")));
      row.setNotebooksCount(String.valueOf(notebooks.size()));
    } catch (NotFoundException e) {
      row.setNotebookNames("Error: Not Found");
      row.setNotebooksCount("N/A");
    }

    DbWorkspaceFreeTierUsage usage = workspaceFreeTierUsageDao.findOneByWorkspace(workspace);
    row.setWorkspaceSpending(usage == null ? "0" : String.valueOf(usage.getCost()));

    row.setReviewForStigmatizingResearch(toYesNo(workspace.getReviewRequested()));
    row.setWorkspaceLastUpdatedDate(dateFormat.format(workspace.getLastModifiedTime()));
    row.setActive(toYesNo(workspace.isActive()));
    return row;
  }

  private WorkspaceExportRow toWorkspaceExportRow(DbUser user, boolean includeDemographics) {
    Optional<DbVerifiedInstitutionalAffiliation> verifiedAffiliation =
        verifiedInstitutionalAffiliationDao.findFirstByUser(user);

    WorkspaceExportRow row = new WorkspaceExportRow();
    row.setCreatorContactEmail(user.getContactEmail());
    row.setCreatorUsername(user.getUsername());
    row.setName(formatName(user));
    row.setInstitution(
        verifiedAffiliation.map(via -> via.getInstitution().getDisplayName()).orElse(""));
    row.setInstitutionalRole(
        verifiedAffiliation.map(via -> via.getInstitutionalRoleEnum().toString()).orElse(""));
    row.setCreatorFirstSignIn(
        Optional.ofNullable(user.getFirstSignInTime()).map(dateFormat::format).orElse(""));
    row.setTwoFactorAuthCompletionDate(
        Optional.ofNullable(user.getTwoFactorAuthCompletionTime())
            .map(dateFormat::format)
            .orElse(""));
    row.setEraCompletionDate(
        Optional.ofNullable(user.getEraCommonsCompletionTime()).map(dateFormat::format).orElse(""));
    row.setTrainingCompletionDate(
        Optional.ofNullable(user.getComplianceTrainingCompletionTime())
            .map(dateFormat::format)
            .orElse(""));
    row.setDuccCompletionDate(
        Optional.ofNullable(user.getDataUseAgreementCompletionTime())
            .map(dateFormat::format)
            .orElse(""));

    // TODO if we revive this tool:
    // update to use Access Tiers instead of Data Access Level
    // we can use this kluge in the meantime
    // (migrated from AccessTierService.temporaryDataAccessLevelKluge)
    final String accessTierShortNames =
        String.join(",", accessTierService.getAccessTierShortNamesForUser(user));
    if (accessTierShortNames != null
        && accessTierShortNames.contains(AccessTierService.REGISTERED_TIER_SHORT_NAME)) {
      row.setCreatorRegistrationState(AccessTierService.REGISTERED_TIER_SHORT_NAME);
    } else {
      row.setCreatorRegistrationState("unregistered");
    }

    row.setDegrees(
        Optional.ofNullable(user.getDegreesEnum()).orElse(ImmutableList.of()).stream()
            .map(Degree::toString)
            .collect(Collectors.joining(",\n")));
    DbDemographicSurvey demo = user.getDemographicSurvey();
    if (includeDemographics && demo != null) {
      row.setRace(
          Optional.ofNullable(demo.getRaceEnum()).orElse(ImmutableList.of()).stream()
              .map(Race::toString)
              .collect(Collectors.joining(",\n")));
      row.setEthnicity(
          Optional.ofNullable(demo.getEthnicityEnum()).map(Ethnicity::toString).orElse(""));
      row.setGenderIdentity(
          Optional.ofNullable(demo.getGenderIdentityEnumList()).orElse(ImmutableList.of()).stream()
              .map(GenderIdentity::toString)
              .collect(Collectors.joining(",\n")));
      row.setIdentifyAsLgbtq(
          Optional.ofNullable(demo.getIdentifiesAsLgbtq()).map(this::toYesNo).orElse(""));
      row.setLgbtqIdentity(Optional.ofNullable(demo.getLgbtqIdentity()).orElse(""));
      row.setSexAtBirth(
          Optional.ofNullable(demo.getSexAtBirthEnum()).orElse(ImmutableList.of()).stream()
              .map(SexAtBirth::toString)
              .collect(Collectors.joining(",\n")));
      row.setYearOfBirth(
          Optional.ofNullable(demo.getYear_of_birth())
              .filter(i -> i > 0)
              .map(i -> Integer.toString(i))
              .orElse(""));
      row.setDisability(
          Optional.ofNullable(demo.getDisabilityEnum()).map(Disability::toString).orElse(""));
      row.setHighestEducation(
          Optional.ofNullable(demo.getEducationEnum()).map(Education::toString).orElse(""));
    }

    return row;
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(ExportWorkspaceData.class, args);
  }

  private String toYesNo(boolean b) {
    return b ? "Yes" : "No";
  }

  private String formatName(DbUser user) {
    return Optional.ofNullable(user.getFamilyName()).orElse("")
        + ", "
        + Optional.ofNullable(user.getGivenName()).orElse("");
  }
}

class CustomMappingStrategy<T> extends ColumnPositionMappingStrategy<T> {
  @Override
  public String[] generateHeader(T bean) {
    super.setColumnMapping(new String[FieldUtils.getAllFields(bean.getClass()).length]);

    final int numColumns = findMaxFieldIndex();

    String[] header = new String[numColumns + 1];

    BeanField<T> beanField;
    for (int i = 0; i <= numColumns; i++) {
      beanField = findField(i);
      String columnHeaderName = extractHeaderName(beanField);
      header[i] = columnHeaderName;
    }
    return header;
  }

  private String extractHeaderName(final BeanField<T> beanField) {
    return beanField.getField().getDeclaredAnnotationsByType(CsvBindByName.class)[0].column();
  }
}
