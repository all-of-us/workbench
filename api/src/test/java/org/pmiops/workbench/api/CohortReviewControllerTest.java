package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewTabQueryBuilder;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.ParticipantCohortStatuses;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class CohortReviewControllerTest {

  private static final Long COHORT_ID = 1L;
  private static final Long WORKSPACE_ID = 1L;
  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";
  private static final Long CDR_VERSION_ID = 1L;
  private CohortReview cohortReview;
  private ParticipantCohortStatus participantCohortStatus1;
  private ParticipantCohortStatus participantCohortStatus2;
  private Workspace workspace;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  private WorkspaceService workspaceService;

  @Autowired
  private CohortReviewController cohortReviewController;

  private enum TestDemo {
    ASIAN("Asian", 8515),
    WHITE("White", 8527),
    MALE("MALE", 8507),
    FEMALE("FEMALE", 8532),
    NOT_HISPANIC("Not Hispanic or Latino", 38003564);

    private final String name;
    private final long conceptId;

    private TestDemo(String name, long conceptId) {
      this.name = name;
      this.conceptId = conceptId;
    }

    public String getName() {
      return name;
    }

    public long getConceptId() {
      return conceptId;
    }
  }

  @TestConfiguration
  @Import({
    CdrVersionService.class,
    CohortReviewController.class,
    CohortReviewServiceImpl.class,
    ParticipantCounter.class,
    CohortQueryBuilder.class,
    DomainLookupService.class,
    ReviewTabQueryBuilder.class
  })
  @MockBean({
    WorkspaceService.class,
    BigQueryService.class,
    FireCloudService.class
  })
  static class Configuration {

    @Bean
    public GenderRaceEthnicityConcept getGenderRaceEthnicityConcept() {
      Map<String, Map<Long, String>> concepts = new HashMap<>();
      concepts.put(ParticipantCohortStatusColumns.RACE.name(),
        ImmutableMap.of(TestDemo.ASIAN.getConceptId(), TestDemo.ASIAN.getName(),
          TestDemo.WHITE.getConceptId(), TestDemo.WHITE.getName()));
      concepts.put(ParticipantCohortStatusColumns.GENDER.name(),
        ImmutableMap.of(TestDemo.MALE.getConceptId(), TestDemo.MALE.getName(),
          TestDemo.FEMALE.getConceptId(), TestDemo.FEMALE.getName()));
      concepts.put(ParticipantCohortStatusColumns.ETHNICITY.name(),
        ImmutableMap.of(TestDemo.NOT_HISPANIC.getConceptId(), TestDemo.NOT_HISPANIC.getName()));
      return new GenderRaceEthnicityConcept(concepts);
    }
  }

  @Before
  public void setUp() {
    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset("dataSetId");
    cdrVersion.setBigqueryProject("projectId");
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    workspaceDao.save(workspace);

    Cohort cohort = new Cohort();
    cohort.setCohortId(COHORT_ID);
    cohort.setWorkspaceId(WORKSPACE_ID);
    cohortDao.save(cohort);

    Timestamp today = new Timestamp(new Date().getTime());
    cohortReview = cohortReviewDao.save(
      new CohortReview()
      .cohortId(COHORT_ID)
      .cdrVersionId(CDR_VERSION_ID))
      .creationTime(today);

    ParticipantCohortStatusKey key1 = new ParticipantCohortStatusKey()
      .cohortReviewId(cohortReview.getCohortReviewId())
      .participantId(1L);
    ParticipantCohortStatusKey key2 = new ParticipantCohortStatusKey()
      .cohortReviewId(cohortReview.getCohortReviewId())
      .participantId(2L);

    participantCohortStatus1 = new ParticipantCohortStatus()
      .statusEnum(CohortStatus.NOT_REVIEWED)
      .participantKey(key1)
      .genderConceptId(TestDemo.MALE.getConceptId())
      .gender(TestDemo.MALE.getName())
      .raceConceptId(TestDemo.ASIAN.getConceptId())
      .race(TestDemo.ASIAN.getName())
      .ethnicityConceptId(TestDemo.NOT_HISPANIC.getConceptId())
      .ethnicity(TestDemo.NOT_HISPANIC.getName())
      .birthDate(new java.sql.Date(today.getTime()));
    participantCohortStatus2 = new ParticipantCohortStatus()
      .statusEnum(CohortStatus.NOT_REVIEWED)
      .participantKey(key2)
      .genderConceptId(TestDemo.FEMALE.getConceptId())
      .gender(TestDemo.FEMALE.getName())
      .raceConceptId(TestDemo.WHITE.getConceptId())
      .race(TestDemo.WHITE.getName())
      .ethnicityConceptId(TestDemo.NOT_HISPANIC.getConceptId())
      .ethnicity(TestDemo.NOT_HISPANIC.getName())
      .birthDate(new java.sql.Date(today.getTime()));

    participantCohortStatusDao.save(participantCohortStatus1);
    participantCohortStatusDao.save(participantCohortStatus2);
  }

  @Test
  public void getParticipantCohortStatuses() throws Exception {
    int page = 0;
    int pageSize = 25;
    org.pmiops.workbench.model.CohortReview expectedReview1 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus1, participantCohortStatus2),
        page,
        pageSize,
        SortOrder.DESC,
        ParticipantCohortStatusColumns.STATUS);
    org.pmiops.workbench.model.CohortReview expectedReview2 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus2, participantCohortStatus1),
        page,
        pageSize,
        SortOrder.DESC,
        ParticipantCohortStatusColumns.PARTICIPANTID);
    org.pmiops.workbench.model.CohortReview expectedReview3 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus1, participantCohortStatus2),
        page,
        pageSize,
        SortOrder.ASC,
        ParticipantCohortStatusColumns.STATUS);
    org.pmiops.workbench.model.CohortReview expectedReview4 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus1, participantCohortStatus2),
        page,
        pageSize,
        SortOrder.ASC,
        ParticipantCohortStatusColumns.PARTICIPANTID);

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME, WorkspaceAccessLevel.READER)).thenReturn(workspace);

    assertParticipantCohortStatuses(expectedReview1, page, pageSize, SortOrder.DESC, ParticipantCohortStatusColumns.STATUS);
    assertParticipantCohortStatuses(expectedReview2, page, pageSize, SortOrder.DESC, ParticipantCohortStatusColumns.PARTICIPANTID);
    assertParticipantCohortStatuses(expectedReview3, null, null, null, ParticipantCohortStatusColumns.STATUS);
    assertParticipantCohortStatuses(expectedReview4, null, null, SortOrder.ASC, null);
    assertParticipantCohortStatuses(expectedReview4, null, pageSize, null, null);
    assertParticipantCohortStatuses(expectedReview4, page, null, null, null);
    assertParticipantCohortStatuses(expectedReview4, null, null, null, null);
  }

  /**
   * Helper method to assert results for
   * {@link CohortReviewController#getParticipantCohortStatuses(String, String, Long, Long, PageFilterRequest)}.
   *
   * @param expectedReview
   * @param page
   * @param pageSize
   * @param sortOrder
   * @param sortColumn
   */
  private void assertParticipantCohortStatuses(org.pmiops.workbench.model.CohortReview expectedReview,
                                               Integer page,
                                               Integer pageSize,
                                               SortOrder sortOrder,
                                               ParticipantCohortStatusColumns sortColumn) {
    org.pmiops.workbench.model.CohortReview actualReview =
      cohortReviewController.getParticipantCohortStatuses(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      COHORT_ID,
      CDR_VERSION_ID,
        new ParticipantCohortStatuses()
          .sortColumn(sortColumn)
          .page(page)
          .pageSize(pageSize)
          .sortOrder(sortOrder)).getBody();

    assertThat(actualReview).isEqualTo(expectedReview);
  }

  private org.pmiops.workbench.model.CohortReview createCohortReview(CohortReview actualReview,
                                                                     List<ParticipantCohortStatus> participantCohortStatusList,
                                                                     Integer page,
                                                                     Integer pageSize,
                                                                     SortOrder sortOrder,
                                                                     ParticipantCohortStatusColumns sortColumn) {
    List<org.pmiops.workbench.model.ParticipantCohortStatus> newParticipantCohortStatusList = new ArrayList<>();
    for (ParticipantCohortStatus participantCohortStatus : participantCohortStatusList) {
      newParticipantCohortStatusList.add(new org.pmiops.workbench.model.ParticipantCohortStatus()
        .birthDate(participantCohortStatus.getBirthDate().toString())
        .ethnicityConceptId(participantCohortStatus.getEthnicityConceptId())
        .ethnicity(participantCohortStatus.getEthnicity())
        .genderConceptId(participantCohortStatus.getGenderConceptId())
        .gender(participantCohortStatus.getGender())
        .participantId(participantCohortStatus.getParticipantKey().getParticipantId())
        .raceConceptId(participantCohortStatus.getRaceConceptId())
        .race(participantCohortStatus.getRace())
        .status(participantCohortStatus.getStatusEnum()));
    }
    return new org.pmiops.workbench.model.CohortReview()
        .cohortReviewId(actualReview.getCohortReviewId())
        .cohortId(actualReview.getCohortId())
        .cdrVersionId(actualReview.getCdrVersionId())
        .creationTime(actualReview.getCreationTime().toString())
        .matchedParticipantCount(actualReview.getMatchedParticipantCount())
        .reviewSize(actualReview.getReviewSize())
        .reviewedCount(actualReview.getReviewedCount())
        .participantCohortStatuses(newParticipantCohortStatusList)
        .page(page)
        .pageSize(pageSize)
        .sortOrder(sortOrder.toString())
        .sortColumn(sortColumn.name());
  }
}
