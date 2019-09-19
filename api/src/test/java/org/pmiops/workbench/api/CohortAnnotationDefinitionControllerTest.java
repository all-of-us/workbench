package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Provider;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortAnnotationDefinitionControllerTest {

  private static String NAMESPACE = "aou-test";
  private static String NAME = "test";
  private static String EXISTING_COLUMN_NAME = "testing";
  private static String NEW_COLUMN_NAME = "new_column";
  private Workspace workspace;
  private Cohort cohort;
  private CohortAnnotationDefinition dbCohortAnnotationDefinition;
  @Autowired CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  @Autowired CohortDao cohortDao;
  @Mock Provider<User> userProvider;
  @Autowired WorkspaceDao workspaceDao;
  @Mock WorkspaceService workspaceService;
  CohortAnnotationDefinitionController cohortAnnotationDefinitionController;

  @Before
  public void setUp() {
    cohortAnnotationDefinitionController =
        new CohortAnnotationDefinitionController(
            cohortAnnotationDefinitionDao, cohortDao, userProvider, workspaceService);

    workspace = new Workspace();
    workspace.setWorkspaceNamespace(NAMESPACE);
    workspace.setFirecloudName(NAME);
    workspaceDao.save(workspace);

    cohort = new Cohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohortDao.save(cohort);

    dbCohortAnnotationDefinition =
        new CohortAnnotationDefinition()
            .cohortId(cohort.getCohortId())
            .annotationTypeEnum(AnnotationType.STRING)
            .columnName(EXISTING_COLUMN_NAME)
            .version(0);
    cohortAnnotationDefinitionDao.save(dbCohortAnnotationDefinition);
  }

  @Test
  public void createCohortAnnotationDefinition_BadCohortId() throws Exception {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
          NAMESPACE, NAME, 0L, new org.pmiops.workbench.model.CohortAnnotationDefinition());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + 0L, e.getMessage());
    }
  }

  @Test
  public void createCohortAnnotationDefinition_BadWorkspace() throws Exception {
    setupBadWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          new org.pmiops.workbench.model.CohortAnnotationDefinition());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No workspace matching workspaceNamespace: "
              + NAMESPACE
              + ", workspaceId: "
              + NAME,
          e.getMessage());
    }
  }

  @Test
  public void createCohortAnnotationDefinition_NameConflict() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition request =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .columnName(dbCohortAnnotationDefinition.getColumnName())
            .annotationType(AnnotationType.STRING)
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));

    try {
      cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
          NAMESPACE, NAME, cohort.getCohortId(), request);
      fail("Should have thrown a ConflictException!");
    } catch (ConflictException e) {
      assertEquals(
          "Conflict: Cohort Annotation Definition name exists for: "
              + dbCohortAnnotationDefinition.getColumnName(),
          e.getMessage());
    }
  }

  @Test
  public void createCohortAnnotationDefinition() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition request =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.STRING)
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));

    org.pmiops.workbench.model.CohortAnnotationDefinition response =
        cohortAnnotationDefinitionController
            .createCohortAnnotationDefinition(NAMESPACE, NAME, cohort.getCohortId(), request)
            .getBody();
    org.pmiops.workbench.model.CohortAnnotationDefinition expectedResponse =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(response.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.STRING)
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));
    assertEquals(expectedResponse, response);
  }

  @Test
  public void updateCohortAnnotationDefinition_BadCohortId() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition request =
        new org.pmiops.workbench.model.CohortAnnotationDefinition().columnName("ignore");

    try {
      cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
          NAMESPACE,
          NAME,
          99L,
          dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId(),
          request);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.getMessage());
    }
  }

  @Test
  public void updateCohortAnnotationDefinition_BadWorkspace() throws Exception {
    setupBadWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition request =
        new org.pmiops.workbench.model.CohortAnnotationDefinition().columnName("ignore");

    try {
      cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId(),
          request);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No workspace matching workspaceNamespace: "
              + NAMESPACE
              + ", workspaceId: "
              + NAME,
          e.getMessage());
    }
  }

  @Test
  public void updateCohortAnnotationDefinition_BadAnnotationDefinitionId() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition request =
        new org.pmiops.workbench.model.CohortAnnotationDefinition().columnName("ignore");

    try {
      cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
          NAMESPACE, NAME, cohort.getCohortId(), 99L, request);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: " + 99L,
          e.getMessage());
    }
  }

  @Test
  public void updateCohortAnnotationDefinition_NameConflict() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition request =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .columnName(EXISTING_COLUMN_NAME)
            .etag(Etags.fromVersion(0));

    try {
      cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId(),
          request);
      fail("Should have thrown a ConflictException!");
    } catch (ConflictException e) {
      assertEquals(
          "Conflict: Cohort Annotation Definition name exists for: " + EXISTING_COLUMN_NAME,
          e.getMessage());
    }
  }

  @Test
  public void updateCohortAnnotationDefinition() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition request =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .columnName(NEW_COLUMN_NAME)
            .etag(Etags.fromVersion(0))
            .cohortId(cohort.getCohortId());

    org.pmiops.workbench.model.CohortAnnotationDefinition expectedResponse =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.STRING)
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));

    org.pmiops.workbench.model.CohortAnnotationDefinition responseDefinition =
        cohortAnnotationDefinitionController
            .updateCohortAnnotationDefinition(
                NAMESPACE,
                NAME,
                cohort.getCohortId(),
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId(),
                request)
            .getBody();

    assertEquals(expectedResponse, responseDefinition);
  }

  @Test
  public void deleteCohortAnnotationDefinition_BadCohortId() throws Exception {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
          NAMESPACE, NAME, 99L, dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.getMessage());
    }
  }

  @Test
  public void deleteCohortAnnotationDefinition_BadWorkspace() throws Exception {
    setupBadWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No workspace matching workspaceNamespace: "
              + NAMESPACE
              + ", workspaceId: "
              + NAME,
          e.getMessage());
    }
  }

  @Test
  public void deleteCohortAnnotationDefinition_BadAnnotationDefinitionId() throws Exception {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
          NAMESPACE, NAME, cohort.getCohortId(), 99L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: " + 99L,
          e.getMessage());
    }
  }

  @Test
  public void deleteCohortAnnotationDefinition() throws Exception {
    setupWorkspaceServiceMock();

    EmptyResponse response =
        cohortAnnotationDefinitionController
            .deleteCohortAnnotationDefinition(
                NAMESPACE,
                NAME,
                cohort.getCohortId(),
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .getBody();

    assertEquals(new EmptyResponse(), response);
  }

  @Test
  public void getCohortAnnotationDefinition_NotFoundCohort() throws Exception {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
          NAMESPACE, NAME, 99L, dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.getMessage());
    }
  }

  @Test
  public void getCohortAnnotationDefinition_NotFoundWorkspace() throws Exception {
    setupBadWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
          NAMESPACE,
          NAME,
          cohort.getCohortId(),
          dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No workspace matching workspaceNamespace: "
              + NAMESPACE
              + ", workspaceId: "
              + NAME,
          e.getMessage());
    }
  }

  @Test
  public void getCohortAnnotationDefinition_NotFoundAnnotationDefinition() throws Exception {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
          NAMESPACE, NAME, cohort.getCohortId(), 99L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: " + 99L,
          e.getMessage());
    }
  }

  @Test
  public void getCohortAnnotationDefinition() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinition responseDefinition =
        cohortAnnotationDefinitionController
            .getCohortAnnotationDefinition(
                NAMESPACE,
                NAME,
                cohort.getCohortId(),
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .getBody();

    org.pmiops.workbench.model.CohortAnnotationDefinition expectedResponse =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .annotationType(AnnotationType.STRING)
            .columnName(dbCohortAnnotationDefinition.getColumnName())
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));

    assertEquals(expectedResponse, responseDefinition);
  }

  @Test
  public void getCohortAnnotationDefinitions_NotFoundCohort() throws Exception {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(NAMESPACE, NAME, 99L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.getMessage());
    }
  }

  @Test
  public void getCohortAnnotationDefinitions_NotFoundWorkspace() throws Exception {
    setupBadWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(
          NAMESPACE, NAME, cohort.getCohortId());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals(
          "Not Found: No workspace matching workspaceNamespace: "
              + NAMESPACE
              + ", workspaceId: "
              + NAME,
          e.getMessage());
    }
  }

  @Test
  public void getCohortAnnotationDefinitions() throws Exception {
    setupWorkspaceServiceMock();

    org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse responseDefinition =
        cohortAnnotationDefinitionController
            .getCohortAnnotationDefinitions(NAMESPACE, NAME, cohort.getCohortId())
            .getBody();

    org.pmiops.workbench.model.CohortAnnotationDefinition expectedResponse =
        new org.pmiops.workbench.model.CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .annotationType(AnnotationType.STRING)
            .columnName(dbCohortAnnotationDefinition.getColumnName())
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));

    assertEquals(1, responseDefinition.getItems().size());
    assertEquals(expectedResponse, responseDefinition.getItems().get(0));
  }

  private void setupWorkspaceServiceMock() {
    Workspace mockWorkspace = new Workspace();
    mockWorkspace.setWorkspaceNamespace(NAMESPACE);
    mockWorkspace.setFirecloudName(NAME);
    mockWorkspace.setWorkspaceId(workspace.getWorkspaceId());

    when(workspaceService.enforceWorkspaceAccessLevel(NAMESPACE, NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(WorkspaceAccessLevel.OWNER);
    when(workspaceService.getRequired(NAMESPACE, NAME)).thenReturn(mockWorkspace);
  }

  private void setupBadWorkspaceServiceMock() {
    Workspace mockWorkspace = new Workspace();
    mockWorkspace.setWorkspaceNamespace(NAMESPACE);
    mockWorkspace.setFirecloudName(NAME);
    mockWorkspace.setWorkspaceId(0L);

    when(workspaceService.enforceWorkspaceAccessLevel(NAMESPACE, NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(WorkspaceAccessLevel.OWNER);
    when(workspaceService.getRequired(NAMESPACE, NAME)).thenReturn(mockWorkspace);
  }
}
