package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.common.collect.ImmutableList;
import jakarta.mail.MessagingException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.LeonardoConfig;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoCloudContext;
import org.pmiops.workbench.leonardo.model.LeonardoCloudProvider;
import org.pmiops.workbench.leonardo.model.LeonardoDiskStatus;
import org.pmiops.workbench.leonardo.model.LeonardoDiskType;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

@DataJpaTest
public class OfflineRuntimeControllerTest {
  private static final Instant NOW = FakeClockConfiguration.NOW.toInstant();
  private static final Duration RUNTIME_MAX_AGE = Duration.ofDays(14);
  private static final Duration RUNTIME_IDLE_MAX_AGE = Duration.ofDays(7);

  @TestConfiguration
  @MockBean({
    FireCloudService.class,
    FreeTierBillingService.class,
    MailService.class,
  })
  @Import({FakeClockConfiguration.class, OfflineRuntimeController.class, LeonardoMapperImpl.class})
  static class Configuration {
    @Bean
    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
      config.firecloud.notebookRuntimeMaxAgeDays = (int) RUNTIME_MAX_AGE.toDays();
      config.firecloud.notebookRuntimeIdleMaxAgeDays = (int) RUNTIME_IDLE_MAX_AGE.toDays();
      return config;
    }
  }

  @Qualifier(LeonardoConfig.SERVICE_RUNTIMES_API)
  @MockBean
  private RuntimesApi mockRuntimesApi;

  @Qualifier(LeonardoConfig.SERVICE_DISKS_API)
  @MockBean
  private DisksApi mockDisksApi;

  @Autowired private FireCloudService mockFireCloudService;
  @Autowired private FreeTierBillingService mockFreeTierBillingService;
  @Autowired private MailService mockMailService;

  @Autowired private LeonardoMapper leonardoMapper;
  @Autowired private OfflineRuntimeController controller;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;

  private DbUser user1;
  private DbUser user2;
  private DbWorkspace workspace;
  private int runtimeProjectIdIndex = 0;

  @BeforeEach
  public void setUp() {
    runtimeProjectIdIndex = 0;
    DbCdrVersion cdrVersion = createDefaultCdrVersion();
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion = cdrVersionDao.save(cdrVersion);

    user1 = new DbUser();
    user1.setUsername("alice@fake-research-aou.org");
    user1.setContactEmail("alice@gmail.com");
    user1 = userDao.save(user1);

    user2 = new DbUser();
    user2.setUsername("bob@fake-research-aou.org");
    user2.setContactEmail("bob@gmail.com");
    user2 = userDao.save(user2);

    workspace =
        workspaceDao.save(
            TestMockFactory.createDbWorkspaceStub(TestMockFactory.createWorkspace("ns1", "ws1"), 0L)
                .setCdrVersion(cdrVersion)
                .setGoogleProject("proj1"));
  }

  @AfterEach
  public void tearDown() {
    workspaceDao.deleteAll();
    cdrVersionDao.deleteAll();
    userDao.deleteAll();
  }

  private LeonardoGetRuntimeResponse runtimeWithAge(Duration age) {
    return runtimeWithAgeAndIdle(age, Duration.ZERO);
  }

  private LeonardoGetRuntimeResponse runtimeWithAgeAndIdle(Duration age, Duration idleTime) {
    // There should only be one runtime per project, so increment an index for
    // each runtime created per test.
    return new LeonardoGetRuntimeResponse()
        .runtimeName("all-of-us")
        .cloudContext(
            new LeonardoCloudContext()
                .cloudProvider(LeonardoCloudProvider.GCP)
                .cloudResource(String.format("proj-%d", runtimeProjectIdIndex++)))
        .status(LeonardoRuntimeStatus.RUNNING)
        .auditInfo(
            new LeonardoAuditInfo()
                .createdDate(NOW.minus(age).toString())
                .dateAccessed(NOW.minus(idleTime).toString()));
  }

  private List<LeonardoListRuntimeResponse> toListRuntimeResponseList(
      List<LeonardoGetRuntimeResponse> runtimes) {
    return runtimes.stream()
        .map(leonardoMapper::toListRuntimeResponse)
        .collect(Collectors.toList());
  }

  private void stubRuntimes(List<LeonardoGetRuntimeResponse> runtimes) throws Exception {
    when(mockRuntimesApi.listRuntimes(any(), any()))
        .thenReturn(toListRuntimeResponseList(runtimes));

    for (LeonardoGetRuntimeResponse runtime : runtimes) {
      when(mockRuntimesApi.getRuntime(
              leonardoMapper.toGoogleProject(runtime.getCloudContext()), runtime.getRuntimeName()))
          .thenReturn(runtime);
    }
  }

  private LeonardoListPersistentDiskResponse idleDisk(Duration idleTime) {
    return idleDiskForProjectAndCreator(
        workspace.getGoogleProject(), user1.getUsername(), idleTime);
  }

  private LeonardoListPersistentDiskResponse idleDiskForProjectAndCreator(
      String googleProject, String creatorEmail, Duration idleTime) {
    return new LeonardoListPersistentDiskResponse()
        .diskType(LeonardoDiskType.STANDARD)
        .status(LeonardoDiskStatus.READY)
        .cloudContext(
            new LeonardoCloudContext()
                .cloudProvider(LeonardoCloudProvider.GCP)
                .cloudResource(googleProject))
        .name("my-disk")
        .size(200)
        .auditInfo(
            new LeonardoAuditInfo()
                .creator(creatorEmail)
                .createdDate(NOW.minus(idleTime.plus(Duration.ofDays(1L))).toString())
                .dateAccessed(NOW.minus(idleTime).toString()));
  }

  private void stubDisks(List<LeonardoListPersistentDiskResponse> disks) throws Exception {
    when(mockDisksApi.listDisks(any(), any(), anyString(), any())).thenReturn(disks);
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

  @Test
  public void testDeleteOldRuntimesNoResults() throws Exception {
    stubRuntimes(ImmutableList.of());
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testDeleteOldRuntimesActiveRuntime() throws Exception {
    stubRuntimes(ImmutableList.of(runtimeWithAge(Duration.ofHours(10))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testDeleteOldRuntimesActiveTooOld() throws Exception {
    stubRuntimes(ImmutableList.of(runtimeWithAge(RUNTIME_MAX_AGE.plusMinutes(5))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testDeleteOldRuntimesIdleYoung() throws Exception {
    // Running for under the IDLE_MAX_AGE, idle for 10 hours
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAgeAndIdle(RUNTIME_IDLE_MAX_AGE.minusMinutes(10), Duration.ofHours(10))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testDeleteOldRuntimesIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for 10 hours
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAgeAndIdle(RUNTIME_IDLE_MAX_AGE.plusMinutes(15), Duration.ofHours(10))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testDeleteOldRuntimesBrieflyIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for only 15 minutes
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAgeAndIdle(RUNTIME_IDLE_MAX_AGE.plusMinutes(15), Duration.ofMinutes(15))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testDeleteOldRuntimesOtherStatusFiltered() throws Exception {
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAge(RUNTIME_MAX_AGE.plusDays(10)).status(LeonardoRuntimeStatus.DELETING)));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testCheckPersistentDisksNoDisks() throws Exception {
    stubDisks(ImmutableList.of());
    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verifyNoInteractions(mockMailService);
  }

  @Test
  public void testCheckPersistentDisksNoMatchingNotifications() throws Exception {
    stubDisks(
        ImmutableList.of(
            idleDisk(Duration.ofDays(0L)),
            idleDisk(Duration.ofDays(1L)),
            idleDisk(Duration.ofDays(13L)),
            idleDisk(Duration.ofDays(31L)),
            idleDisk(Duration.ofDays(119L))));
    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verifyNoInteractions(mockMailService);
  }

  @Test
  public void testCheckPersistentDisksSkipsNonReady() throws Exception {
    stubDisks(ImmutableList.of(idleDisk(Duration.ofDays(14L)).status(LeonardoDiskStatus.FAILED)));
    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verifyNoInteractions(mockMailService);
  }

  @Test
  public void testCheckPersistentDisksInitialThresholdNotification() throws Exception {
    stubWorkspaceOwners(workspace, ImmutableList.of(user1));
    stubDisks(ImmutableList.of(idleDisk(Duration.ofDays(14L))));
    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(
            eq(ImmutableList.of(user1)), eq(workspace), any(), eq(14), eq(null));
  }

  @Test
  public void testCheckPersistentDisksPeriodicThresholdNotification() throws Exception {
    stubWorkspaceOwners(workspace, ImmutableList.of(user1));
    stubDisks(
        ImmutableList.of(
            idleDisk(Duration.ofDays(30L)),
            idleDisk(Duration.ofDays(60L)),
            idleDisk(Duration.ofDays(90L))));
    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockMailService, times(3))
        .alertUsersUnusedDiskWarningThreshold(
            eq(ImmutableList.of(user1)), eq(workspace), any(), anyInt(), any());
  }

  @Test
  public void testCheckPersistentDisksMultipleOwners() throws Exception {
    stubWorkspaceOwners(workspace, ImmutableList.of(user1, user2));
    stubDisks(ImmutableList.of(idleDisk(Duration.ofDays(30L))));
    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockMailService, times(1))
        .alertUsersUnusedDiskWarningThreshold(
            eq(ImmutableList.of(user1, user2)), eq(workspace), any(), anyInt(), any());
  }

  @Test
  public void testCheckPersistentDisksSkipsUnknownUser() throws Exception {
    stubWorkspaceOwners(workspace, ImmutableList.of(user1));

    LeonardoListPersistentDiskResponse mysteryDisk = idleDisk(Duration.ofDays(14L));
    mysteryDisk.getAuditInfo().setCreator("404@aou.org");
    stubDisks(ImmutableList.of(mysteryDisk, idleDisk(Duration.ofDays(30L))));

    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Skips the unknown user, but still sends the rest.
    verify(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(
            eq(ImmutableList.of(user1)), eq(workspace), any(), eq(30), any());
  }

  @Test
  public void testCheckPersistentDisksContinuesOnMailFailure() throws Exception {
    stubWorkspaceOwners(workspace, ImmutableList.of(user1));
    stubDisks(
        ImmutableList.of(
            idleDisk(Duration.ofDays(14L)),
            idleDisk(Duration.ofDays(14L)),
            idleDisk(Duration.ofDays(14L))));

    doThrow(MessagingException.class)
        // Throw on the first call only.
        .doNothing()
        .when(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(any(), any(), any(), anyInt(), any());

    assertThrows(ServerErrorException.class, () -> controller.checkPersistentDisks());

    // 3 calls, including the initial throwing call.
    verify(mockMailService, times(3))
        .alertUsersUnusedDiskWarningThreshold(any(), any(), any(), anyInt(), any());
  }

  @Test
  public void testCheckPersistentDisksFreeTier() throws Exception {
    stubWorkspaceOwners(workspace, ImmutableList.of(user1));
    stubDisks(ImmutableList.of(idleDisk(Duration.ofDays(14L))));

    when(mockFreeTierBillingService.isFreeTier(workspace)).thenReturn(true);
    when(mockFreeTierBillingService.getWorkspaceCreatorFreeCreditsRemaining(workspace))
        .thenReturn(123.0);

    assertThat(controller.checkPersistentDisks().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockMailService)
        .alertUsersUnusedDiskWarningThreshold(
            eq(ImmutableList.of(user1)), eq(workspace), any(), eq(14), eq(123.0));
  }
}
