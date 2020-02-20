package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.api.services.cloudbilling.Cloudbilling;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceServiceTest {

  @TestConfiguration
  @Import({ManualWorkspaceMapper.class})
  static class Configuration {
    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.featureFlags.enableBillingLockout = true;
      return workbenchConfig;
    }
  }

  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private UserDao userDao;
  @Autowired private UserRecentWorkspaceDao userRecentWorkspaceDao;
  @Autowired private ManualWorkspaceMapper manualWorkspaceMapper;
  @Autowired private Provider<WorkbenchConfig> mockWorkbenchConfigProvider;

  @Mock private CohortCloningService mockCohortCloningService;
  @Mock private ConceptSetService mockConceptSetService;
  @Mock private DataSetService mockDataSetService;
  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private FireCloudService mockFireCloudService;
  @Mock private Provider<Cloudbilling> mockCloudbillingProvider;
  @Mock private Clock mockClock;
  @Mock private FreeTierBillingService freeTierBillingService;

  private WorkspaceService workspaceService;

  private List<FirecloudWorkspaceResponse> mockWorkspaceResponses = new ArrayList<>();
  private List<DbWorkspace> mockWorkspaces = new ArrayList<>();
  private AtomicLong workspaceIdIncrementer = new AtomicLong(1);
  private Instant NOW = Instant.now();
  private long USER_ID = 1L;
  private String DEFAULT_USERNAME = "mock@mock.com";
  private String DEFAULT_WORKSPACE_NAMESPACE = "namespace";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    workspaceService =
        new WorkspaceServiceImpl(
            mockCloudbillingProvider,
            mockCloudbillingProvider,
            mockClock,
            mockCohortCloningService,
            mockConceptSetService,
            mockDataSetService,
            mockFireCloudService,
            userDao,
            mockUserProvider,
            userRecentWorkspaceDao,
            mockWorkbenchConfigProvider,
            workspaceDao,
            manualWorkspaceMapper,
            freeTierBillingService);

    mockWorkspaceResponses.clear();
    mockWorkspaces.clear();
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "reader",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.READER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "writer",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.WRITER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "owner",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "another_extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);

    doReturn(mockWorkspaceResponses).when(mockFireCloudService).getWorkspaces(any());
    DbUser mockUser = mock(DbUser.class);
    doReturn(mockUser).when(mockUserProvider).get();
    doReturn(DEFAULT_USERNAME).when(mockUser).getUsername();
    doReturn(USER_ID).when(mockUser).getUserId();
  }

  private FirecloudWorkspaceResponse mockFirecloudWorkspaceResponse(
      String workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel) {
    FirecloudWorkspace mockWorkspace = mock(FirecloudWorkspace.class);
    doReturn(workspaceNamespace).when(mockWorkspace).getNamespace();
    doReturn(workspaceName).when(mockWorkspace).getName();
    doReturn(workspaceId).when(mockWorkspace).getWorkspaceId();
    FirecloudWorkspaceResponse mockWorkspaceResponse = mock(FirecloudWorkspaceResponse.class);
    doReturn(mockWorkspace).when(mockWorkspaceResponse).getWorkspace();
    doReturn(accessLevel.toString()).when(mockWorkspaceResponse).getAccessLevel();
    return mockWorkspaceResponse;
  }

  private DbWorkspace buildDbWorkspace(
      long dbId, String name, String namespace, WorkspaceActiveStatus activeStatus) {
    DbWorkspace workspace = new DbWorkspace();
    Timestamp nowTimestamp = Timestamp.from(NOW);
    workspace.setLastModifiedTime(nowTimestamp);
    workspace.setCreationTime(nowTimestamp);
    workspace.setName(name);
    workspace.setWorkspaceId(dbId);
    workspace.setWorkspaceNamespace(namespace);
    workspace.setWorkspaceActiveStatusEnum(activeStatus);
    workspace.setFirecloudName(name);
    workspace.setFirecloudUuid(Long.toString(dbId));
    return workspace;
  }

  private DbWorkspace addMockedWorkspace(
      long workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel,
      WorkspaceActiveStatus activeStatus) {

    FirecloudWorkspaceResponse mockWorkspaceResponse =
        mockFirecloudWorkspaceResponse(
            Long.toString(workspaceId), workspaceName, workspaceNamespace, accessLevel);
    mockWorkspaceResponses.add(mockWorkspaceResponse);
    doReturn(mockWorkspaceResponse)
        .when(mockFireCloudService)
        .getWorkspace(workspaceNamespace, workspaceName);

    DbWorkspace dbWorkspace =
        workspaceDao.save(
            buildDbWorkspace(
                workspaceId,
                mockWorkspaceResponse.getWorkspace().getName(),
                workspaceNamespace,
                activeStatus));

    mockWorkspaces.add(dbWorkspace);
    return dbWorkspace;
  }

  @Test
  public void getWorkspaces() {
    assertThat(workspaceService.getWorkspaces()).hasSize(5);
  }

  @Test
  public void getWorkspaces_skipPending() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "inactive",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.PENDING_DELETION_POST_1PPW_MIGRATION);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void getWorkspaces_skipDeleted() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "deleted",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.DELETED);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void activeStatus() {
    EnumSet.allOf(WorkspaceActiveStatus.class)
        .forEach(
            status ->
                assertThat(
                        buildDbWorkspace(
                                workspaceIdIncrementer.getAndIncrement(),
                                "1",
                                DEFAULT_WORKSPACE_NAMESPACE,
                                status)
                            .getWorkspaceActiveStatusEnum())
                    .isEqualTo(status));
  }

  @Test
  public void updateRecentWorkspaces() {
    mockWorkspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          workspaceService.updateRecentWorkspaces(
              workspace,
              USER_ID,
              Timestamp.from(NOW.minusSeconds(mockWorkspaces.size() - workspace.getWorkspaceId())));
        });
    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT);

    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<Long> expectedIds =
        mockWorkspaces
            .subList(
                mockWorkspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                mockWorkspaces.size())
            .stream()
            .map(DbWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsAllIn(expectedIds);
  }

  @Test
  public void updateRecentWorkspaces_multipleUsers() {
    long OTHER_USER_ID = 2L;
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(0), OTHER_USER_ID, Timestamp.from(NOW));
    mockWorkspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          workspaceService.updateRecentWorkspaces(
              workspace,
              USER_ID,
              Timestamp.from(NOW.minusSeconds(mockWorkspaces.size() - workspace.getWorkspaceId())));
        });
    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();

    assertThat(recentWorkspaces.size()).isEqualTo(4);
    recentWorkspaces.forEach(
        userRecentWorkspace ->
            assertThat(userRecentWorkspace.getId())
                .isNotEqualTo(userRecentWorkspace.getWorkspaceId()));

    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<Long> expectedIds =
        mockWorkspaces
            .subList(
                mockWorkspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                mockWorkspaces.size())
            .stream()
            .map(DbWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsAllIn(expectedIds);

    DbUser mockUser = mock(DbUser.class);
    doReturn(mockUser).when(mockUserProvider).get();
    doReturn(DEFAULT_USERNAME).when(mockUser).getUsername();
    doReturn(OTHER_USER_ID).when(mockUser).getUserId();
    List<DbUserRecentWorkspace> otherRecentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(otherRecentWorkspaces.size()).isEqualTo(1);
    assertThat(otherRecentWorkspaces.get(0).getWorkspaceId())
        .isEqualTo(mockWorkspaces.get(0).getWorkspaceId());
  }

  @Test
  public void updateRecentWorkspaces_flipFlop() {
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(0), USER_ID, Timestamp.from(NOW.minusSeconds(4)));
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(1), USER_ID, Timestamp.from(NOW.minusSeconds(3)));
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(0), USER_ID, Timestamp.from(NOW.minusSeconds(2)));
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(1), USER_ID, Timestamp.from(NOW.minusSeconds(1)));
    workspaceService.updateRecentWorkspaces(mockWorkspaces.get(0), USER_ID, Timestamp.from(NOW));

    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(2);
    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsAllOf(1L, 2L);
  }

  @Test
  public void enforceFirecloudAclsInRecentWorkspaces() {
    long ownedId = workspaceIdIncrementer.getAndIncrement();
    DbWorkspace ownedWorkspace =
        addMockedWorkspace(
            ownedId,
            "owned",
            "owned_namespace",
            WorkspaceAccessLevel.OWNER,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(ownedWorkspace, USER_ID, Timestamp.from(NOW));

    DbWorkspace sharedWorkspace =
        addMockedWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "shared",
            "shared_namespace",
            WorkspaceAccessLevel.NO_ACCESS,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(sharedWorkspace, USER_ID, Timestamp.from(NOW));

    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(1);
    assertThat(recentWorkspaces.get(0).getWorkspaceId()).isEqualTo(ownedId);
  }
}
