package org.pmiops.workbench.leonardo;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.leonardo.api.AppsApi;
import org.broadinstitute.dsde.workbench.client.leonardo.api.DisksApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AllowedChartName;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudContext;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudProvider;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.legacy_leonardo_client.api.RuntimesApi;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.CreateAppRequest;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.KubernetesRuntimeConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.backoff.NoBackOffPolicy;

@DataJpaTest
public class LeonardoApiClientTest {
  @TestConfiguration
  @Import({
    CommonMappers.class,
    FakeClockConfiguration.class,
    FirecloudMapperImpl.class,
    LegacyLeonardoRetryHandler.class,
    LeonardoApiClientImpl.class,
    LeonardoApiClientImpl.class,
    LeonardoMapperImpl.class,
    LeonardoRetryHandler.class,
    NoBackOffPolicy.class,
    NotebooksRetryHandler.class,
  })
  @MockBean(LeonardoApiClientFactory.class)
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
  AppsApi mockUserAppsApi;

  @Qualifier(LeonardoConfig.USER_DISKS_API)
  @MockBean
  DisksApi mockUserDisksApi;

  @Qualifier("userRuntimesApi")
  @MockBean
  RuntimesApi userRuntimesApi;

  @MockBean FireCloudService mockFireCloudService;
  @MockBean WorkspaceDao mockWorkspaceDao;

  @SpyBean LeonardoMapper leonardoMapper;

  @Autowired AccessTierDao accessTierDao;
  @Autowired FirecloudMapper firecloudMapper;

  @Autowired LeonardoApiClient leonardoApiClient;

  @Captor
  private ArgumentCaptor<org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest>
      createAppRequestArgumentCaptor;

  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String WORKSPACE_ID = "myfirstworkspace";
  private static final String WORKSPACE_NAME = "My First Workspace";
  private static final String WORKSPACE_BUCKET = "workspace bucket";
  private static final String GOOGLE_PROJECT_ID = "aou-gcp-id";
  private static final String MACHINE_TYPE = "n1-standard-1";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String CDR_BUCKET = "gs://cdr-bucket";
  private static final String CDR_STORAGE_BASE_PATH = "v99";
  private static final String WGS_PATH = "wgs/cram/manifest.csv";
  private static final String LEONARDO_BASE_URL = "http://LeonardoUrl/dummy";
  private static final String RUNTIME_NAME = "runtime-name";

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();

  private DbWorkspace testWorkspace;
  private UserAppEnvironment testApp;
  private CreateAppRequest createAppRequest;
  private org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig
      leonardoKubernetesRuntimeConfig;
  private org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest
      leonardoPersistentDiskRequest;

  private PersistentDiskRequest persistentDiskRequest;
  private final Map<String, String> appLabels = new HashMap<>();
  private final Map<String, String> customEnvironmentVariables = new HashMap<>();

