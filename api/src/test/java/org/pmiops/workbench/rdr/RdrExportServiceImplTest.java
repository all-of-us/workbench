package org.pmiops.workbench.rdr;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspaceCreator;
import org.pmiops.workbench.rdr.model.RdrWorkspaceDemographic;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class RdrExportServiceImplTest {
  @Autowired private RdrExportService rdrExportService;
  @Autowired private RdrMapper rdrMapper;

  @Autowired private ApiClient mockApiClient;
  @Autowired private RdrApi mockRdrApi;
  @Autowired private RdrExportDao rdrExportDao;
  @Autowired private UserDao mockUserDao;
  @Autowired private WorkspaceDao mockWorkspaceDao;
  @Autowired private WorkspaceService mockWorkspaceService;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private DbWorkspace workspace, deletedWorkspace, creatorWorkspace;

  private static final Instant NOW = Instant.now();
  private static final Timestamp NOW_TIMESTAMP = Timestamp.from(NOW);
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final boolean NO_BACKFILL = false;

  private DbUser dbUserWithEmail;
  private DbUser dbUserWithoutEmail;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, RdrExportServiceImpl.class, RdrMapperImpl.class})
  @MockBean({
    ApiClient.class,
    RdrApi.class,
    RdrExportDao.class,
    UserDao.class,
    InstitutionService.class,
    WorkspaceDao.class,
    WorkspaceService.class,
    VerifiedInstitutionalAffiliationDao.class
  })
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @BeforeEach
  public void setUp() {
    rdrExportService = spy(rdrExportService);
    when(mockRdrApi.getApiClient()).thenReturn(mockApiClient);

    dbUserWithEmail =
        new DbUser()
            .setUserId(1L)
            .setCreationTime(NOW_TIMESTAMP)
            .setLastModifiedTime(NOW_TIMESTAMP)
            .setGivenName("icanhas")
            .setFamilyName("email")
            .setContactEmail("i.can.has.email@gmail.com")
            .setDegreesEnum(Collections.singletonList(Degree.NONE));

    when(mockUserDao.findUserByUserId(1L)).thenReturn(dbUserWithEmail);
    dbUserWithoutEmail =
        new DbUser()
            .setUserId(2L)
            .setCreationTime(NOW_TIMESTAMP)
            .setLastModifiedTime(NOW_TIMESTAMP)
            .setGivenName("icannothas")
            .setFamilyName("email")
            .setDegreesEnum(Collections.singletonList(Degree.NONE));

    when(mockUserDao.findUserByUserId(2L)).thenReturn(dbUserWithoutEmail);

    when(rdrExportDao.findByEntityTypeAndEntityId(anyShort(), anyLong())).thenReturn(null);
    workspace =
        buildDbWorkspace(
            1, "workspace_name", "workspaceNS", WorkspaceActiveStatus.ACTIVE, dbUserWithEmail);
    when(mockWorkspaceDao.findDbWorkspaceByWorkspaceId(1)).thenReturn(workspace);

    deletedWorkspace =
        buildDbWorkspace(
            2, "workspace_del", "workspaceNS", WorkspaceActiveStatus.DELETED, dbUserWithEmail);
    when(mockWorkspaceDao.findDbWorkspaceByWorkspaceId(2)).thenReturn(deletedWorkspace);

    creatorWorkspace =
        buildDbWorkspace(
            3, "workspace_name_3", "workspaceNS", WorkspaceActiveStatus.ACTIVE, dbUserWithoutEmail);
    when(mockWorkspaceDao.findDbWorkspaceByWorkspaceId(3)).thenReturn(creatorWorkspace);

    when(verifiedInstitutionalAffiliationDao.findFirstByUser(dbUserWithEmail))
        .thenReturn(
            Optional.of(
                new DbVerifiedInstitutionalAffiliation()
                    .setInstitution(new DbInstitution().setShortName("mockInstitution"))
                    .setInstitutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL)));
  }

  private DbWorkspace buildDbWorkspace(
      long dbId,
      String name,
      String namespace,
      WorkspaceActiveStatus activeStatus,
      DbUser creator) {
    Timestamp nowTimestamp = Timestamp.from(NOW);
    return new DbWorkspace()
        .setLastModifiedTime(nowTimestamp)
        .setCreationTime(nowTimestamp)
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
    doNothing().when(mockRdrApi).exportResearchers(anyList());

    rdrExportService.exportUsers(
        ImmutableList.of(dbUserWithEmail.getUserId(), dbUserWithoutEmail.getUserId()));

    verify(rdrExportService, times(1)).updateDbRdrExport(any(), anyList());
  }

  @Test
  public void exportUsersUnsuccessfulnoPersist() throws ApiException {
    doThrow(new ApiException()).when(mockRdrApi).exportResearchers(anyList());

    List<Long> userIds = new ArrayList<>();
    userIds.add(dbUserWithEmail.getUserId());
    userIds.add(dbUserWithoutEmail.getUserId());
    assertThrows(ServerErrorException.class, () -> rdrExportService.exportUsers(userIds));

    verify(rdrExportService, times(0)).updateDbRdrExport(any(), anyList());
  }

  @Test
  public void exportWorkspace() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);
    rdrExportService.exportWorkspaces(ImmutableList.of(1L), NO_BACKFILL);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verify(rdrExportDao, times(1)).saveAll(anyList());

    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  @Test
  public void exportWorkspaceBackfill() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);
    rdrExportService.exportWorkspaces(ImmutableList.of(1L), true);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verify(rdrExportDao, never()).saveAll(anyList());

    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), true);
  }

  @Test
  public void exportWorkspaceFocusOnUnderservedPopulation() throws ApiException {
    workspace.setSpecificPopulationsEnum(ImmutableSet.of(SpecificPopulationEnum.RACE_AA));
    when(mockWorkspaceDao.findDbWorkspaceByWorkspaceId(1)).thenReturn(workspace);

    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);

    rdrExportService.exportWorkspaces(ImmutableList.of(1L), NO_BACKFILL);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verify(rdrExportDao, times(1)).saveAll(anyList());

    rdrWorkspace
        .getWorkspaceDemographic()
        .setRaceEthnicity(ImmutableList.of(RdrWorkspaceDemographic.RaceEthnicityEnum.AA));
    rdrWorkspace.setFocusOnUnderrepresentedPopulations(true);
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  @Test
  public void exportWorkspaceCreatorInformation() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(workspace);

    rdrExportService.exportWorkspaces(ImmutableList.of(1L), NO_BACKFILL);
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);

    rdrWorkspace = toDefaultRdrWorkspace(creatorWorkspace);

    rdrExportService.exportWorkspaces(ImmutableList.of(3L), NO_BACKFILL);
    rdrWorkspace.setCreator(
        new RdrWorkspaceCreator().userId(2l).familyName("email").givenName("icannothas"));
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  @Test
  public void exportWorkspaceDeletedWorkspace() throws ApiException {
    RdrWorkspace rdrWorkspace = toDefaultRdrWorkspace(deletedWorkspace);

    rdrExportService.exportWorkspaces(ImmutableList.of(2L), NO_BACKFILL);
    verify(mockWorkspaceService, never())
        .getFirecloudUserRoles(
            deletedWorkspace.getWorkspaceNamespace(), deletedWorkspace.getFirecloudName());
    verify(rdrExportDao, times(1)).saveAll(anyList());

    rdrWorkspace.setStatus(RdrWorkspace.StatusEnum.INACTIVE);
    verify(mockRdrApi).exportWorkspaces(ImmutableList.of(rdrWorkspace), NO_BACKFILL);
  }

  private RdrWorkspace toDefaultRdrWorkspace(DbWorkspace dbWorkspace) {
    return rdrMapper
        .toRdrWorkspace(dbWorkspace)
        .excludeFromPublicDirectory(false)
        .workspaceUsers(ImmutableList.of());
  }
}
