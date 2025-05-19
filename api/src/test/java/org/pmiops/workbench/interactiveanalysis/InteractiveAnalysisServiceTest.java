package org.pmiops.workbench.interactiveanalysis;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService.JUPYTER_DELOC_PATTERN;
import static org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService.RSTUDIO_DELOC_PATTERN;
import static org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService.SAS_DELOC_PATTERN;
import static org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService.aouConfigDataUri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class InteractiveAnalysisServiceTest {
  @TestConfiguration
  @Import({
    InteractiveAnalysisService.class,
  })
  @MockBean({
    UserRecentResourceService.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return user;
    }
  }

  private static final WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
  private static DbUser user;
  private String aouConfigDataUri;

  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String GOOGLE_PROJECT_ID = "aou-gcp-id";
  private static final String CDR_BIGQUERY_PROJECT = "cdr-project-id";
  private static final String CDR_BIGQUERY_DATASET = "cdr-dataset-id";
  private static final String FIRECLOUD_WS_NAME = "fc-workspace";
  private static final String BUCKET_NAME = "bucket-name";
  private static final String NOTEBOOK_DIR = "gs://" + BUCKET_NAME + "/notebooks";
  private static final String APP_NAME = "app-name";
  @MockBean WorkspaceService mockWorkspaceService;
  @MockBean LeonardoApiClient mockLeonardoApiClient;
  @MockBean FireCloudService mockFireCloudService;
  @MockBean NotebooksService mockNotebooksService;

  @Autowired InteractiveAnalysisService interactiveAnalysisService;

  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final long LOGGED_IN_USER_ID = 123L;

  @BeforeEach
  public void setUp() {
    config.server.apiBaseUrl = "https://aou.org";
    user = new DbUser().setUsername(LOGGED_IN_USER_EMAIL).setUserId(LOGGED_IN_USER_ID);
    DbCdrVersion dbCdrVersion =
        new DbCdrVersion()
            .setBigqueryProject(CDR_BIGQUERY_PROJECT)
            .setBigqueryDataset(CDR_BIGQUERY_DATASET);
    DbWorkspace dbWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setCdrVersion(dbCdrVersion)
            .setFirecloudName(FIRECLOUD_WS_NAME);
    when(mockWorkspaceService.lookupWorkspaceByNamespace(WORKSPACE_NS)).thenReturn(dbWorkspace);
    RawlsWorkspaceDetails rawlsWorkspaceDetails =
        new RawlsWorkspaceDetails().bucketName(BUCKET_NAME).name(FIRECLOUD_WS_NAME);
    when(mockFireCloudService.getWorkspace(WORKSPACE_NS, FIRECLOUD_WS_NAME))
        .thenReturn(new RawlsWorkspaceResponse().workspace(rawlsWorkspaceDetails));

    aouConfigDataUri =
        aouConfigDataUri(rawlsWorkspaceDetails, dbCdrVersion, WORKSPACE_NS, "https://aou.org");
  }

  @Test
  public void testLocalize_jupyter_editMode() {
    String editDir = "workspaces/" + FIRECLOUD_WS_NAME;
    List<String> notebookLists = List.of("foo.ipynb");
    StorageLink expectedStorageLink =
        new StorageLink()
            .cloudStorageDirectory(NOTEBOOK_DIR)
            .localBaseDirectory(editDir)
            .pattern(JUPYTER_DELOC_PATTERN);
    Map<String, String> expectedLocalizeMap = new HashMap<>();
    expectedLocalizeMap.put(editDir + "/.all_of_us_config.json", aouConfigDataUri);
    expectedLocalizeMap.put(editDir + "/foo.ipynb", NOTEBOOK_DIR + "/foo.ipynb");

    AppType appType = null; // Jupyter uses GCE, so it doesn't have a GKE App Type
    interactiveAnalysisService.localize(
        WORKSPACE_NS, APP_NAME, appType, notebookLists, true, false);
    verify(mockLeonardoApiClient)
        .createStorageLinkForRuntime(GOOGLE_PROJECT_ID, APP_NAME, expectedStorageLink);
    verify(mockLeonardoApiClient)
        .localizeForRuntime(GOOGLE_PROJECT_ID, APP_NAME, expectedLocalizeMap);
  }

  @Test
  public void testLocalize_RStudio_editMode() {
    String editDir = "";
    List<String> notebookLists = List.of("foo.Rmd");
    StorageLink expectedStorageLink =
        new StorageLink()
            .cloudStorageDirectory(NOTEBOOK_DIR)
            .localBaseDirectory(editDir)
            .pattern(RSTUDIO_DELOC_PATTERN);
    Map<String, String> expectedLocalizeMap = new HashMap<>();
    expectedLocalizeMap.put(".all_of_us_config.json", aouConfigDataUri);
    expectedLocalizeMap.put("foo.Rmd", NOTEBOOK_DIR + "/foo.Rmd");

    interactiveAnalysisService.localize(
        WORKSPACE_NS, APP_NAME, AppType.RSTUDIO, notebookLists, false, false);
    verify(mockLeonardoApiClient)
        .createStorageLinkForApp(GOOGLE_PROJECT_ID, APP_NAME, expectedStorageLink);
    verify(mockLeonardoApiClient).localizeForApp(GOOGLE_PROJECT_ID, APP_NAME, expectedLocalizeMap);
  }

  @Test
  public void testLocalize_allFiles_rstudio() {
    interactiveAnalysisService.localize(
        WORKSPACE_NS, APP_NAME, AppType.RSTUDIO, new ArrayList<>(), false, true);
    verify(mockNotebooksService)
        .getAllNotebooksByAppType(BUCKET_NAME, WORKSPACE_NS, FIRECLOUD_WS_NAME, AppType.RSTUDIO);
  }

  @Test
  public void testLocalize_allFiles_sas() {
    interactiveAnalysisService.localize(
        WORKSPACE_NS, APP_NAME, AppType.SAS, new ArrayList<>(), false, true);
    verify(mockNotebooksService)
        .getAllNotebooksByAppType(BUCKET_NAME, WORKSPACE_NS, FIRECLOUD_WS_NAME, AppType.SAS);
  }

  @Test
  public void testLocalize_allFiles_jupyter() {
    interactiveAnalysisService.localize(
        WORKSPACE_NS, APP_NAME, null, new ArrayList<>(), true, true);
    verify(mockNotebooksService)
        .getAllJupyterNotebooks(BUCKET_NAME, WORKSPACE_NS, FIRECLOUD_WS_NAME);
  }

  @Test
  public void testLocalize_SAS_editMode() {
    String editDir = "";
    List<String> notebookLists = List.of("foo.sas");
    StorageLink expectedStorageLink =
        new StorageLink()
            .cloudStorageDirectory(NOTEBOOK_DIR)
            .localBaseDirectory(editDir)
            .pattern(SAS_DELOC_PATTERN);
    Map<String, String> expectedLocalizeMap = new HashMap<>();
    expectedLocalizeMap.put(".all_of_us_config.json", aouConfigDataUri);
    expectedLocalizeMap.put("foo.sas", NOTEBOOK_DIR + "/foo.sas");

    interactiveAnalysisService.localize(
        WORKSPACE_NS, APP_NAME, AppType.SAS, notebookLists, false, false);
    verify(mockLeonardoApiClient)
        .createStorageLinkForApp(GOOGLE_PROJECT_ID, APP_NAME, expectedStorageLink);
    verify(mockLeonardoApiClient).localizeForApp(GOOGLE_PROJECT_ID, APP_NAME, expectedLocalizeMap);
  }

  @Test
  public void testLocalize_appType_not_supported() {
    var unsupportedAppType = AppType.CROMWELL;
    assertThrows(
        NotImplementedException.class,
        () ->
            interactiveAnalysisService.localize(
                WORKSPACE_NS, APP_NAME, unsupportedAppType, Collections.emptyList(), false, false));
  }
}
