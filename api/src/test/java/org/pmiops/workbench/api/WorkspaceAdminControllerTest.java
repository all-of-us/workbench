package org.pmiops.workbench.api;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.PublishWorkspaceRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class WorkspaceAdminControllerTest {

  private static final long DB_WORKSPACE_ID = 2222L;
  private static final String FIRECLOUD_WORKSPACE_CREATOR_USERNAME = "jay@allofus.biz";
  private static final String WORKSPACE_DISPLAY_NAME = "Work It !";
  private static final String WORKSPACE_TERRA_NAME = "workit";
  private static final String WORKSPACE_NAMESPACE = "aou-rw-12345";
  private static final String NONSENSE_NAMESPACE = "wharrgarbl_wharrgarbl";
  private static final String BAD_EXCEPTION_NULL_REQUEST_DATE_REASON =
      "Cannot have empty Request reason or Request Date";
  private static final String BAD_EXCEPTION_REQUEST_REASON_CHAR =
      "Locking Reason text length should be at least 10 characters long and at most 4000 characters";

  @MockBean private FireCloudService mockFirecloudService;
  @MockBean private LeonardoApiClient mockLeonardoNotebooksClient;
  @MockBean private WorkspaceAdminService mockWorkspaceAdminService;
  @MockBean private WorkspaceService mockWorkspaceService;

  @Autowired private WorkspaceAdminController workspaceAdminController;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    WorkspaceAdminController.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({
    ActionAuditQueryService.class,
    CloudMonitoringService.class,
    CloudStorageClient.class,
    CohortService.class,
    ConceptSetService.class,
    NotebooksService.class,
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    when(mockWorkspaceAdminService.getFirstWorkspaceByNamespace(anyString()))
        .thenReturn(Optional.empty());

    final Workspace workspace =
        TestMockFactory.createWorkspace(
            WORKSPACE_NAMESPACE, WORKSPACE_DISPLAY_NAME, WORKSPACE_TERRA_NAME);
    final DbWorkspace dbWorkspace =
        TestMockFactory.createDbWorkspaceStub(workspace, DB_WORKSPACE_ID);
    when(mockWorkspaceAdminService.getFirstWorkspaceByNamespace(WORKSPACE_NAMESPACE))
        .thenReturn(Optional.of(dbWorkspace));

    final UserRole collaborator =
        new UserRole().email("test@test.test").role(WorkspaceAccessLevel.WRITER);
    final List<UserRole> collaborators = List.of(collaborator);
    when(mockWorkspaceService.getFirecloudUserRoles(WORKSPACE_NAMESPACE, WORKSPACE_TERRA_NAME))
        .thenReturn(collaborators);

    final AdminWorkspaceObjectsCounts adminWorkspaceObjectsCounts =
        new AdminWorkspaceObjectsCounts().cohortCount(1).conceptSetCount(2).datasetCount(3);
    when(mockWorkspaceAdminService.getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId()))
        .thenReturn(adminWorkspaceObjectsCounts);

    final AdminWorkspaceCloudStorageCounts cloudStorageCounts =
        new AdminWorkspaceCloudStorageCounts()
            .notebookFileCount(1)
            .nonNotebookFileCount(2)
            .storageBytesUsed(123456789L);
    when(mockWorkspaceAdminService.getAdminWorkspaceCloudStorageCounts(
            WORKSPACE_NAMESPACE, dbWorkspace.getFirecloudName()))
        .thenReturn(cloudStorageCounts);

    LeonardoListRuntimeResponse leonardoListRuntimeResponse =
        TestMockFactory.createLeonardoListRuntimesResponse();
    List<LeonardoListRuntimeResponse> runtimes = List.of(leonardoListRuntimeResponse);
    when(mockLeonardoNotebooksClient.listRuntimesByProjectAsService(WORKSPACE_NAMESPACE))
        .thenReturn(runtimes);

    RawlsWorkspaceDetails fcWorkspace =
        TestMockFactory.createTerraWorkspace(
            WORKSPACE_NAMESPACE, WORKSPACE_TERRA_NAME, FIRECLOUD_WORKSPACE_CREATOR_USERNAME);
    RawlsWorkspaceResponse fcWorkspaceResponse =
        new RawlsWorkspaceResponse().workspace(fcWorkspace);
    when(mockFirecloudService.getWorkspaceAsService(WORKSPACE_NAMESPACE, WORKSPACE_TERRA_NAME))
        .thenReturn(fcWorkspaceResponse);
  }

  @Test
  public void getWorkspaceAdminView_404sWhenNotFound() {
    doThrow(
            new NotFoundException(
                String.format("No workspace found for namespace %s", NONSENSE_NAMESPACE)))
        .when(mockWorkspaceAdminService)
        .getWorkspaceAdminView(NONSENSE_NAMESPACE);
    assertThrows(
        NotFoundException.class,
        () -> workspaceAdminController.getWorkspaceAdminView(NONSENSE_NAMESPACE));
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_noRequestDate() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis(0L);
    adminLockingRequest.setRequestReason("Some reason to lock");
    assertThrows(
        BadRequestException.class,
        () ->
            workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest),
        BAD_EXCEPTION_NULL_REQUEST_DATE_REASON);
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_noRequestReason() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis(23456L);
    adminLockingRequest.setRequestReason("");
    assertThrows(
        BadRequestException.class,
        () ->
            workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest),
        BAD_EXCEPTION_NULL_REQUEST_DATE_REASON);
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_nullRequestDate() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis(null);
    adminLockingRequest.setRequestReason("Some reason for Locking Workspace");
    assertThrows(
        BadRequestException.class,
        () ->
            workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest),
        BAD_EXCEPTION_NULL_REQUEST_DATE_REASON);
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_nullRequestReason() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis((long) 123456);
    adminLockingRequest.setRequestReason(null);
    assertThrows(
        BadRequestException.class,
        () ->
            workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest),
        BAD_EXCEPTION_NULL_REQUEST_DATE_REASON);
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_correctAdminLockingRequest() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis(654321L);
    adminLockingRequest.setRequestReason("Some reason for Locking Workspace");
    workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest);
    verify(mockWorkspaceAdminService).setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest);
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_lockingReason_lessThan10() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis(654321L);
    adminLockingRequest.setRequestReason("Something");
    assertThrows(
        BadRequestException.class,
        () ->
            workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest),
        BAD_EXCEPTION_REQUEST_REASON_CHAR);
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_lockingReason_moreThanAllowed() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis(654321L);
    // Send locking Reason of length 4001
    adminLockingRequest.setRequestReason(StringUtils.repeat("abcd", 1000) + "1");
    assertThrows(
        BadRequestException.class,
        () ->
            workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest),
        BAD_EXCEPTION_REQUEST_REASON_CHAR);
  }

  @Test
  public void getWorkspaceAdmin_setAdminLock_lockingReason_exactCharacter() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestDateInMillis(654321L);
    // Send locking Reason of length 4000
    adminLockingRequest.setRequestReason(StringUtils.repeat("abcd", 1000));
    workspaceAdminController.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest);
    verify(mockWorkspaceAdminService).setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest);
  }

  @Test
  public void getWorkspace_setAdminUnlock() {
    workspaceAdminController.setAdminUnlockedState(WORKSPACE_NAMESPACE);
    verify(mockWorkspaceAdminService).setAdminUnlockedState(WORKSPACE_NAMESPACE);
  }

  @Test
  public void publishWorkspaceViaDB_Success() {
    PublishWorkspaceRequest request = new PublishWorkspaceRequest();
    workspaceAdminController.publishWorkspaceViaDB(WORKSPACE_NAMESPACE, request);
    verify(mockWorkspaceAdminService).publishWorkspaceViaDB(WORKSPACE_NAMESPACE, request);
  }

  @Test
  public void publishWorkspaceViaDB_NotFound() {
    PublishWorkspaceRequest request = new PublishWorkspaceRequest();
    doThrow(new NotFoundException("not found"))
        .when(mockWorkspaceAdminService)
        .publishWorkspaceViaDB(NONSENSE_NAMESPACE, request);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> workspaceAdminController.publishWorkspaceViaDB(NONSENSE_NAMESPACE, request));
  }

  @Test
  public void unpublishWorkspaceViaDB_Success() {
    workspaceAdminController.unpublishWorkspaceViaDB(WORKSPACE_NAMESPACE);
    verify(mockWorkspaceAdminService).unpublishWorkspaceViaDB(WORKSPACE_NAMESPACE);
  }

  @Test
  public void unpublishWorkspaceViaDB_NotFound() {
    doThrow(new NotFoundException("not found"))
        .when(mockWorkspaceAdminService)
        .unpublishWorkspaceViaDB(NONSENSE_NAMESPACE);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> workspaceAdminController.unpublishWorkspaceViaDB(NONSENSE_NAMESPACE));
  }
}
