package org.pmiops.workbench.leonardo;

import java.util.Random;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.api.RuntimeController;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.leonardo.api.AppsApi;
import org.pmiops.workbench.leonardo.model.LeonardoCreateAppRequest;
import org.pmiops.workbench.model.App;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
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
      RuntimeController.class,
      CommonMappers.class,
      UserServiceTestConfiguration.class,
      LeonardoMapperImpl.class,
      LeonardoApiClientImpl.class,
      NotebooksRetryHandler.class,
      LeonardoRetryHandler.class,
      NoBackOffPolicy.class,
      AccessTierServiceImpl.class,
      LeonardoApiClientImpl.class,
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

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }
  }

  @Qualifier(LeonardoConfig.USER_APPS_API)
  @MockBean
  AppsApi userAppsApi;

  @Autowired
  CdrVersionDao cdrVersionDao;
  @MockBean
  WorkspaceDao workspaceDao;
  @Autowired
  UserDao userDao;
  @Autowired
  LeonardoMapper leonardoMapper;
  @Autowired
  LeonardoApiClient leonardoApiClient;
  @MockBean LeonardoApiClientFactory mockLeonardoApiClientFactory;

  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";

  private static WorkbenchConfig config = new WorkbenchConfig();
  private static DbUser user = new DbUser();

  private DbCdrVersion cdrVersion;
  private LeonardoCreateAppRequest testCreateAppRequest;

  private App testApp;
  private DbWorkspace testWorkspace;

  @BeforeEach
  public void setUp() {
    config = WorkbenchConfig.createEmptyConfig();
    config.server.apiBaseUrl = API_BASE_URL;
    config.access.enableComplianceTraining = true;
    config.firecloud.gceVmZone = "us-central-1";

    user = new DbUser().setUsername(LOGGED_IN_USER_EMAIL).setUserId(123L);

    cdrVersion =
        new DbCdrVersion()
            .setName("1")
            // set the db name to be empty since test cases currently
            // run in the workbench schema only.
            .setCdrDbName("")
            .setBigqueryDataset(BIGQUERY_DATASET)
            .setAccessTier(
                TestMockFactory.createControlledTierForTests(accessTierDao)
                    .setDatasetsBucket("gs://cdr-bucket"))
            .setStorageBasePath("v99")
            .setWgsCramManifestPath("wgs/cram/manifest.csv");

    String createdDate = Date.fromYearMonthDay(1988, 12, 26).toString();

    leonardoMapper.mapRuntimeConfig(tmpRuntime, gceConfigObj, null);
    gceConfig = tmpRuntime.getGceConfig();

    testLeoRuntime =
        new LeonardoGetRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.DELETING)
            .runtimeImages(Collections.singletonList(RUNTIME_IMAGE))
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .runtimeConfig(dataprocConfigObj)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));
    testLeoListRuntimeResponse =
        new LeonardoListRuntimeResponse()
            .runtimeName(getRuntimeName())
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING);
    testRuntime =
        new Runtime()
            .runtimeName(getRuntimeName())
            .configurationType(RuntimeConfigurationType.HAILGENOMICANALYSIS)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(RuntimeStatus.DELETING)
            .toolDockerImage(TOOL_DOCKER_IMAGE)
            .autopauseThreshold(AUTOPAUSE_THRESHOLD)
            .dataprocConfig(dataprocConfig)
            .createdDate(createdDate);

    testLeoRuntime2 =
        new LeonardoGetRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));

    testLeoListRuntimeResponse2 =
        new LeonardoListRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING);

    testLeoRuntimeDifferentProject =
        new LeonardoGetRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT)
            .googleProject(GOOGLE_PROJECT_ID_2)
            .status(LeonardoRuntimeStatus.RUNNING)
            .auditInfo(new LeonardoAuditInfo().createdDate(createdDate));

    testLeoListRuntimeResponseDifferentProject =
        new LeonardoListRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT)
            .googleProject(GOOGLE_PROJECT_ID_2)
            .status(LeonardoRuntimeStatus.RUNNING);

    testWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(WORKSPACE_NS)
            .setGoogleProject(GOOGLE_PROJECT_ID)
            .setName(WORKSPACE_NAME)
            .setFirecloudName(WORKSPACE_ID)
            .setCdrVersion(cdrVersion);
    doReturn(Optional.of(testWorkspace)).when(workspaceDao).getByNamespace(WORKSPACE_NS);
  }

  @Test

}
