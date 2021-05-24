package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortAnnotationDefinitionService;
import org.pmiops.workbench.cohortreview.CohortAnnotationDefinitionServiceImpl;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.mapper.CohortAnnotationDefinitionMapper;
import org.pmiops.workbench.cohortreview.mapper.CohortAnnotationDefinitionMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapper;
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
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortAnnotationDefinitionControllerTest {

  private static final String NAMESPACE = "aou-test";
  private static final String NAME = "test";
  private static final String NAMESPACE2 = "aou-test";
  private static final String NAME2 = "test2";
  private static final String EXISTING_COLUMN_NAME = "testing";
  private static final String NEW_COLUMN_NAME = "new_column";
  private DbCohort cohort;
  private DbCohortAnnotationDefinition dbCohortAnnotationDefinition;
  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  @Autowired private CohortDao cohortDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private CohortAnnotationDefinitionMapper cohortAnnotationDefinitionMapper;
  @Mock private BigQueryService bigQueryService;
  @Mock private CohortBuilderService cohortBuilderService;
  @Mock private CohortReviewDao cohortReviewDao;
  @Mock private CohortReviewMapper cohortReviewMapper;
  @Mock private CohortQueryBuilder cohortQueryBuilder;
  @Mock private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  @Mock private ParticipantCohortAnnotationMapper participantCohortAnnotationMapper;
  @Mock private ParticipantCohortStatusDao participantCohortStatusDao;
  @Mock private ParticipantCohortStatusMapper participantCohortStatusMapper;
  @Mock private ReviewQueryBuilder reviewQueryBuilder;
  @Mock private WorkspaceAuthService workspaceAuthService;
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private CohortAnnotationDefinitionController cohortAnnotationDefinitionController;
  private DbWorkspace workspace;
  private DbWorkspace workspace2;

  @TestConfiguration
  @Import({CohortAnnotationDefinitionMapperImpl.class, CommonMappers.class})
  @MockBean({Clock.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    CohortReviewService cohortReviewService =
        new CohortReviewServiceImpl(
            bigQueryService,
            cohortAnnotationDefinitionDao,
            cohortBuilderService,
            cohortDao,
            cohortReviewDao,
            cohortReviewMapper,
            cohortQueryBuilder,
            participantCohortAnnotationDao,
            participantCohortAnnotationMapper,
            participantCohortStatusDao,
            participantCohortStatusMapper,
            reviewQueryBuilder,
            CLOCK);
    CohortAnnotationDefinitionService cohortAnnotationDefinitionService =
        new CohortAnnotationDefinitionServiceImpl(
            cohortAnnotationDefinitionDao, cohortAnnotationDefinitionMapper);
    cohortAnnotationDefinitionController =
        new CohortAnnotationDefinitionController(
            cohortAnnotationDefinitionService, cohortReviewService, workspaceAuthService);

    workspace = new DbWorkspace();
    workspace.setWorkspaceNamespace(NAMESPACE);
    workspace.setFirecloudName(NAME);
    workspace.setWorkspaceId(1L);
    workspaceDao.save(workspace);

    workspace2 = new DbWorkspace();
    workspace2.setWorkspaceNamespace(NAMESPACE2);
    workspace2.setFirecloudName(NAME2);
    workspace2.setWorkspaceId(2L);
    workspaceDao.save(workspace2);

    cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohortDao.save(cohort);

    dbCohortAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .cohortId(cohort.getCohortId())
            .annotationTypeEnum(AnnotationType.STRING)
            .columnName(EXISTING_COLUMN_NAME)
            .version(0);
    cohortAnnotationDefinitionDao.save(dbCohortAnnotationDefinition);
  }

  @Test
  public void createCohortAnnotationDefinition_BadCohortId() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

    try {
      cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
          NAMESPACE, NAME, 0L, new CohortAnnotationDefinition().columnName("column_name"));
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertThat(e.getMessage()).isEqualTo("Not Found: No Cohort exists for cohortId: " + 0L);
    }
  }

  @Test
  public void createCohortAnnotationDefinition_NameConflict() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

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
      assertThat(e.getMessage())
          .isEqualTo(
              "Conflict: Cohort Annotation Definition name exists for: "
                  + dbCohortAnnotationDefinition.getColumnName());
    }
  }

  @Test
  public void createCohortAnnotationDefinition() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

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
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  public void createCohortAnnotationDefinitionEnumValues() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

    CohortAnnotationDefinition request =
        new CohortAnnotationDefinition()
            .cohortId(cohort.getCohortId())
            .columnName(NEW_COLUMN_NAME)
            .annotationType(AnnotationType.ENUM)
            .enumValues(ImmutableList.of("value"))
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
            .enumValues(ImmutableList.of("value"))
            .etag(Etags.fromVersion(0));
    assertThat(response).isEqualTo(expectedResponse);
  }

  @Test
  public void updateCohortAnnotationDefinition_BadCohortId() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

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
      assertThat(e.getMessage()).isEqualTo("Not Found: No Cohort exists for cohortId: " + 99L);
    }
  }

  @Test
  public void updateCohortAnnotationDefinition_BadAnnotationDefinitionId() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

    CohortAnnotationDefinition request = new CohortAnnotationDefinition().columnName("ignore");

    try {
      cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
          NAMESPACE, NAME, cohort.getCohortId(), 99L, request);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertThat(e.getMessage())
          .isEqualTo(
              "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: "
                  + 99L);
    }
  }

  @Test
  public void updateCohortAnnotationDefinition_NameConflict() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

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
      assertThat(e.getMessage())
          .isEqualTo(
              "Conflict: Cohort Annotation Definition name exists for: " + EXISTING_COLUMN_NAME);
    }
  }

  @Test
  public void updateCohortAnnotationDefinition() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

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

    assertThat(responseDefinition).isEqualTo(expectedResponse);
  }

  @Test
  public void deleteCohortAnnotationDefinition_BadCohortId() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

    try {
      cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
          NAMESPACE, NAME, 99L, dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertThat(e.getMessage()).isEqualTo("Not Found: No Cohort exists for cohortId: " + 99L);
    }
  }

  @Test
  public void deleteCohortAnnotationDefinition_BadAnnotationDefinitionId() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

    try {
      cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
          NAMESPACE, NAME, cohort.getCohortId(), 99L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertThat(e.getMessage())
          .isEqualTo(
              "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: "
                  + 99L);
    }
  }

  @Test(expected = NotFoundException.class)
  public void deleteCohortAnnotationDefinitionWrongWorkspace() {
    setupWorkspace2ServiceMock(WorkspaceAccessLevel.WRITER);

    cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
        NAMESPACE2,
        NAME2,
        cohort.getCohortId(),
        dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
  }

  @Test(expected = NotFoundException.class)
  public void getCohortAnnotationDefinitionWrongWorkspace() {
    setupWorkspace2ServiceMock(WorkspaceAccessLevel.READER);

    cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
        NAMESPACE2,
        NAME2,
        cohort.getCohortId(),
        dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
  }

  @Test(expected = NotFoundException.class)
  public void updateCohortAnnotationDefinitionWrongWorkspace() {
    setupWorkspace2ServiceMock(WorkspaceAccessLevel.WRITER);

    CohortAnnotationDefinition request =
        new CohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .columnName(NEW_COLUMN_NAME)
            .etag(Etags.fromVersion(0))
            .cohortId(cohort.getCohortId());

    cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
        NAMESPACE2,
        NAME2,
        cohort.getCohortId(),
        dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId(),
        request);
  }

  @Test
  public void deleteCohortAnnotationDefinition() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.WRITER);

    EmptyResponse response =
        cohortAnnotationDefinitionController
            .deleteCohortAnnotationDefinition(
                NAMESPACE,
                NAME,
                cohort.getCohortId(),
                dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId())
            .getBody();

    assertThat(response).isEqualTo(new EmptyResponse());
  }

  @Test
  public void getCohortAnnotationDefinition_NotFoundCohort() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.READER);

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
          NAMESPACE, NAME, 99L, dbCohortAnnotationDefinition.getCohortAnnotationDefinitionId());
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertThat(e.getMessage()).isEqualTo("Not Found: No Cohort exists for cohortId: " + 99L);
    }
  }

  @Test
  public void getCohortAnnotationDefinition_NotFoundAnnotationDefinition() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.READER);

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
          NAMESPACE, NAME, cohort.getCohortId(), 99L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertThat(e.getMessage())
          .isEqualTo(
              "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: "
                  + 99L);
    }
  }

  @Test
  public void getCohortAnnotationDefinition() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.READER);

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

    assertThat(responseDefinition).isEqualTo(expectedResponse);
  }

  @Test
  public void getCohortAnnotationDefinitions_NotFoundCohort() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.READER);

    try {
      cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(NAMESPACE, NAME, 99L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException e) {
      assertThat(e.getMessage()).isEqualTo("Not Found: No Cohort exists for cohortId: " + 99L);
    }
  }

  @Test
  public void getCohortAnnotationDefinitions() {
    setupWorkspaceServiceMock(WorkspaceAccessLevel.READER);

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

    assertThat(responseDefinition.getItems().size()).isEqualTo(1);
    assertThat(responseDefinition.getItems().get(0)).isEqualTo(expectedResponse);
  }

  private void setupWorkspaceServiceMock(WorkspaceAccessLevel workspaceAccessLevel) {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            NAMESPACE, NAME, workspaceAccessLevel))
        .thenReturn(workspace);
  }

  private void setupWorkspace2ServiceMock(WorkspaceAccessLevel workspaceAccessLevel) {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            NAMESPACE2, NAME2, workspaceAccessLevel))
        .thenReturn(workspace2);
  }
}
