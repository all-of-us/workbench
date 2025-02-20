package org.pmiops.workbench.disk;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.appTypeToLabelValue;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import jakarta.mail.MessagingException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudContext;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudProvider;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.disks.DiskAdminService;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.legacy_leonardo_client.ApiException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfigInResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class DiskAdminServiceTest {
  private static final Instant NOW = FakeClockConfiguration.NOW.toInstant();

  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private LeonardoApiClient mockLeonardoApiClient;
  @MockBean private InitialCreditsService mockInitialCreditsService;
  @MockBean private MailService mockMailService;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private DiskAdminService service;

  private DbUser user1;
  private DbUser user2;
  private DbWorkspace workspace;
  private static final WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();

  @TestConfiguration
  @MockBean(WorkspaceService.class)
  @Import({
    FakeClockConfiguration.class,
    DiskAdminService.class,
    LeonardoMapperImpl.class,
  })
  static class Configuration {
    @Bean
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  @BeforeEach
  public void setUp() {
    user1 = new DbUser();
    user1.setUsername("alice@fake-research-aou.org");
    user1.setContactEmail("alice@gmail.com");
    user1 = userDao.save(user1);

    user2 = new DbUser();
    user2.setUsername("bob@fake-research-aou.org");
    user2.setContactEmail("bob@gmail.com");
    user2 = userDao.save(user2);

    DbCdrVersion cdrVersion = createDefaultCdrVersion();
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion = cdrVersionDao.save(cdrVersion);

    workspace =
        workspaceDao.save(
            TestMockFactory.createDbWorkspaceStub(
                    TestMockFactory.createWorkspace("ns1", "Workspace Name 1", "workspacename1"),
                    0L)
                .setCdrVersion(cdrVersion)
                .setGoogleProject("proj1"));
  }

  @AfterEach
  public void tearDown() {
    workspaceDao.deleteAll();
    cdrVersionDao.deleteAll();
    userDao.deleteAll();
  }

  @Test
  public void testCheckPersistentDisksNoDisks() {
    stubDisks(Collections.emptyList());

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    verifyNoInteractions(mockMailService);
  }

  @Test
  public void testCheckPersistentDisksNoMatchingNotifications() {
    stubDisks(
        List.of(
            idleDisk(Duration.ofDays(0L)),
            idleDisk(Duration.ofDays(1L)),
            idleDisk(Duration.ofDays(13L)),
            idleDisk(Duration.ofDays(31L)),
            idleDisk(Duration.ofDays(119L))));

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    verifyNoInteractions(mockMailService);
  }

  @Test
  public void testCheckPersistentDisksSkipsNonReady() {
    stubDisks(List.of(idleDisk(Duration.ofDays(14L)).status(DiskStatus.FAILED)));

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    verifyNoInteractions(mockMailService);
  }

  @Test
  public void testCheckPersistentDisksInitialThresholdNotification() throws MessagingException {
    stubWorkspaceOwners(workspace, List.of(user1));
    stubDisks(List.of(idleDisk(Duration.ofDays(14L))));

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    verify(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(
            eq(List.of(user1)), eq(workspace), any(), anyBoolean(), eq(14), eq(null));
  }

  @Test
  public void testCheckPersistentDisksPeriodicThresholdNotification() throws MessagingException {
    stubWorkspaceOwners(workspace, List.of(user1));
    stubDisks(
        List.of(
            idleDisk(Duration.ofDays(30L)),
            idleDisk(Duration.ofDays(60L)),
            idleDisk(Duration.ofDays(90L))));

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    verify(mockMailService, times(3))
        .alertUsersUnusedDiskWarningThreshold(
            eq(List.of(user1)), eq(workspace), any(), anyBoolean(), anyInt(), any());
  }

  @Test
  public void testCheckPersistentDisksMultipleOwners() throws MessagingException {
    stubWorkspaceOwners(workspace, List.of(user1, user2));
    stubDisks(List.of(idleDisk(Duration.ofDays(30L))));

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    verify(mockMailService, times(1))
        .alertUsersUnusedDiskWarningThreshold(
            eq(List.of(user1, user2)), eq(workspace), any(), anyBoolean(), anyInt(), any());
  }

  @Test
  public void testCheckPersistentDisksSkipsUnknownUser() throws MessagingException {
    stubWorkspaceOwners(workspace, List.of(user1));

    ListPersistentDiskResponse mysteryDisk = idleDisk(Duration.ofDays(14L));
    mysteryDisk.getAuditInfo().setCreator("404@aou.org");
    stubDisks(List.of(mysteryDisk, idleDisk(Duration.ofDays(30L))));

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    // Skips the unknown user, but still sends the rest.
    verify(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(
            eq(List.of(user1)), eq(workspace), any(), anyBoolean(), eq(30), any());
  }

  @Test
  public void testCheckPersistentDisksContinuesOnMailFailure() throws MessagingException {
    stubWorkspaceOwners(workspace, List.of(user1));
    stubDisks(
        List.of(
            idleDisk(Duration.ofDays(14L)),
            idleDisk(Duration.ofDays(14L)),
            idleDisk(Duration.ofDays(14L))));

    doThrow(MessagingException.class)
        // Throw on the first call only.
        .doNothing()
        .when(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(any(), any(), any(), anyBoolean(), anyInt(), any());

    assertThrows(ServerErrorException.class, () -> service.checkPersistentDisks());

    // 3 calls, including the initial throwing call.
    verify(mockMailService, times(3))
        .alertUsersUnusedDiskWarningThreshold(any(), any(), any(), anyBoolean(), anyInt(), any());
  }

  @Test
  public void testCheckPersistentDisksFreeTier() throws MessagingException {
    stubWorkspaceOwners(workspace, List.of(user1));
    stubDisks(List.of(idleDisk(Duration.ofDays(14L))));

    workspace.setBillingAccountName(config.billing.initialCreditsBillingAccountName());
    when(mockInitialCreditsService.getWorkspaceCreatorFreeCreditsRemaining(workspace))
        .thenReturn(123.0);

    assertDoesNotThrow(() -> service.checkPersistentDisks());

    verify(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(
            eq(List.of(user1)), eq(workspace), any(), anyBoolean(), eq(14), eq(123.0));
  }

  @Test
  public void testIsDiskAttached_Gce_attached() throws ApiException {
    int diskId = 1;
    ListPersistentDiskResponse diskResponse = new ListPersistentDiskResponse().id(diskId);
    LeonardoGceWithPdConfigInResponse runtimeConfigResponse =
        new LeonardoGceWithPdConfigInResponse().persistentDiskId(diskId);
    // need to use a separate call because this returns a LeonardoRuntimeConfig object instead
    runtimeConfigResponse.cloudService(CloudServiceEnum.GCE);

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(anyString()))
        .thenReturn(
            List.of(new LeonardoListRuntimeResponse().runtimeConfig(runtimeConfigResponse)));

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isTrue();
  }

  @Test
  public void testIsDiskAttached_Gce_multiple() throws ApiException {
    int diskId = 1;
    int otherDiskId = 2;
    ListPersistentDiskResponse diskResponse = new ListPersistentDiskResponse().id(diskId);

    LeonardoGceWithPdConfigInResponse runtimeConfigResponse1 =
        new LeonardoGceWithPdConfigInResponse().persistentDiskId(diskId);
    // need to use a separate call because this returns a LeonardoRuntimeConfig object instead
    runtimeConfigResponse1.cloudService(CloudServiceEnum.GCE);

    LeonardoGceWithPdConfigInResponse runtimeConfigResponse2 =
        new LeonardoGceWithPdConfigInResponse().persistentDiskId(otherDiskId);
    // need to use a separate call because this returns a LeonardoRuntimeConfig object instead
    runtimeConfigResponse2.cloudService(CloudServiceEnum.GCE);

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(anyString()))
        .thenReturn(
            List.of(
                new LeonardoListRuntimeResponse().runtimeConfig(runtimeConfigResponse1),
                new LeonardoListRuntimeResponse().runtimeConfig(runtimeConfigResponse2)));

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isTrue();
  }

  @Test
  public void testIsDiskAttached_Gce_mismatch() throws ApiException {
    int diskId = 1;
    int otherDiskId = 2;
    ListPersistentDiskResponse diskResponse = new ListPersistentDiskResponse().id(diskId);

    LeonardoGceWithPdConfigInResponse runtimeConfigResponse2 =
        new LeonardoGceWithPdConfigInResponse().persistentDiskId(otherDiskId);
    // need to use a separate call because this returns a LeonardoRuntimeConfig object instead
    runtimeConfigResponse2.cloudService(CloudServiceEnum.GCE);

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(anyString()))
        .thenReturn(
            List.of(new LeonardoListRuntimeResponse().runtimeConfig(runtimeConfigResponse2)));

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isFalse();
  }

  @Test
  public void testIsDiskAttached_Gce_no_runtimes() throws ApiException {
    int diskId = 1;
    ListPersistentDiskResponse diskResponse = new ListPersistentDiskResponse().id(diskId);

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(anyString()))
        .thenReturn(Collections.emptyList());

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isFalse();
  }

  @Test
  public void testIsDiskAttached_GKE_App_attached() throws ApiException {
    String diskName = "my-disk-name";
    ListPersistentDiskResponse diskResponse =
        new ListPersistentDiskResponse()
            .name(diskName)
            .labels(Map.of(LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(AppType.RSTUDIO)));

    when(mockLeonardoApiClient.listAppsInProjectAsService(anyString()))
        .thenReturn(List.of(new UserAppEnvironment().diskName(diskName)));

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isTrue();
  }

  @Test
  public void testIsDiskAttached_GKE_App_multiple() throws ApiException {
    String diskName = "my-disk-name";
    String otherDiskName = "other-disk-name";
    ListPersistentDiskResponse diskResponse =
        new ListPersistentDiskResponse()
            .name(diskName)
            .labels(Map.of(LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(AppType.RSTUDIO)));

    when(mockLeonardoApiClient.listAppsInProjectAsService(anyString()))
        .thenReturn(
            List.of(
                new UserAppEnvironment().diskName(diskName),
                new UserAppEnvironment().diskName(otherDiskName)));

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isTrue();
  }

  @Test
  public void testIsDiskAttached_GKE_App_mismatch() throws ApiException {
    String diskName = "my-disk-name";
    String otherDiskName = "other-disk-name";
    ListPersistentDiskResponse diskResponse =
        new ListPersistentDiskResponse()
            .name(diskName)
            .labels(Map.of(LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(AppType.RSTUDIO)));

    when(mockLeonardoApiClient.listAppsInProjectAsService(anyString()))
        .thenReturn(List.of(new UserAppEnvironment().diskName(otherDiskName)));

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isFalse();
  }

  @Test
  public void testIsDiskAttached_GKE_App_no_apps() throws ApiException {
    String diskName = "my-disk-name";
    ListPersistentDiskResponse diskResponse =
        new ListPersistentDiskResponse()
            .name(diskName)
            .labels(Map.of(LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(AppType.RSTUDIO)));

    when(mockLeonardoApiClient.listAppsInProjectAsService(anyString()))
        .thenReturn(Collections.emptyList());

    assertThat(service.isDiskAttached(diskResponse, "test-project")).isFalse();
  }

  private ListPersistentDiskResponse idleDisk(Duration idleTime) {
    return idleDiskForProjectAndCreator(
        workspace.getGoogleProject(), user1.getUsername(), idleTime);
  }

  private ListPersistentDiskResponse idleDiskForProjectAndCreator(
      String googleProject, String creatorEmail, Duration idleTime) {
    return new ListPersistentDiskResponse()
        .diskType(DiskType.STANDARD)
        .status(DiskStatus.READY)
        .cloudContext(
            new CloudContext().cloudProvider(CloudProvider.GCP).cloudResource(googleProject))
        .name("my-disk")
        .size(200)
        .auditInfo(
            new AuditInfo()
                .creator(creatorEmail)
                .createdDate(NOW.minus(idleTime.plus(Duration.ofDays(1L))).toString())
                .dateAccessed(NOW.minus(idleTime).toString()));
  }

  private void stubDisks(List<ListPersistentDiskResponse> disks) {
    when(mockLeonardoApiClient.listDisksAsService()).thenReturn(disks);
  }

  private void stubWorkspaceOwners(DbWorkspace w, List<DbUser> users) {
    when(mockFireCloudService.getWorkspaceAclAsService(
            w.getWorkspaceNamespace(), w.getFirecloudName()))
        .thenReturn(
            new RawlsWorkspaceACL()
                .acl(
                    users.stream()
                        .collect(
                            Collectors.toMap(
                                DbUser::getUsername,
                                u ->
                                    new RawlsWorkspaceAccessEntry()
                                        .accessLevel(WorkspaceAccessLevel.OWNER.toString())))));
  }
}
