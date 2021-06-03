package org.pmiops.workbench.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.NotebooksService;
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
public class WorkspaceAdminControllerTest extends SpringTest {

  private static final long DB_WORKSPACE_ID = 2222L;
  private static final String FIRECLOUD_WORKSPACE_CREATOR_USERNAME = "jay@allofus.biz";
  private static final String WORKSPACE_NAME = "Gone with the Wind";
  private static final String DB_WORKSPACE_FIRECLOUD_NAME = "gonewiththewind";
  private static final String WORKSPACE_NAMESPACE = "aou-rw-12345";
  private static final String NONSENSE_NAMESPACE = "wharrgarbl_wharrgarbl";
  private static final int QUERY_LIMIT = 50;
  private static final String ACTION_ID = "b937413e-ff66-4e7b-a639-f7947730b2c0";
  private static final WorkspaceAuditLogQueryResponse QUERY_RESPONSE =
      new WorkspaceAuditLogQueryResponse()
          .logEntries(
              ImmutableList.of(
                  new AuditLogEntry()
                      .actionId(ACTION_ID)
                      .actionType("CREATE")
                      .agentId(1111L)
                      .agentType("ADMINISTRATOR")
                      .agentUsername(FIRECLOUD_WORKSPACE_CREATOR_USERNAME)
                      .eventTime(
                          OffsetDateTime.parse("2020-02-10T01:20+02:00").toInstant().toEpochMilli())
                      .newValue("true")
                      .previousValue(null)
                      .targetId(DB_WORKSPACE_ID)
                      .targetProperty("approved")
                      .targetType("WORKSPACE")))
          .query("select foo from bar")
          .workspaceDatabaseId(DB_WORKSPACE_ID);
  private static final WorkspaceAuditLogQueryResponse EMPTY_QUERY_RESPONSE =
      new WorkspaceAuditLogQueryResponse()
          .query("select foo from bar")
          .workspaceDatabaseId(DB_WORKSPACE_ID);
  private static final DateTime DEFAULT_AFTER_INCLUSIVE = DateTime.parse("2001-02-14T01:20+02:00");
  private static final DateTime DEFAULT_BEFORE_EXCLUSIVE = DateTime.parse("2020-05-01T01:20+02:00");

  @MockBean private ActionAuditQueryService mockActionAuditQueryService;
  @MockBean private CloudMonitoringService mockCloudMonitoringService;
  @MockBean private FireCloudService mockFirecloudService;
  @MockBean private LeonardoNotebooksClient mockLeonardoNotebooksClient;
  @MockBean private WorkspaceAdminService mockWorkspaceAdminService;
  @MockBean private WorkspaceService mockWorkspaceService;

  @Autowired private WorkspaceAdminController workspaceAdminController;

  @TestConfiguration
  @Import({
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
    CloudStorageClient.class,
    NotebooksService.class,
    ConceptSetService.class,
    CohortService.class,
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    final TestMockFactory testMockFactory = new TestMockFactory();

    when(mockWorkspaceAdminService.getFirstWorkspaceByNamespace(anyString()))
        .thenReturn(Optional.empty());

    final Workspace workspace =
        testMockFactory.createWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME);
    final DbWorkspace dbWorkspace =
        TestMockFactory.createDbWorkspaceStub(workspace, DB_WORKSPACE_ID);
    when(mockWorkspaceAdminService.getFirstWorkspaceByNamespace(WORKSPACE_NAMESPACE))
        .thenReturn(Optional.of(dbWorkspace));

    final UserRole collaborator =
        new UserRole().email("test@test.test").role(WorkspaceAccessLevel.WRITER);
    final List<UserRole> collaborators = ImmutableList.of(collaborator);
    when(mockWorkspaceService.getFirecloudUserRoles(
            WORKSPACE_NAMESPACE, DB_WORKSPACE_FIRECLOUD_NAME))
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
        testMockFactory.createLeonardoListRuntimesResponse();
    List<LeonardoListRuntimeResponse> runtimes = ImmutableList.of(leonardoListRuntimeResponse);
    when(mockLeonardoNotebooksClient.listRuntimesByProjectAsService(WORKSPACE_NAMESPACE))
        .thenReturn(runtimes);

    FirecloudWorkspace fcWorkspace =
        TestMockFactory.createFirecloudWorkspace(
            WORKSPACE_NAMESPACE, DB_WORKSPACE_FIRECLOUD_NAME, FIRECLOUD_WORKSPACE_CREATOR_USERNAME);
    FirecloudWorkspaceResponse fcWorkspaceResponse =
        new FirecloudWorkspaceResponse().workspace(fcWorkspace);
    when(mockFirecloudService.getWorkspaceAsService(
            WORKSPACE_NAMESPACE, DB_WORKSPACE_FIRECLOUD_NAME))
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
