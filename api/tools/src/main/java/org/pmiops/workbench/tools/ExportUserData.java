package org.pmiops.workbench.tools;

import static org.pmiops.workbench.tools.BackfillBillingProjectUsers.extractAclResponse;

import com.google.appengine.repackaged.com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

/**
 * A tool that takes a Workspace namespace / Firecloud Project ID and returns details for any
 * workspaces found.
 *
 * <p>Details currently include... - Name - Creator Email - Collaborator Emails and Access Levels
 */
@SpringBootApplication
@EnableJpaRepositories({"org.pmiops.workbench.db.dao"})
@EntityScan("org.pmiops.workbench.db.model")
public class ExportUserData {

  private static final Logger log = Logger.getLogger(ExportUserData.class.getName());

  private static Option fcBaseUrlOpt =
      Option.builder()
          .longOpt("fc-base-url")
          .desc("Firecloud API base URL")
          .required()
          .hasArg()
          .build();

  private static Options options = new Options().addOption(fcBaseUrlOpt);

  // Short circuit the DI wiring here with a "mock" WorkspaceService
  // Importing the real one requires importing a large subtree of dependencies
  @Bean
  WorkspaceService workspaceService() {
    return new WorkspaceServiceImpl(null, null, null, null, null, null, null, null, null);
  }

  private static WorkspacesApi workspacesApi;

  @Primary
  @Bean
  @Scope("prototype")
  @Qualifier(FireCloudConfig.END_USER_WORKSPACE_API)
  WorkspacesApi workspaceApi() {
    return workspacesApi;
  }

  private void initializeApis(ServiceAccountAPIClientFactory apiClientFactory) {
    try {
      workspacesApi = apiClientFactory.workspacesApi();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  ServiceAccountAPIClientFactory serviceAccountAPIClientFactory(Provider<WorkbenchConfig> configProvider) {
    return new ServiceAccountAPIClientFactory(configProvider.get().firecloud.baseUrl);
  }

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      NotebooksService notebooksService,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      Provider<WorkspacesApi> workspacesApiProvider,
      ServiceAccountAPIClientFactory apiClientFactory) {
    return (args) -> {
      initializeApis(apiClientFactory);
      WorkspacesApi workspacesApi = workspacesApiProvider.get();

      FileWriter writer = new FileWriter("export.csv");

      List<WorkspaceExportRow> rows = new ArrayList<>();

      int n = 0;
      for (DbWorkspace workspace : workspaceDao.findAll()) {
        WorkspaceExportRow row = new WorkspaceExportRow();
        row.setCreatorContactEmail(workspace.getCreator().getContactEmail());
        row.setCreatorUsername(workspace.getCreator().getEmail());
        row.setCreatorFirstSignIn(workspace.getCreator().getFirstSignInTime().toString());
        row.setCreatorRegistrationState(workspace.getCreator().getDataAccessLevelEnum().toString());

        row.setProjectId(workspace.getWorkspaceNamespace());
        row.setName(workspace.getName());
        row.setCreatedDate(workspace.getCreationTime().toString());

        row.setCollaborators(extractAclResponse(
            workspacesApi.getWorkspaceAcl(
                workspace.getWorkspaceNamespace(),
                workspace.getFirecloudName())
        ).keySet().stream().collect(
            Collectors.joining(",\n")));

        Collection<DbCohort> cohorts = cohortDao.findByWorkspaceId(workspace.getWorkspaceId());
        row.setCohortNames(cohorts.stream().map(cohort -> cohort.getName())
            .collect(Collectors.joining(",\n")));
        row.setCohortCount(String.valueOf(cohorts.size()));

        Collection<DbConceptSet> conceptSets = conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
        row.setConceptSetNames(conceptSets.stream().map(conceptSet -> conceptSet.getName())
            .collect(Collectors.joining(",\n")));
        row.setConceptSetCount(String.valueOf(conceptSets.size()));

        Collection<DbDataset> datasets = dataSetDao.findByWorkspaceId(workspace.getWorkspaceId());
        row.setDatasetNames(datasets.stream().map(dataSet -> dataSet.getName())
            .collect(Collectors.joining(",\n")));
        row.setDatasetCount(String.valueOf(datasets.size()));

        Collection<FileDetail> notebooks = notebooksService.getNotebooks(
            workspace.getWorkspaceNamespace(),
            workspace.getFirecloudName());
        row.setNotebookNames(notebooks.stream().map(notebook -> notebook.getName()).collect(
            Collectors.joining(",\n")));
        row.setNotebooksCount(String.valueOf(notebooks.size()));

        DbWorkspaceFreeTierUsage usage = workspaceFreeTierUsageDao.findOneByWorkspace(workspace);
        row.setWorkspaceSpending(usage == null ? "0" : String.valueOf(usage.getCost()));

        row.setReviewForStigmatizingResearch(toYesNo(workspace.getReviewRequested()));
        row.setWorkspaceLastUpdatedDate(workspace.getLastModifiedTime().toString());
        row.setActive(toYesNo(workspace.isActive()));

        n++;
        if (n == 100) {
          break;
        }

        rows.add(row);
      }

      final CustomMappingStrategy mappingStrategy = new CustomMappingStrategy();
      mappingStrategy.setType(WorkspaceExportRow.class);

      StatefulBeanToCsvBuilder<WorkspaceExportRow> builder =
          new StatefulBeanToCsvBuilder(writer);
      StatefulBeanToCsv beanWriter =
          builder.withMappingStrategy(mappingStrategy).build();

      beanWriter.write(rows);

      writer.close();
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(ExportUserData.class).web(false).run(args);
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
    return beanField.getField()
        .getDeclaredAnnotationsByType(CsvBindByName.class)[0].column();
  }
}