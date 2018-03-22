package org.pmiops.workbench.cohorts;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryBaseTest;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.DomainLookupService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.DemoQueryBuilder;
import org.pmiops.workbench.config.ConceptCacheConfiguration;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({DemoQueryBuilder.class, QueryBuilderFactory.class, CohortMaterializationService.class,
        BigQueryService.class, ParticipantCounter.class, DomainLookupService.class,
        QueryBuilderFactory.class, TestJpaConfig.class,
        ConceptCacheConfiguration.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortMaterializationServiceTest extends BigQueryBaseTest {

  @Autowired
  private CohortMaterializationService cohortMaterializationService;

  private CdrVersion cdrVersion = new CdrVersion();
  private CohortReview cohortReview;

  @Autowired
  private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CriteriaDao criteriaDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  private ParticipantCohortStatus makeStatus(long cohortReviewId, long participantId, CohortStatus status) {
    ParticipantCohortStatusKey key = new ParticipantCohortStatusKey();
    key.setCohortReviewId(cohortReviewId);
    key.setParticipantId(participantId);
    ParticipantCohortStatus result = new ParticipantCohortStatus();
    result.setStatus(status);
    result.setParticipantKey(key);
    return result;
  }

  @Before
  public void setUp() {
    cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersion(cdrVersion);

    Criteria icd9CriteriaGroup =
            new Criteria().group(true)
                    .name("group")
                    .selectable(true)
                    .code(SearchRequests.ICD9_GROUP_CODE)
                    .type(SearchRequests.ICD9_TYPE)
                    .parentId(0);
    criteriaDao.save(icd9CriteriaGroup);
    Criteria icd9CriteriaChild =
            new Criteria().group(false)
                    .name("child")
                    .selectable(true)
                    .code(SearchRequests.ICD9_GROUP_CODE + ".1")
                    .type(SearchRequests.ICD9_TYPE)
                    .domainId("Condition")
                    .parentId(icd9CriteriaGroup.getId());
    criteriaDao.save(icd9CriteriaChild);

    Workspace workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspaceDao.save(workspace);

    Cohort cohort = new Cohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort.setName("males");
    cohort.setType("AOU");
    Gson gson = new Gson();
    cohort.setCriteria(gson.toJson(SearchRequests.males()));
    cohortDao.save(cohort);

    Cohort cohort2 = new Cohort();
    cohort2.setWorkspaceId(workspace.getWorkspaceId());
    cohort2.setName("all genders");
    cohort2.setType("AOU");
    cohort2.setCriteria(gson.toJson(SearchRequests.allGenders()));
    cohortDao.save(cohort2);

    cohortReview = new CohortReview();
    cohortReview.setCdrVersionId(cdrVersion.getCdrVersionId());
    cohortReview.setCohortId(cohort2.getCohortId());
    cohortReview.setMatchedParticipantCount(3);
    cohortReview.setReviewedCount(2);
    cohortReview.setReviewSize(3);
    cohortReviewDao.save(cohortReview);

    participantCohortStatusDao.save(makeStatus(cohortReview.getCohortReviewId(), 1L, CohortStatus.INCLUDED));
    participantCohortStatusDao.save(makeStatus(cohortReview.getCohortReviewId(), 2L, CohortStatus.EXCLUDED));
  }

  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
            "person",
            "concept",
            "condition_occurrence");
  }

  @Test
  public void testMaterializeCohortOneMale() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(null,
        SearchRequests.males(),null, 1000, null);
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortICD9Group() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(null,
            SearchRequests.icd9Codes(),null, 1000, null);
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNullStatusFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(),null, 2, null);
    // With a null status filter, everyone is returned.
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNotNull();
    MaterializeCohortResponse response2 = cohortMaterializationService.materializeCohort(null,
        SearchRequests.allGenders(),null, 2, response.getNextPageToken());
    assertPersonIds(response2, 102246L);
    assertThat(response2.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotExcludedFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList
            .of(CohortStatus.NOT_REVIEWED, CohortStatus.INCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW),
        2, null);
    // With a not excluded status filter, ID 2 is not returned.
    assertPersonIds(response, 1L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewJustExcludedFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList.of(CohortStatus.EXCLUDED),
        2, null);
    assertPersonIds(response, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewJustIncludedFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList.of(CohortStatus.INCLUDED),
        2, null);
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewIncludedAndExcludedFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList.of(CohortStatus.EXCLUDED, CohortStatus.INCLUDED),
        2, null);
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewJustNotReviewedFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList.of(CohortStatus.NOT_REVIEWED),
        2, null);
    assertPersonIds(response, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotReviewedAndIncludedFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.NOT_REVIEWED),
        2, null);
    assertPersonIds(response, 1L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotReviewedAndNeedsFurtherReviewFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList.of(CohortStatus.NEEDS_FURTHER_REVIEW,
            CohortStatus.NOT_REVIEWED),
        2, null);
    assertPersonIds(response, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotReviewedAndExcludedFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList.of(CohortStatus.EXCLUDED,
            CohortStatus.NOT_REVIEWED),
        2, null);
    assertPersonIds(response, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewAllFilter() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList
            .of(CohortStatus.EXCLUDED, CohortStatus.NOT_REVIEWED, CohortStatus.INCLUDED,
                CohortStatus.NEEDS_FURTHER_REVIEW),
        2, null);
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNotNull();
    MaterializeCohortResponse response2 = cohortMaterializationService.materializeCohort(cohortReview,
        SearchRequests.allGenders(), ImmutableList
            .of(CohortStatus.EXCLUDED, CohortStatus.NOT_REVIEWED, CohortStatus.INCLUDED,
                CohortStatus.NEEDS_FURTHER_REVIEW),
        2, response.getNextPageToken());
    assertPersonIds(response2, 102246L);
    assertThat(response2.getNextPageToken()).isNull();
  }




  @Test
  public void testMaterializeCohortPaging() {
    MaterializeCohortResponse response = cohortMaterializationService.materializeCohort(null,
        SearchRequests.allGenders(),null, 2, null);
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNotNull();
    MaterializeCohortResponse response2 = cohortMaterializationService.materializeCohort(null,
        SearchRequests.allGenders(),null, 2, response.getNextPageToken());
    assertPersonIds(response2, 102246L);
    assertThat(response2.getNextPageToken()).isNull();

    try {
      // Pagination token doesn't match, this should fail.
      cohortMaterializationService.materializeCohort(null, SearchRequests.males(),
          null, 2, response.getNextPageToken());
      fail("Exception expected");
    } catch (BadRequestException e) {
      // expected
    }

    PaginationToken token = PaginationToken.fromBase64(response.getNextPageToken());
    PaginationToken invalidToken = new PaginationToken(-1L, token.getParameterHash());
    try {
      // Pagination token doesn't match, this should fail.
      cohortMaterializationService.materializeCohort(null, SearchRequests.males(),
          null, 2, invalidToken.toBase64());
      fail("Exception expected");
    } catch (BadRequestException e) {
      // expected
    }
  }

  private void assertPersonIds(MaterializeCohortResponse response, long... personIds) {
    List<Object> expectedResults = new ArrayList<>();
    for (long personId : personIds) {
      expectedResults.add(ImmutableMap.of(CohortMaterializationService.PERSON_ID, personId));
    }
    assertThat(response.getResults()).isEqualTo(expectedResults);
  }
}
