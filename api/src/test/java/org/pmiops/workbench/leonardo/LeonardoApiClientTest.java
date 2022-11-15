package org.pmiops.workbench.leonardo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.leonardo.model.LeonardoCreateAppRequest;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoKubernetesRuntimeConfig;
import org.pmiops.workbench.leonardo.model.LeonardoPersistentDiskRequest;
import org.pmiops.workbench.model.App;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.KubernetesRuntimeConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.fileArtifacts.FileArtifactsRetryHandler;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.backoff.NoBackOffPolicy;

@DataJpaTest
public class LeonardoApiClientTest {
  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    LeonardoMapperImpl.class,
    LeonardoApiClientImpl.class,
    LeonardoRetryHandler.class,
    LeonardoApiClientImpl.class,
    NoBackOffPolicy.class,
    FileArtifactsRetryHandler.class,
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

  @Qualifier(LeonardoConfig.USER_APPS_API)
  @MockBean
  AppsApi userAppsApi;

  @MockBean WorkspaceDao workspaceDao;
  @MockBean LeonardoApiClientFactory mockLeonardoApiClientFactory;
  @MockBean FireCloudService mockFireCloudService;

  @Autowired AccessTierDao accessTierDao;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired UserDao userDao;
  @Autowired LeonardoMapper leonardoMapper;
  @Autowired LeonardoApiClient leonardoApiClient;

  @Captor private ArgumentCaptor<LeonardoCreateAppRequest> createAppRequestArgumentCaptor;

  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String WORKSPACE_ID = "myfirstworkspace";
  private static final String WORKSPACE_NAME = "My First Workspace";
  private static final String WORKSPACE_BUCKET = "workspace bucket";
  private static final String GOOGLE_PROJECT_ID = "aou-gcp-id";
  private static final String MACHINE_TYPE = "n1-standard-1";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String RSTUDIO_DESCRIPTOR_PATH = "rstudio/path";
  private static final String CDR_BUCKET = "gs://cdr-bucket";
  private static final String CDR_STORAGE_BASE_PATH = "v99";
  private static final String WGS_PATH = "wgs/cram/manifest.csv";

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();

  private DbWorkspace testWorkspace;
  private App testApp;
  private CreateAppRequest createAppRequest;
  private LeonardoKubernetesRuntimeConfig leonardoKubernetesRuntimeConfig;
  private LeonardoPersistentDiskRequest leonardoPersistentDiskRequest;
  private PersistentDiskRequest persistentDiskRequest;
  private Map<String, String> appLabels = new HashMap<>();
  private Map<String, String> customEnvironmentVariables = new HashMap<>();

  @BeforeEach
  public void setUp() {
    config = WorkbenchConfig.createEmptyConfig();
    config.app.rStudioDescriptorPath = RSTUDIO_DESCRIPTOR_PATH;

    user = new DbUser().setUsername(LOGGED_IN_USER_EMAIL).setUserId(123L);

    KubernetesRuntimeConfig kubernetesRuntimeConfig =
        new KubernetesRuntimeConfig().autoscalingEnabled(false).machineType(MACHINE_TYPE);
    leonardoKubernetesRuntimeConfig =
        new LeonardoKubernetesRuntimeConfig().autoscalingEnabled(false).machineType(MACHINE_TYPE);
    persistentDiskRequest = new PersistentDiskRequest().diskType(DiskType.STANDARD).size(10);
    leonardoPersistentDiskRequest =
        new LeonardoPersistentDiskRequest().diskType(LeonardoDiskType.STANDARD).size(10);
    testApp =
        new App()
            .appType(AppType.RSTUDIO)
            .googleProject(GOOGLE_PROJECT_ID)
            .kubernetesRuntimeConfig(kubernetesRuntimeConfig);
    createAppRequest =
        new CreateAppRequest()
            .appType(AppType.RSTUDIO)
            .kubernetesRuntimeConfig(kubernetesRuntimeConfig)
            .persistentDiskRequest(persistentDiskRequest);

    DbCdrVersion cdrVersion =
        new DbCdrVersion()
            .setName("1")
            // set the db name to be empty since test cases currently
            // run in the workbench schema only.
            .setCdrDbName("")
            .setBigqueryProject("cdr")
            .setBigqueryDataset("bq")
            .setAccessTier(
                TestMockFactory.createControlledTierForTests(accessTierDao)
                    .setDatasetsBucket(CDR_BUCKET))
            .setStorageBasePath(CDR_STORAGE_BASE_PATH)
            .setWgsCramManifestPath(WGS_PATH);
    testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_NAME)
            .setFirecloudName(WORKSPACE_ID)
            .setCdrVersion(cdrVersion);
    doReturn(testWorkspace).when(workspaceDao).getRequired(WORKSPACE_NS, WORKSPACE_NAME);

    appLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_AOU, "true");
    appLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_CREATED_BY, LOGGED_IN_USER_EMAIL);

    customEnvironmentVariables.put("WORKSPACE_CDR", "cdr.bq");
    customEnvironmentVariables.put("WORKSPACE_NAMESPACE", WORKSPACE_NS);
    customEnvironmentVariables.put("WORKSPACE_BUCKET", "gs://" + WORKSPACE_BUCKET);
    customEnvironmentVariables.put("BIGQUERY_STORAGE_API_ENABLED", "true");
    customEnvironmentVariables.put("CDR_STORAGE_PATH", CDR_BUCKET + "/" + CDR_STORAGE_BASE_PATH);
    customEnvironmentVariables.put(
        "WGS_CRAM_MANIFEST_PATH", CDR_BUCKET + "/" + CDR_STORAGE_BASE_PATH + "/" + WGS_PATH);
  }

  @Test
  public void testCreateAppSuccess_exisingPd() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    leonardoApiClient.createApp(
        createAppRequest.persistentDiskRequest(persistentDiskRequest.name("pd-name")),
        testWorkspace);
    verify(userAppsApi)
        .createApp(
            eq(GOOGLE_PROJECT_ID),
            startsWith(getAppName(AppType.RSTUDIO)),
            createAppRequestArgumentCaptor.capture());

    Map<String, String> diskLabels = new HashMap<>();
    diskLabels.put(
        LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE,
        LeonardoLabelHelper.appTypeToLabelValue(AppType.RSTUDIO));

    LeonardoCreateAppRequest createAppRequest = createAppRequestArgumentCaptor.getValue();
    appLabels.put(
        LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE, AppType.RSTUDIO.toString().toLowerCase());

    LeonardoCreateAppRequest expectedAppRequest =
        new LeonardoCreateAppRequest()
            .appType(LeonardoAppType.CUSTOM)
            .kubernetesRuntimeConfig(leonardoKubernetesRuntimeConfig)
            .descriptorPath(RSTUDIO_DESCRIPTOR_PATH)
            .labels(appLabels)
            .diskConfig(leonardoPersistentDiskRequest.labels(diskLabels).name("pd-name"))
            .customEnvironmentVariables(customEnvironmentVariables);

    assertThat(createAppRequest).isEqualTo(expectedAppRequest);
  }

  @Test
  public void testCreateAppSuccess_newPd() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    leonardoApiClient.createApp(
        createAppRequest.persistentDiskRequest(persistentDiskRequest), testWorkspace);

    verify(userAppsApi)
        .createApp(
            eq(GOOGLE_PROJECT_ID),
            startsWith(getAppName(AppType.RSTUDIO)),
            createAppRequestArgumentCaptor.capture());

    LeonardoCreateAppRequest createAppRequest = createAppRequestArgumentCaptor.getValue();

    assertThat(createAppRequest.getDiskConfig().getName())
        .startsWith("all-of-us-pd-" + user.getUserId() + "-" + "rstudio");
  }

  @Test
  public void testGetAppSuccess() throws Exception {
    leonardoApiClient.getAppByNameByProjectId(GOOGLE_PROJECT_ID, getAppName(AppType.RSTUDIO));
    verify(userAppsApi).getApp(GOOGLE_PROJECT_ID, getAppName(AppType.RSTUDIO));
  }

  @Test
  public void testListAppSuccess() throws Exception {
    leonardoApiClient.listAppsInProject(GOOGLE_PROJECT_ID);
    verify(userAppsApi)
        .listAppByProject(
            GOOGLE_PROJECT_ID, null, false, LeonardoLabelHelper.LEONARDO_APP_LABEL_KEYS);
  }

  @Test
  public void testDeleteAppSuccess() throws Exception {
    String appName = "app-name";
    boolean deleteDisk = true;
    leonardoApiClient.deleteApp(appName, testWorkspace, deleteDisk);
    verify(userAppsApi).deleteApp(GOOGLE_PROJECT_ID, appName, deleteDisk);
  }

  private void stubGetFcWorkspace(WorkspaceAccessLevel accessLevel) {
    FirecloudWorkspaceDetails fcWorkspaceDetail =
        new FirecloudWorkspaceDetails()
            .namespace(WORKSPACE_NS)
            .name(WORKSPACE_NS)
            .createdBy(LOGGED_IN_USER_EMAIL)
            .googleProject(GOOGLE_PROJECT_ID)
            .bucketName(WORKSPACE_BUCKET);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspaceDetail);
    fcResponse.setAccessLevel(accessLevel.toString());
    when(mockFireCloudService.getWorkspace(any())).thenReturn(Optional.of(fcResponse));
    when(mockFireCloudService.getWorkspace(
            fcWorkspaceDetail.getNamespace(), fcWorkspaceDetail.getName()))
        .thenReturn(fcResponse);
  }

  private String getAppName(AppType appType) {
    return "all-of-us-" + user.getUserId() + "-" + appType.toString().toLowerCase();
  }
}
