package org.pmiops.workbench.rdr;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.FakeJpaDateTimeConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2.DbEducationV2;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.RdrEntity;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.profile.DemographicSurveyMapperImpl;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.rdr.model.RdrResearcher;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspaceCreator;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class RdrExportServiceImplTest {
  @Autowired private RdrExportService rdrExportService;
  @Autowired private RdrMapper rdrMapper;

  @Autowired private FakeClock clock;
  @Autowired private AccessTierService mockAccessTierService;
  @Autowired private ApiClient mockApiClient;
  @Autowired private RdrApi mockRdrApi;
  @Autowired private WorkspaceService mockWorkspaceService;
  @Autowired private RdrExportDao rdrExportDao;
  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private DbWorkspace workspace;
  private DbWorkspace deletedWorkspace;
  private DbWorkspace creatorWorkspace;

  private static final boolean NO_BACKFILL = false;

  private DbUser dbUserWithEmail;
  private DbUser dbUserWithoutEmail;

  private static final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    FakeJpaDateTimeConfiguration.class,
    DemographicSurveyMapperImpl.class,
    RdrExportServiceImpl.class,
    RdrMapperImpl.class
  })
  @MockBean({
    AccessTierService.class,
    ApiClient.class,
    RdrApi.class,
    InstitutionService.class,
    WorkspaceService.class,
    VerifiedInstitutionalAffiliationDao.class
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    workbenchConfig.auth.serviceAccountApiUsers = ImmutableList.of("appspot@gserviceaccount.com");

    rdrExportService = spy(rdrExportService);
    when(mockAccessTierService.getAccessTiersForUser(any())).thenReturn(ImmutableList.of());
    when(mockRdrApi.getApiClient()).thenReturn(mockApiClient);

    dbUserWithEmail =
        userDao.save(
            new DbUser()
                .setUserId(1L)
                .setUsername("userWithEmail")
                .setCreationTime(FakeClockConfiguration.NOW)
                .setLastModifiedTime(FakeClockConfiguration.NOW)
                .setGivenName("icanhas")
                .setFamilyName("email")
                .setContactEmail("i.can.has.email@gmail.com")
                .setDegreesEnum(Collections.singletonList(Degree.NONE)));

    dbUserWithoutEmail =
        userDao.save(
            new DbUser()
                .setUserId(2L)
                .setUsername("userWithoutEmail")
                .setCreationTime(FakeClockConfiguration.NOW)
                .setLastModifiedTime(FakeClockConfiguration.NOW)
                .setGivenName("icannothas")
                .setFamilyName("email")
                .setDegreesEnum(Collections.singletonList(Degree.NONE)));

    workspace =
        workspaceDao.save(
            buildDbWorkspace(
                1, "workspace_name", "workspaceNS", WorkspaceActiveStatus.ACTIVE, dbUserWithEmail));

    deletedWorkspace =
        workspaceDao.save(
            buildDbWorkspace(
                2, "workspace_del", "workspaceNS", WorkspaceActiveStatus.DELETED, dbUserWithEmail));

    creatorWorkspace =
        workspaceDao.save(
            buildDbWorkspace(
                3,
                "workspace_name_3",
                "workspaceNS",
                WorkspaceActiveStatus.ACTIVE,
                dbUserWithoutEmail));

    verifiedInstitutionalAffiliationDao.save(
        new DbVerifiedInstitutionalAffiliation()
            .setUser(dbUserWithEmail)
            .setInstitution(new DbInstitution().setShortName("mockInstitution"))
            .setInstitutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL));
  }

  @AfterEach
  public void tearDown() {
    rdrExportDao.deleteAll();
    workspaceDao.deleteAll();
    verifiedInstitutionalAffiliationDao.deleteAll();
    userDao.deleteAll();
  }

  private DbWorkspace buildDbWorkspace(
      long dbId,
      String name,
      String namespace,
      WorkspaceActiveStatus activeStatus,
      DbUser creator) {
    return new DbWorkspace()
        .setLastModifiedTime(FakeClockConfiguration.NOW)
        .setCreationTime(FakeClockConfiguration.NOW)
        .setName(name)
        .setWorkspaceId(dbId)
        .setWorkspaceNamespace(namespace)
        .setWorkspaceActiveStatusEnum(activeStatus)
        .setFirecloudName(name)
        .setFirecloudUuid(Long.toString(dbId))
        .setScientificApproach("Scientific Approach")
        .setReasonForAllOfUs("Reason for AllOf Us")
        .setEthics(false)
        .setReviewRequested(true)
        .setCreator(creator);
  }

  @Test
  public void exportUsers() throws ApiException {
    boolean backfill = false;
    doNothing().when(mockRdrApi).exportResearchers(anyList(), anyBoolean());

    List<Long> userIds =
        ImmutableList.of(dbUserWithEmail.getUserId(), dbUserWithoutEmail.getUserId());
    rdrExportService.exportUsers(userIds, backfill);

    verify(rdrExportService, times(1)).updateDbRdrExport(RdrEntity.USER, userIds);
  }

  @Test
  public void exportUsersUnsuccessfulNoPersist() throws ApiException {
    boolean backfill = false;
    doThrow(new ApiException()).when(mockRdrApi).exportResearchers(anyList(), anyBoolean());

    List<Long> userIds =
        ImmutableList.of(dbUserWithEmail.getUserId(), dbUserWithoutEmail.getUserId());
    assertThrows(ServerErrorException.class, () -> rdrExportService.exportUsers(userIds, backfill));

    verify(rdrExportService, times(0)).updateDbRdrExport(any(), anyList());
  }

  @Test
  public void exportUsersBackfill() throws ApiException {
    boolean backfill = true;
    rdrExportService.exportUsers(
        ImmutableList.of(dbUserWithEmail.getUserId(), dbUserWithoutEmail.getUserId()), backfill);
    assertThat(rdrExportDao.findAll()).isEmpty();

    verify(mockRdrApi).exportResearchers(anyList(), eq(backfill));
  }

  @Test
  public void exportUsers_DemoSurveyV2Export() throws ApiException {
    boolean backfill = false;

    dbUserWithEmail =
        userDao.save(
            dbUserWithEmail.setDemographicSurveyV2(
                new DbDemographicSurveyV2()
                    .setUser(dbUserWithEmail)
                    .setEducation(DbEducationV2.DOCTORATE)));

    List<Long> userIds = Collections.singletonList(dbUserWithEmail.getUserId());
    rdrExportService.exportUsers(userIds, backfill);

    RdrResearcher expectedWithSurvey =
        rdrMapper.toRdrResearcher(dbUserWithEmail, Collections.emptyList(), null);
    // test sanity check
    assertThat(expectedWithSurvey.getDemographicSurveyV2()).isNotNull();

    verify(rdrExportService, times(1)).updateDbRdrExport(RdrEntity.USER, userIds);
    verify(mockRdrApi).exportResearchers(Collections.singletonList(expectedWithSurvey), backfill);
  }

  @Test
  public void exportUsers_DemoSurveyV2NoExport_nullBoolean() {
    boolean backfill = false;

    dbUserWithEmail =
        userDao.save(
            dbUserWithEmail.setDemographicSurveyV2(
                new DbDemographicSurveyV2()
                    .setUser(dbUserWithEmail)
                    .setEducation(DbEducationV2.DOCTORATE)));

    List<Long> userIds = Collections.singletonList(dbUserWithEmail.getUserId());
    rdrExportService.exportUsers(userIds, backfill);

    RdrResearcher expectedNoSurvey =
        rdrMapper
            .toRdrResearcher(dbUserWithEmail, Collections.emptyList(), null)
            .demographicSurveyV2(null);
    // test sanity check
    assertThat(expectedNoSurvey.getDemographicSurveyV2()).isNull();

    verify(rdrExportService, times(1)).updateDbRdrExport(RdrEntity.USER, userIds);
  }

  @Test
  public void exportWorkspace() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);
    rdrExportService.exportWorkspaces(ImmutableList.of(workspace.getWorkspaceId()), NO_BACKFILL);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    assertThat(rdrExportDao.findAll()).hasSize(1);

    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  @Test
  public void exportWorkspace_firecloudCallFail_skipUpdateRdrEntity() throws ApiException {
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenThrow(WorkbenchException.class);

    // workspace.getWorkspaceId() fails, so skip that export. There should be only one workspace
    // exported
    rdrExportService.exportWorkspaces(
        ImmutableList.of(workspace.getWorkspaceId(), creatorWorkspace.getWorkspaceId()),
        NO_BACKFILL);
    assertThat(rdrExportDao.findAll()).hasSize(1);
  }

  @Test
  public void exportWorkspaceBackfill() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);
    rdrExportService.exportWorkspaces(ImmutableList.of(workspace.getWorkspaceId()), true);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    assertThat(rdrExportDao.findAll()).isEmpty();

    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), true);
  }

  @Test
  public void exportWorkspaceFocusOnUnderservedPopulation() throws ApiException {
    workspace =
        workspaceDao.save(
            workspace.setSpecificPopulationsEnum(ImmutableSet.of(SpecificPopulationEnum.RACE_AA)));

    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);

    rdrExportService.exportWorkspaces(ImmutableList.of(workspace.getWorkspaceId()), NO_BACKFILL);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    assertThat(rdrExportDao.findAll()).hasSize(1);

    rdrWorkspace
        .getWorkspaceDemographic()
        .setRaceEthnicity(ImmutableList.of(RdrWorkspaceDemographic.RaceEthnicityEnum.AA));
    rdrWorkspace.setFocusOnUnderrepresentedPopulations(true);
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  @Test
  public void exportWorkspaceCreatorInformation() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);

    rdrExportService.exportWorkspaces(ImmutableList.of(workspace.getWorkspaceId()), NO_BACKFILL);
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);

    rdrWorkspace = toDefaultRdrWorkspace(creatorWorkspace);

    rdrExportService.exportWorkspaces(
        ImmutableList.of(creatorWorkspace.getWorkspaceId()), NO_BACKFILL);
    rdrWorkspace.setCreator(
        new RdrWorkspaceCreator()
            .userId(dbUserWithoutEmail.getUserId())
            .familyName("email")
            .givenName("icannothas"));
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  @Test
  public void exportWorkspaceDeletedWorkspace() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(deletedWorkspace);

    rdrExportService.exportWorkspaces(
        ImmutableList.of(deletedWorkspace.getWorkspaceId()), NO_BACKFILL);
    verify(mockWorkspaceService, never())
        .getFirecloudUserRoles(
            deletedWorkspace.getWorkspaceNamespace(), deletedWorkspace.getFirecloudName());
    assertThat(rdrExportDao.findAll()).hasSize(1);

    rdrWorkspace.setStatus(RdrWorkspace.StatusEnum.INACTIVE);
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  @Test
  public void findUnchangedEntitiesForBackfill_users() {
    Supplier<List<Long>> findUnchangedUsers =
        () -> rdrExportService.findUnchangedEntitiesForBackfill(RdrEntity.USER);
    assertThat(findUnchangedUsers.get()).isEmpty();

    rdrExportService.exportUsers(ImmutableList.of(dbUserWithEmail.getUserId()), NO_BACKFILL);
    assertThat(findUnchangedUsers.get()).hasSize(1);

    clock.increment(Duration.ofMinutes(5).toMillis());
    userDao.save(
        dbUserWithEmail
            .setAreaOfResearch("sorcery")
            .setLastModifiedTime(Timestamp.from(clock.instant())));
    assertThat(rdrExportService.findAllUserIdsToExport()).hasSize(2);
    assertThat(findUnchangedUsers.get()).isEmpty();
  }

  @Test
  public void findUnchangedEntitiesForBackfill_workspaces() {
    Supplier<List<Long>> findUnchangedWorkspaces =
        () -> rdrExportService.findUnchangedEntitiesForBackfill(RdrEntity.WORKSPACE);
    assertThat(findUnchangedWorkspaces.get()).isEmpty();

    rdrExportService.exportWorkspaces(
        ImmutableList.of(
            workspace.getWorkspaceId(),
            deletedWorkspace.getWorkspaceId(),
            creatorWorkspace.getWorkspaceId()),
        NO_BACKFILL);
    assertThat(findUnchangedWorkspaces.get()).hasSize(3);

    clock.increment(Duration.ofMinutes(5).toMillis());
    workspaceDao.save(
        workspace.setAdditionalNotes("!!!").setLastModifiedTime(Timestamp.from(clock.instant())));
    assertThat(rdrExportService.findAllWorkspacesIdsToExport()).hasSize(1);
    assertThat(findUnchangedWorkspaces.get()).hasSize(2);
  }

  private RdrWorkspace toDefaultRdrWorkspace(DbWorkspace dbWorkspace) {
    return rdrMapper
        .toRdrWorkspace(dbWorkspace)
        .excludeFromPublicDirectory(false)
        .workspaceUsers(ImmutableList.of());
  }
}
