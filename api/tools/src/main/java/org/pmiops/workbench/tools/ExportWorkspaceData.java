package org.pmiops.workbench.tools;

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
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.monitoring.MonitoringServiceImpl;
import org.pmiops.workbench.monitoring.MonitoringSpringConfiguration;
import org.pmiops.workbench.monitoring.StackdriverStatsExporterService;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/** A tool that will generate a CSV export of our workspace data */
@Configuration
@Import({
  MonitoringServiceImpl.class,
  MonitoringSpringConfiguration.class,
  NotebooksServiceImpl.class,
  StackdriverStatsExporterService.class,
  UserRecentResourceServiceImpl.class
})
@ComponentScan("org.pmiops.workbench.firecloud")
public class ExportWorkspaceData {

  private static final Logger log = Logger.getLogger(ExportWorkspaceData.class.getName());

  private static Option exportFilenameOpt =
      Option.builder()
          .longOpt("export-filename")
          .desc("Filename of export")
          .required()
          .hasArg()
          .build();

  private static Options options = new Options().addOption(exportFilenameOpt);

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

  // Short circuit the DI wiring here with a "mock" WorkspaceService
  // Importing the real one requires importing a large subtree of dependencies
  @Bean
  public WorkspaceService workspaceService() {
    return new WorkspaceServiceImpl(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null);
  }

  @Bean
  ServiceAccountAPIClientFactory serviceAccountAPIClientFactory(WorkbenchConfig config) {
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

  @Bean
  public CommandLineRunner run(
      WorkspaceDao workspaceDao,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      NotebooksService notebooksService,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      UserDao userDao,
      WorkspacesApi workspacesApi) {
    this.workspaceDao = workspaceDao;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.notebooksService = notebooksService;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.userDao = userDao;
    this.workspacesApi = workspacesApi;

    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      List<WorkspaceExportRow> rows = new ArrayList<>();
      Set<DbUser> usersWithoutWorkspaces =
          Streams.stream(userDao.findAll()).collect(Collectors.toSet());

      for (DbWorkspace workspace : this.workspaceDao.findAll()) {
        rows.add(toWorkspaceExportRow(workspace));
        usersWithoutWorkspaces.remove(workspace.getCreator());

        if (rows.size() % 10 == 0) {
          log.info("Processed " + rows.size() + "/" + this.workspaceDao.count() + " rows");
        }
      }

      for (DbUser user : usersWithoutWorkspaces) {
        rows.add(toWorkspaceExportRow(user));
      }

      final CustomMappingStrategy mappingStrategy = new CustomMappingStrategy();
      mappingStrategy.setType(WorkspaceExportRow.class);

      try (FileWriter writer =
          new FileWriter(opts.getOptionValue(exportFilenameOpt.getLongOpt()))) {
        new StatefulBeanToCsvBuilder(writer)
            .withMappingStrategy(mappingStrategy)
            .build()
            .write(rows);
      }
    };
  }

  private WorkspaceExportRow toWorkspaceExportRow(DbWorkspace workspace) {
    WorkspaceExportRow row = toWorkspaceExportRow(workspace.getCreator());

    row.setProjectId(workspace.getWorkspaceNamespace());
    row.setName(workspace.getName());
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

  private WorkspaceExportRow toWorkspaceExportRow(DbUser user) {
    WorkspaceExportRow row = new WorkspaceExportRow();
    row.setCreatorContactEmail(user.getContactEmail());
    row.setCreatorUsername(user.getUsername());
    row.setCreatorFirstSignIn(
        user.getFirstSignInTime() == null ? "" : dateFormat.format(user.getFirstSignInTime()));
    row.setCreatorRegistrationState(user.getDataAccessLevelEnum().toString());

    return row;
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(ExportWorkspaceData.class, args);
  }

  private String toYesNo(boolean b) {
    return b ? "Yes" : "No";
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
