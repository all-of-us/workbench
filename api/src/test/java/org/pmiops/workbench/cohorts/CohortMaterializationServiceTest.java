package org.pmiops.workbench.cohorts;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.DomainLookupService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.ConceptSynonymDao;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder;
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
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
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CohortMaterializationServiceTest {

  @Autowired
  private FieldSetQueryBuilder fieldSetQueryBuilder;

  @Autowired
  private AnnotationQueryBuilder annotationQueryBuilder;

  @Autowired
  private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private ConceptSynonymDao conceptSynonymDao;

  @PersistenceContext
  private EntityManager entityManager;

  private CohortReview cohortReview;
  private CohortMaterializationService cohortMaterializationService;

  @TestConfiguration
  @Import({FieldSetQueryBuilder.class, AnnotationQueryBuilder.class,
      TestBigQueryCdrSchemaConfig.class, CohortQueryBuilder.class,
      CdrBigQuerySchemaConfigService.class, DomainLookupService.class})
  @MockBean({BigQueryService.class})
  static class Configuration {
  }

  @Before
  public void setUp() {
    CdrVersion cdrVersion = new CdrVersion();
    cdrVersionDao.save(cdrVersion);

    Workspace workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
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

    ConceptService conceptService = new ConceptService(entityManager, conceptDao, conceptSynonymDao);

    this.cohortMaterializationService = new CohortMaterializationService(fieldSetQueryBuilder,
        annotationQueryBuilder, participantCohortStatusDao, cdrBigQuerySchemaConfigService, conceptService);

  }

  @Test
  public void testMaterializeAnnotationQueryNoPagination() {
    FieldSet fieldSet = new FieldSet();
    fieldSet.setAnnotationQuery(new AnnotationQuery());
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(cohortReview, SearchRequests.allGenders(),
            null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map = ImmutableMap.of("person_id", 1L, "review_status", "INCLUDED");
    assertResults(response, p1Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeAnnotationQueryWithPagination() {
    FieldSet fieldSet = new FieldSet();
    fieldSet.setAnnotationQuery(new AnnotationQuery());
    MaterializeCohortRequest request = makeRequest(fieldSet, 1);
    request.setStatusFilter(ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.EXCLUDED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(cohortReview, SearchRequests.allGenders(), null, 0, request);
    ImmutableMap<String, Object> p1Map = ImmutableMap.of("person_id", 1L, "review_status", "INCLUDED");
    assertResults(response, p1Map);
    assertThat(response.getNextPageToken()).isNotNull();

    request.setPageToken(response.getNextPageToken());
    MaterializeCohortResponse response2 =
        cohortMaterializationService.materializeCohort(cohortReview, SearchRequests.allGenders(), null, 0, request);
    ImmutableMap<String, Object> p2Map = ImmutableMap.of("person_id", 2L, "review_status", "EXCLUDED");
    assertResults(response2, p2Map);
    assertThat(response2.getNextPageToken()).isNull();
  }

  private MaterializeCohortRequest makeRequest(int pageSize) {
    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setPageSize(pageSize);
    return request;
  }

  private MaterializeCohortRequest makeRequest(FieldSet fieldSet, int pageSize) {
    MaterializeCohortRequest request = makeRequest(pageSize);
    request.setFieldSet(fieldSet);
    return request;
  }

  private ParticipantCohortStatus makeStatus(long cohortReviewId, long participantId, CohortStatus status) {
    ParticipantCohortStatusKey key = new ParticipantCohortStatusKey();
    key.setCohortReviewId(cohortReviewId);
    key.setParticipantId(participantId);
    ParticipantCohortStatus result = new ParticipantCohortStatus();
    result.setStatusEnum(status);
    result.setParticipantKey(key);
    return result;
  }

  private void assertResults(MaterializeCohortResponse response, ImmutableMap<String, Object>... results) {
    if (response.getResults().size() != results.length) {
      fail("Expected " + results.length + ", got " + response.getResults().size() + "; actual results: " +
          response.getResults());
    }
    for (int i = 0; i < response.getResults().size(); i++) {
      MapDifference<String, Object> difference =
          Maps.difference((Map<String, Object>) response.getResults().get(i), results[i]);
      if (!difference.areEqual()) {
        fail("Result " + i + " had difference: " + difference.entriesDiffering()
            + "; unexpected entries: " + difference.entriesOnlyOnLeft()
            + "; missing entries: " + difference.entriesOnlyOnRight());
      }
    }
  }


}
