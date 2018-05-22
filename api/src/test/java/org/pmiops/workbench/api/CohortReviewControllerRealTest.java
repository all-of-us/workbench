package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
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
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.PageFilterType;
import org.pmiops.workbench.model.ParticipantCohortStatuses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class CohortReviewControllerRealTest {

  private static final Long COHORT_ID = 1L;
  private static final Long WORKSPACE_ID = 1L;
  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";
  private static final Long CDR_VERSION_ID = 1L;
  private CohortReview cohortReview;
  private ParticipantCohortStatus participantCohortStatus1;
  private ParticipantCohortStatus participantCohortStatus2;

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
    CohortReviewController.class,
    CohortReviewServiceImpl.class,
    ParticipantCounter.class,
    CohortQueryBuilder.class,
    DomainLookupService.class,
    ReviewTabQueryBuilder.class
  })
  @MockBean({
    WorkspaceService.class,
    BigQueryService.class
  })
  static class Configuration {

    @Bean
    GenderRaceEthnicityConcept getGenderRaceEthnicityConcept() {
      Map<String, Map<Long, String>> concepts = new HashMap<>();
      concepts.put(GenderRaceEthnicityType.RACE.name(),
        new HashMap<Long, String>() {{
          put(TestDemo.ASIAN.getConceptId(), TestDemo.ASIAN.getName());
          put(TestDemo.WHITE.getConceptId(), TestDemo.WHITE.getName()); }});
      concepts.put(GenderRaceEthnicityType.GENDER.name(),
        new HashMap<Long, String>() {{
          put(TestDemo.MALE.getConceptId(), TestDemo.MALE.getName());
          put(TestDemo.FEMALE.getConceptId(), TestDemo.FEMALE.getName()); }});
      concepts.put(GenderRaceEthnicityType.ETHNICITY.name(),
        new HashMap<Long, String>() {{
          put(TestDemo.NOT_HISPANIC.getConceptId(), TestDemo.NOT_HISPANIC.getName()); }});
      return new GenderRaceEthnicityConcept(concepts);
    }
  }

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

  @Before
  public void setUp() {
    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset("dataSetId");
    cdrVersion.setBigqueryProject("projectId");
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersion(cdrVersion);

    workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
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
      .status(CohortStatus.NOT_REVIEWED)
      .participantKey(key1)
      .genderConceptId(TestDemo.MALE.getConceptId())
      .gender(TestDemo.MALE.getName())
      .raceConceptId(TestDemo.ASIAN.getConceptId())
      .race(TestDemo.ASIAN.getName())
      .ethnicityConceptId(TestDemo.NOT_HISPANIC.getConceptId())
      .ethnicity(TestDemo.NOT_HISPANIC.getName())
      .birthDate(new java.sql.Date(today.getTime()));
    participantCohortStatus2 = new ParticipantCohortStatus()
      .status(CohortStatus.NOT_REVIEWED)
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
    when(workspaceService.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)).thenReturn(workspace);

    PageFilterRequest filter = new ParticipantCohortStatuses();
    org.pmiops.workbench.model.CohortReview actualReview =
      cohortReviewController.getParticipantCohortStatuses(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      COHORT_ID,
      CDR_VERSION_ID,
      filter).getBody();

    org.pmiops.workbench.model.CohortReview expectedReview =
      getCohortReview(cohortReview, Arrays.asList(participantCohortStatus1, participantCohortStatus2));

    assertThat(actualReview).isEqualTo(expectedReview);
  }

  private org.pmiops.workbench.model.CohortReview
  getCohortReview(CohortReview actualReview, List<ParticipantCohortStatus> participantCohortStatusList) {
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
        .status(participantCohortStatus.getStatus()));
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
        .page(0)
        .pageSize(25)
        .sortOrder("asc")
        .sortColumn("participantId");
  }
}