  @BeforeEach
  public void setUp() throws Exception {
    config = WorkbenchConfig.createEmptyConfig();
    config.firecloud.leoBaseUrl = LEONARDO_BASE_URL;
    config.featureFlags.enableGcsFuseOnGke = true;

    user = new DbUser().setUsername(LOGGED_IN_USER_EMAIL).setUserId(123L);

    KubernetesRuntimeConfig kubernetesRuntimeConfig =
        new KubernetesRuntimeConfig().autoscalingEnabled(false).machineType(MACHINE_TYPE);
    leonardoKubernetesRuntimeConfig =
        new org.broadinstitute.dsde.workbench.client.leonardo.model.KubernetesRuntimeConfig()
            .autoscalingEnabled(false)
            .machineType(MACHINE_TYPE);
    persistentDiskRequest = new PersistentDiskRequest().diskType(DiskType.STANDARD).size(10);
    leonardoPersistentDiskRequest =
        new org.broadinstitute.dsde.workbench.client.leonardo.model.PersistentDiskRequest()
            .diskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType.STANDARD)
            .size(10);
    testApp =
        new UserAppEnvironment()
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
            .setAccessTier(accessTierDao.save(createControlledTier()).setDatasetsBucket(CDR_BUCKET))
            .setStorageBasePath(CDR_STORAGE_BASE_PATH)
            .setWgsCramManifestPath(WGS_PATH);
    testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_NAME)
            .setFirecloudName(WORKSPACE_ID)
            .setCdrVersion(cdrVersion);
    doReturn(testWorkspace).when(mockWorkspaceDao).getRequired(WORKSPACE_NS, WORKSPACE_NAME);

    appLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_AOU, "true");
    appLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_CREATED_BY, LOGGED_IN_USER_EMAIL);

    customEnvironmentVariables.put("LEONARDO_BASE_URL", LEONARDO_BASE_URL);
    customEnvironmentVariables.put("WORKSPACE_CDR", "cdr.bq");
    customEnvironmentVariables.put("WORKSPACE_NAMESPACE", WORKSPACE_NS);
    customEnvironmentVariables.put("WORKSPACE_BUCKET", "gs://" + WORKSPACE_BUCKET);
    customEnvironmentVariables.put("BIGQUERY_STORAGE_API_ENABLED", "true");
    customEnvironmentVariables.put("CDR_STORAGE_PATH", CDR_BUCKET + "/" + CDR_STORAGE_BASE_PATH);
    customEnvironmentVariables.put("ARTIFACT_REGISTRY_DOCKER_REPO", null);
    customEnvironmentVariables.put(
        "WGS_CRAM_MANIFEST_PATH", CDR_BUCKET + "/" + CDR_STORAGE_BASE_PATH + "/" + WGS_PATH);
    customEnvironmentVariables.putAll(LeonardoCustomEnvVarUtils.FASTA_REFERENCE_ENV_VAR_MAP);

    when(mockUserDisksApi.listDisksByProject(any(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
  }

  @Test
  public void testCreateAppSuccess_existingPd() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    leonardoApiClient.createApp(
        createAppRequest.persistentDiskRequest(persistentDiskRequest.name("pd-name")),
        testWorkspace);
    verify(mockUserAppsApi)
        .createApp(
            eq(GOOGLE_PROJECT_ID),
            startsWith(getAppName(AppType.RSTUDIO)),
            createAppRequestArgumentCaptor.capture());

    Map<String, String> diskLabels = new HashMap<>();
    diskLabels.put(
        LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE,
        LeonardoLabelHelper.appTypeToLabelValue(AppType.RSTUDIO));
    diskLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAMESPACE, WORKSPACE_NS);
    diskLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAME, WORKSPACE_NAME);

    org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest createAppRequest =
        createAppRequestArgumentCaptor.getValue();
    appLabels.put(
        LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE, AppType.RSTUDIO.toString().toLowerCase());
    appLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAMESPACE, WORKSPACE_NS);
    appLabels.put(LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAME, WORKSPACE_NAME);
    customEnvironmentVariables.put("WORKSPACE_NAME", testWorkspace.getFirecloudName());
    customEnvironmentVariables.put("GOOGLE_PROJECT", testWorkspace.getGoogleProject());
    customEnvironmentVariables.put("OWNER_EMAIL", user.getUsername());
    org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest expectedAppRequest =
        new org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest()
            .appType(org.broadinstitute.dsde.workbench.client.leonardo.model.AppType.ALLOWED)
            .kubernetesRuntimeConfig(leonardoKubernetesRuntimeConfig)
            .allowedChartName(AllowedChartName.RSTUDIO)
            .labels(appLabels)
            .diskConfig(leonardoPersistentDiskRequest.labels(diskLabels).name("pd-name"))
            .bucketNameToMount(WORKSPACE_BUCKET)
            .customEnvironmentVariables(customEnvironmentVariables);

    assertThat(createAppRequest).isEqualTo(expectedAppRequest);
    assertThat(createAppRequest.getAutodeleteEnabled()).isNull();
  }

  @Test
  public void testCreateAppSuccess_autodeleteEnabled() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    leonardoApiClient.createApp(
        createAppRequest
            .persistentDiskRequest(persistentDiskRequest.name("pd-name"))
            .autodeleteEnabled(true)
            .autodeleteThreshold(10),
        testWorkspace);
    verify(mockUserAppsApi)
        .createApp(
            eq(GOOGLE_PROJECT_ID),
            startsWith(getAppName(AppType.RSTUDIO)),
            createAppRequestArgumentCaptor.capture());

    org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest createAppRequest =
        createAppRequestArgumentCaptor.getValue();

    assertThat(createAppRequest.getAutodeleteEnabled()).isTrue();
    assertThat(createAppRequest.getAutodeleteThreshold()).isEqualTo(10);
  }

  @Test
  public void testCreateApp_mountGcsFuseDisabled() throws Exception {
    config.featureFlags.enableGcsFuseOnGke = false;
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    leonardoApiClient.createApp(createAppRequest, testWorkspace);
    verify(mockUserAppsApi)
        .createApp(
            eq(GOOGLE_PROJECT_ID),
            startsWith(getAppName(AppType.RSTUDIO)),
            createAppRequestArgumentCaptor.capture());

    org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest createAppRequest =
        createAppRequestArgumentCaptor.getValue();

    assertThat(createAppRequest.getBucketNameToMount()).isNull();
  }

  @Test
  public void testCreateAppSuccess_autodeleteDisabled() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    leonardoApiClient.createApp(
        createAppRequest
            .persistentDiskRequest(persistentDiskRequest.name("pd-name"))
            .autodeleteEnabled(false)
            .autodeleteThreshold(10),
        testWorkspace);
    verify(mockUserAppsApi)
        .createApp(
            eq(GOOGLE_PROJECT_ID),
            startsWith(getAppName(AppType.RSTUDIO)),
            createAppRequestArgumentCaptor.capture());

    org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest createAppRequest =
        createAppRequestArgumentCaptor.getValue();

    assertThat(createAppRequest.getAutodeleteEnabled()).isFalse();
    assertThat(createAppRequest.getAutodeleteThreshold()).isEqualTo(10);
  }

  @Test
  public void testCreateAppSuccess_newPd() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    leonardoApiClient.createApp(
        createAppRequest.persistentDiskRequest(persistentDiskRequest), testWorkspace);

    verify(mockUserAppsApi)
        .createApp(
            eq(GOOGLE_PROJECT_ID),
            startsWith(getAppName(AppType.RSTUDIO)),
            createAppRequestArgumentCaptor.capture());

    org.broadinstitute.dsde.workbench.client.leonardo.model.CreateAppRequest createAppRequest =
        createAppRequestArgumentCaptor.getValue();

    assertThat(createAppRequest.getDiskConfig().getName())
        .startsWith("all-of-us-pd-" + user.getUserId() + "-" + "rstudio");
  }

  @Test
  public void testCreateAppFail_newPd_pdAlreadyExist() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    Map<String, String> diskLabels = new HashMap<>();
    diskLabels.put(
        LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE,
        LeonardoLabelHelper.appTypeToLabelValue(AppType.RSTUDIO));
    ListPersistentDiskResponse rstudioDisk =
        new ListPersistentDiskResponse()
            .name("123")
            .cloudContext(
                new CloudContext()
                    .cloudProvider(CloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(DiskStatus.READY)
            .labels(diskLabels);
    when(mockUserDisksApi.listDisksByProject(any(), any(), any(), any(), any()))
        .thenReturn(List.of(rstudioDisk));

    assertThrows(
        BadRequestException.class,
        () ->
            leonardoApiClient.createApp(
                createAppRequest.persistentDiskRequest(persistentDiskRequest), testWorkspace));
  }

  @Test
  public void testListAppSuccess() throws Exception {
    leonardoApiClient.listAppsInProjectCreatedByCreator(GOOGLE_PROJECT_ID);
    verify(mockUserAppsApi)
        .listAppByProject(
            GOOGLE_PROJECT_ID, null, false, LeonardoLabelHelper.LEONARDO_APP_LABEL_KEYS, "creator");
  }

  @Test
  public void testDeleteAppSuccess() throws Exception {
    String appName = "app-name";
    boolean deleteDisk = true;
    leonardoApiClient.deleteApp(appName, testWorkspace, deleteDisk);
    verify(mockUserAppsApi).deleteApp(GOOGLE_PROJECT_ID, appName, deleteDisk);
  }

  @Test
  public void testCreateRuntime_withGceConfig_Success() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    // Arrange
    Runtime mockRuntime = new Runtime().googleProject(GOOGLE_PROJECT_ID).runtimeName(RUNTIME_NAME);
    GceConfig gceConfig = new GceConfig().diskSize(120).machineType("n1-standard-4");
    mockRuntime.setGceConfig(gceConfig);

    // Act
    leonardoApiClient.createRuntime(mockRuntime, WORKSPACE_NS, WORKSPACE_NAME);

    // Assert
    verify(userRuntimesApi).createRuntime(eq(GOOGLE_PROJECT_ID), eq(RUNTIME_NAME), any());
  }

  @Test
  public void testCreateRuntime_withGceWithPdConfig_Success() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    // Arrange
    Runtime mockRuntime = new Runtime().googleProject(GOOGLE_PROJECT_ID).runtimeName(RUNTIME_NAME);
    GceWithPdConfig gceWithPdConfig =
        new GceWithPdConfig()
            .persistentDisk(new PersistentDiskRequest().diskType(DiskType.STANDARD).size(120))
            .machineType("n1-standard-4");
    mockRuntime.setGceWithPdConfig(gceWithPdConfig);

    // Act
    leonardoApiClient.createRuntime(mockRuntime, WORKSPACE_NS, WORKSPACE_NAME);

    // Assert
    verify(userRuntimesApi).createRuntime(eq(GOOGLE_PROJECT_ID), eq(RUNTIME_NAME), any());
  }

  @Test
  public void testCreateRuntime_withDataProcConfig_Success() throws Exception {
    stubGetFcWorkspace(WorkspaceAccessLevel.OWNER);
    // Arrange
    Runtime mockRuntime = new Runtime().googleProject(GOOGLE_PROJECT_ID).runtimeName(RUNTIME_NAME);
    DataprocConfig dataprocConfig =
        new DataprocConfig().masterDiskSize(120).masterMachineType("n1-standard-4");
    mockRuntime.setDataprocConfig(dataprocConfig);

    // Act
    leonardoApiClient.createRuntime(mockRuntime, WORKSPACE_NS, WORKSPACE_NAME);

    // Assert
    verify(userRuntimesApi).createRuntime(eq(GOOGLE_PROJECT_ID), eq(RUNTIME_NAME), any());
  }

  @Test
  public void testUpdateRuntimeMapper_GCEConfig() throws Exception {
    // Arrange
    Runtime mockRuntime = new Runtime();
    GceConfig gceConfig = new GceConfig().diskSize(120).machineType("n1-standard-4");
    mockRuntime.setGceConfig(gceConfig);

    // Act
    leonardoApiClient.updateRuntime(mockRuntime);

    // Assert
    verify(leonardoMapper).toUpdateGceConfig(gceConfig);
    verify(userRuntimesApi).updateRuntime(any(), any(), any());
  }

  @Test
  public void testUpdateRuntimeMapper_GCEWithPdConfig() throws Exception {
    // Arrange
    Runtime mockRuntime = new Runtime();
    GceWithPdConfig gceWithPdConfig = new GceWithPdConfig().machineType("n1-standard-4");
    mockRuntime.setGceWithPdConfig(gceWithPdConfig);

    // Act
    leonardoApiClient.updateRuntime(mockRuntime);

    // Assert
    verify(leonardoMapper).toUpdateGceConfig(gceWithPdConfig);
    verify(userRuntimesApi).updateRuntime(any(), any(), any());
  }

  @Test
  public void testUpdateRuntimeMapper_withDataProcConfig() throws Exception {
    // Arrange
    Runtime mockRuntime = new Runtime();
    DataprocConfig dataprocConfig =
        new DataprocConfig().masterDiskSize(120).masterMachineType("n1-standard-4");
    mockRuntime.setDataprocConfig(dataprocConfig);

    // Act
    leonardoApiClient.updateRuntime(mockRuntime);

    // Assert
    verify(leonardoMapper).toUpdateDataprocConfig(dataprocConfig);
    verify(userRuntimesApi).updateRuntime(any(), any(), any());
  }

  private void stubGetFcWorkspace(WorkspaceAccessLevel accessLevel) {
    RawlsWorkspaceDetails fcWorkspaceDetail =
        new RawlsWorkspaceDetails()
            .namespace(WORKSPACE_NS)
            .name(WORKSPACE_NS)
            .createdBy(LOGGED_IN_USER_EMAIL)
            .googleProject(GOOGLE_PROJECT_ID)
            .bucketName(WORKSPACE_BUCKET);
    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspaceDetail);
    fcResponse.setAccessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(accessLevel));
    when(mockFireCloudService.getWorkspace(any())).thenReturn(Optional.of(fcResponse));
    when(mockFireCloudService.getWorkspace(
            fcWorkspaceDetail.getNamespace(), fcWorkspaceDetail.getName()))
        .thenReturn(fcResponse);
  }

  private String getAppName(AppType appType) {
    return "all-of-us-" + user.getUserId() + "-" + appType.toString().toLowerCase();
  }
}
