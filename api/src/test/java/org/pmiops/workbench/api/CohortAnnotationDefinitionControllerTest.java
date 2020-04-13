package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cohortreview.CohortAnnotationDefinitionService;
import org.pmiops.workbench.cohortreview.CohortAnnotationDefinitionServiceImpl;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.mappers.CohortAnnotationDefinitionMapper;
import org.pmiops.workbench.cohortreview.mappers.CohortAnnotationDefinitionMapperImpl;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortAnnotationDefinitionControllerTest {

  private static String NAMESPACE = "aou-test";
  private static String NAME = "test";
  private static String EXISTING_COLUMN_NAME = "testing";
  private static String NEW_COLUMN_NAME = "new_column";
  private DbCohort cohort;
  private DbCohortAnnotationDefinition dbCohortAnnotationDefinition;
  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  @Autowired private CohortDao cohortDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private CohortAnnotationDefinitionMapper cohortAnnotationDefinitionMapper;
  @Mock private CohortReviewDao cohortReviewDao;
  @Mock private ParticipantCohortStatusDao participantCohortStatusDao;
  @Mock private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  @Mock private WorkspaceService workspaceService;
  private CohortAnnotationDefinitionController cohortAnnotationDefinitionController;

  @TestConfiguration
  @Import({CohortAnnotationDefinitionMapperImpl.class, CommonMappers.class})
  static class Configuration {}

  @Before
  public void setUp() {
    CohortReviewService cohortReviewService =
        new CohortReviewServiceImpl(
            cohortReviewDao,
            cohortDao,
            participantCohortStatusDao,
            participantCohortAnnotationDao,
            cohortAnnotationDefinitionDao,
            workspaceService);
    CohortAnnotationDefinitionService cohortAnnotationDefinitionService =
        new CohortAnnotationDefinitionServiceImpl(
            cohortAnnotationDefinitionDao, cohortAnnotationDefinitionMapper);
    cohortAnnotationDefinitionController =
        new CohortAnnotationDefinitionController(
            cohortAnnotationDefinitionService, cohortReviewService, workspaceService);

    DbWorkspace workspace = new DbWorkspace();
    workspace.setWorkspaceNamespace(NAMESPACE);
    workspace.setFirecloudName(NAME);
    workspaceDao.save(workspace);

    cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohortDao.save(cohort);

    dbCohortAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .cohortId(cohort.getCohortId())
            .annotationTypeEnum(AnnotationType.STRING)
            .columnName(EXISTING_COLUMN_NAME)
            .version(0);
    SortedSet enumValues = new TreeSet();
    cohortAnnotationDefinitionDao.save(dbCohortAnnotationDefinition);
  }

  @Test
  public void createCohortAnnotationDefinition_BadCohortId() {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
          NAMESPACE, NAME, 0L, new CohortAnnotationDefinition().columnName("column_name"));
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + 0L, e.getMessage());
    }
  }

  @Test
  public void createCohortAnnotationDefinition_NameConflict() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinition request =
        new CohortAnnotationDefinition()
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
  public void createCohortAnnotationDefinition() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinition request =
        new CohortAnnotationDefinition()
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.STRING)
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));

    CohortAnnotationDefinition response =
        cohortAnnotationDefinitionController
            .createCohortAnnotationDefinition(NAMESPACE, NAME, cohort.getCohortId(), request)
            .getBody();
    CohortAnnotationDefinition expectedResponse =
        new CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(response.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.STRING)
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));
    assertEquals(expectedResponse, response);
  }

  @Test
  public void createCohortAnnotationDefinitionEnumValues() {
    setupWorkspaceServiceMock();

    List<String> enumValues = Arrays.asList("value");
    CohortAnnotationDefinition request =
        new CohortAnnotationDefinition()
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.ENUM)
            .enumValues(enumValues)
            .etag(Etags.fromVersion(0));

    CohortAnnotationDefinition response =
        cohortAnnotationDefinitionController
            .createCohortAnnotationDefinition(NAMESPACE, NAME, cohort.getCohortId(), request)
            .getBody();
    CohortAnnotationDefinition expectedResponse =
        new CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(response.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.ENUM)
            .enumValues(enumValues)
            .etag(Etags.fromVersion(0));
    assertEquals(expectedResponse, response);
  }

  @Test
  public void updateCohortAnnotationDefinition_BadCohortId() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinition request = new CohortAnnotationDefinition().columnName("ignore");

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
  public void updateCohortAnnotationDefinition_BadAnnotationDefinitionId() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinition request = new CohortAnnotationDefinition().columnName("ignore");

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
  public void updateCohortAnnotationDefinition_NameConflict() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinition request =
        new CohortAnnotationDefinition()
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
  public void updateCohortAnnotationDefinition() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinition request =
        new CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .columnName(NEW_COLUMN_NAME)
            .etag(Etags.fromVersion(0))
            .cohortId(cohort.getCohortId());

    CohortAnnotationDefinition expectedResponse =
        new CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.STRING)
            .enumValues(new ArrayList<>())
            .etag(Etags.fromVersion(0));

    CohortAnnotationDefinition responseDefinition =
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
  public void deleteCohortAnnotationDefinition_BadCohortId() {
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
  public void deleteCohortAnnotationDefinition_BadAnnotationDefinitionId() {
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
  public void deleteCohortAnnotationDefinition() {
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
  public void getCohortAnnotationDefinition_NotFoundCohort() {
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
  public void getCohortAnnotationDefinition_NotFoundAnnotationDefinition() {
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
  public void getCohortAnnotationDefinition() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinition responseDefinition =
        cohortAnnotationDefinitionController
            .getCohortAnnotationDefinition(
                NAMESPACE,
                NAME,
                cohort.getCohortId(),
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .getBody();

    CohortAnnotationDefinition expectedResponse =
        new CohortAnnotationDefinition()
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
  public void getCohortAnnotationDefinitions_NotFoundCohort() {
    setupWorkspaceServiceMock();

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(NAMESPACE, NAME, 99L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.getMessage());
    }
  }

  @Test
  public void getCohortAnnotationDefinitions() {
    setupWorkspaceServiceMock();

    CohortAnnotationDefinitionListResponse responseDefinition =
        cohortAnnotationDefinitionController
            .getCohortAnnotationDefinitions(NAMESPACE, NAME, cohort.getCohortId())
            .getBody();

    CohortAnnotationDefinition expectedResponse =
        new CohortAnnotationDefinition()
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
    when(workspaceService.enforceWorkspaceAccessLevel(NAMESPACE, NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(WorkspaceAccessLevel.OWNER);
  }
}
