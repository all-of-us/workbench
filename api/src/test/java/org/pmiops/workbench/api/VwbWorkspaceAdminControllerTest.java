package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.VwbAodRequest;
import org.pmiops.workbench.model.VwbWorkspace;
import org.pmiops.workbench.model.VwbWorkspaceAdminView;
import org.pmiops.workbench.model.VwbWorkspaceAuditLog;
import org.pmiops.workbench.model.VwbWorkspaceListResponse;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class VwbWorkspaceAdminControllerTest {

  @Mock private VwbAdminQueryService mockVwbAdminQueryService;
  @Mock private VwbUserManagerClient mockVwbUserManagerClient;
  @Mock private WsmClient mockWsmClient;

  private VwbWorkspaceAdminController controller;

  private VwbWorkspace testWorkspace;
  private List<VwbWorkspace> testWorkspaces;
  private List<UserRole> testCollaborators;
  private List<VwbWorkspaceAuditLog> testAuditLogs;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    controller =
        new VwbWorkspaceAdminController(
            mockVwbAdminQueryService, mockVwbUserManagerClient, mockWsmClient);

    // Set up test data
    testWorkspace = new VwbWorkspace();
    testWorkspace.setId("workspace-uuid-123");
    testWorkspace.setUserFacingId("test-ufid");
    testWorkspace.setDisplayName("Test Workspace");
    testWorkspace.setCreatedBy("creator@example.com");

    testWorkspaces = Collections.singletonList(testWorkspace);

    UserRole collaborator = new UserRole();
    collaborator.setEmail("user@example.com");
    testCollaborators = Collections.singletonList(collaborator);

    VwbWorkspaceAuditLog auditLog = new VwbWorkspaceAuditLog();
    auditLog.setWorkspaceId("workspace-uuid-123");
    auditLog.setChangeType("CREATE_WORKSPACE");
    testAuditLogs = Collections.singletonList(auditLog);
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_Creator() {
    when(mockVwbAdminQueryService.queryVwbWorkspacesByCreator("creator@example.com"))
        .thenReturn(testWorkspaces);

    ResponseEntity<VwbWorkspaceListResponse> response =
        controller.getVwbWorkspacesBySearchParam("CREATOR", "creator@example.com");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getItems()).hasSize(1);
    assertThat(response.getBody().getItems().get(0).getCreatedBy())
        .isEqualTo("creator@example.com");
    verify(mockVwbAdminQueryService).queryVwbWorkspacesByCreator("creator@example.com");
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_UserFacingId() {
    when(mockVwbAdminQueryService.queryVwbWorkspacesByUserFacingId("test-ufid"))
        .thenReturn(testWorkspaces);

    ResponseEntity<VwbWorkspaceListResponse> response =
        controller.getVwbWorkspacesBySearchParam("USER_FACING_ID", "test-ufid");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getItems()).hasSize(1);
    verify(mockVwbAdminQueryService).queryVwbWorkspacesByUserFacingId("test-ufid");
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_Id() {
    when(mockVwbAdminQueryService.queryVwbWorkspacesByWorkspaceId("workspace-uuid-123"))
        .thenReturn(testWorkspaces);

    ResponseEntity<VwbWorkspaceListResponse> response =
        controller.getVwbWorkspacesBySearchParam("ID", "workspace-uuid-123");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getItems()).hasSize(1);
    verify(mockVwbAdminQueryService).queryVwbWorkspacesByWorkspaceId("workspace-uuid-123");
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_Name() {
    when(mockVwbAdminQueryService.queryVwbWorkspacesByName("Test Workspace"))
        .thenReturn(testWorkspaces);

    ResponseEntity<VwbWorkspaceListResponse> response =
        controller.getVwbWorkspacesBySearchParam("NAME", "Test Workspace");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getItems()).hasSize(1);
    verify(mockVwbAdminQueryService).queryVwbWorkspacesByName("Test Workspace");
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_Shared() {
    when(mockVwbAdminQueryService.queryVwbWorkspacesByShareActivity("user@example.com"))
        .thenReturn(testWorkspaces);

    ResponseEntity<VwbWorkspaceListResponse> response =
        controller.getVwbWorkspacesBySearchParam("SHARED", "user@example.com");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getItems()).hasSize(1);
    verify(mockVwbAdminQueryService).queryVwbWorkspacesByShareActivity("user@example.com");
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_GcpProjectId() {
    when(mockVwbAdminQueryService.queryVwbWorkspaceByGcpProjectId("test-gcp-project"))
        .thenReturn(testWorkspaces);

    ResponseEntity<VwbWorkspaceListResponse> response =
        controller.getVwbWorkspacesBySearchParam("GCP_PROJECT_ID", "test-gcp-project");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getItems()).hasSize(1);
    verify(mockVwbAdminQueryService).queryVwbWorkspaceByGcpProjectId("test-gcp-project");
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_InvalidParamType() {
    assertThrows(
        BadRequestException.class,
        () -> controller.getVwbWorkspacesBySearchParam("INVALID_PARAM", "test"));
  }

  @Test
  public void testGetVwbWorkspacesBySearchParam_NullParamType() {
    assertThrows(
        BadRequestException.class, () -> controller.getVwbWorkspacesBySearchParam(null, "test"));
  }

  @Test
  public void testGetVwbWorkspaceAdminView_Success() {
    when(mockVwbAdminQueryService.queryVwbWorkspacesByUserFacingId("test-ufid"))
        .thenReturn(testWorkspaces);
    when(mockVwbAdminQueryService.queryVwbWorkspaceCollaboratorsByUserFacingId("test-ufid"))
        .thenReturn(testCollaborators);

    ResponseEntity<VwbWorkspaceAdminView> response =
        controller.getVwbWorkspaceAdminView("test-ufid");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getWorkspace()).isNotNull();
    assertThat(response.getBody().getWorkspace().getUserFacingId()).isEqualTo("test-ufid");
    assertThat(response.getBody().getCollaborators()).hasSize(1);
    verify(mockVwbAdminQueryService).queryVwbWorkspacesByUserFacingId("test-ufid");
    verify(mockVwbAdminQueryService).queryVwbWorkspaceCollaboratorsByUserFacingId("test-ufid");
  }

  @Test
  public void testGetVwbWorkspaceAdminView_NotFound() {
    when(mockVwbAdminQueryService.queryVwbWorkspacesByUserFacingId("nonexistent-ufid"))
        .thenReturn(Collections.emptyList());

    assertThrows(
        NotFoundException.class, () -> controller.getVwbWorkspaceAdminView("nonexistent-ufid"));
  }

  @Test
  public void testGetVwbWorkspaceAuditLogs() {
    when(mockVwbAdminQueryService.queryVwbWorkspaceActivity("workspace-uuid-123"))
        .thenReturn(testAuditLogs);

    ResponseEntity<List<VwbWorkspaceAuditLog>> response =
        controller.getVwbWorkspaceAuditLogs("workspace-uuid-123");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).getChangeType()).isEqualTo("CREATE_WORKSPACE");
    verify(mockVwbAdminQueryService).queryVwbWorkspaceActivity("workspace-uuid-123");
  }

  @Test
  public void testEnableAccessOnDemandByUserFacingId() {
    VwbAodRequest request = new VwbAodRequest();
    request.setReason("Need to delete workspace");

    ResponseEntity<Void> response =
        controller.enableAccessOnDemandByUserFacingId("test-ufid", request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(mockVwbUserManagerClient)
        .workspaceAccessOnDemandByUserFacingId("test-ufid", "Need to delete workspace");
  }

  @Test
  public void testGetVwbWorkspaceResources() {
    org.pmiops.workbench.wsmanager.model.ResourceList mockResourceList =
        new org.pmiops.workbench.wsmanager.model.ResourceList();
    mockResourceList.setResources(Arrays.asList(new Object(), new Object())); // Mock resources

    when(mockWsmClient.enumerateWorkspaceResources("workspace-uuid-123"))
        .thenReturn(mockResourceList);

    ResponseEntity<org.pmiops.workbench.model.InlineResponse200> response =
        controller.getVwbWorkspaceResources("workspace-uuid-123");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getResources()).hasSize(2);
    verify(mockWsmClient).enumerateWorkspaceResources("workspace-uuid-123");
  }

  @Test
  public void testGetVwbWorkspaceResources_EmptyList() {
    org.pmiops.workbench.wsmanager.model.ResourceList mockResourceList =
        new org.pmiops.workbench.wsmanager.model.ResourceList();
    mockResourceList.setResources(Collections.emptyList());

    when(mockWsmClient.enumerateWorkspaceResources("workspace-uuid-123"))
        .thenReturn(mockResourceList);

    ResponseEntity<org.pmiops.workbench.model.InlineResponse200> response =
        controller.getVwbWorkspaceResources("workspace-uuid-123");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getResources()).isEmpty();
  }

  @Test
  public void testDeleteVwbWorkspaceResource() {
    ResponseEntity<Void> response =
        controller.deleteVwbWorkspaceResource(
            "workspace-uuid-123", "resource-uuid-456", "GCE_INSTANCE");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(mockWsmClient)
        .deleteWorkspaceResource("workspace-uuid-123", "resource-uuid-456", "GCE_INSTANCE");
  }
}
