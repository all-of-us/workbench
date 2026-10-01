package org.pmiops.workbench.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
public class WorkspaceAdminControllerTest {

  @MockitoBean private ActionAuditQueryService actionAuditQueryService;
  @MockitoBean private CloudMonitoringService cloudMonitoringService;
  @MockitoBean private CloudStorageClient cloudStorageClient;
  @MockitoBean private NotebooksService notebooksService;
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

  @MockitoBean private FireCloudService mockFirecloudService;
  @MockitoBean private WorkspaceAdminService mockWorkspaceAdminService;
  @MockitoBean private WorkspaceService mockWorkspaceService;

  @Autowired private WorkspaceAdminController workspaceAdminController;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    FirecloudMapperImpl.class,
    WorkspaceAdminController.class,
    WorkspaceMapperImpl.class,
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
}
