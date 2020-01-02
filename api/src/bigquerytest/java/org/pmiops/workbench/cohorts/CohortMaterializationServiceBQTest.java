package org.pmiops.workbench.cohorts;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.api.BigQueryBaseTest;
import org.pmiops.workbench.api.BigQueryTestService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ColumnFilter;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.ResultFilters;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({
  BigQueryTestService.class,
  CohortQueryBuilder.class,
  CohortQueryBuilder.class,
  FieldSetQueryBuilder.class,
  TestJpaConfig.class,
  TestBigQueryCdrSchemaConfig.class,
  AnnotationQueryBuilder.class,
  CdrBigQuerySchemaConfigService.class,
  SearchGroupItemQueryBuilder.class
})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortMaterializationServiceBQTest extends BigQueryBaseTest {

  private CohortMaterializationService cohortMaterializationService;
  private DbCohortReview cohortReview;

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private CohortDao cohortDao;

  @Autowired private CohortReviewDao cohortReviewDao;

  @Autowired private ConceptDao conceptDao;

  @Autowired private DomainInfoDao domainInfoDao;

  @Autowired private SurveyModuleDao surveyModuleDao;

  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired private FieldSetQueryBuilder fieldSetQueryBuilder;

  @Autowired private AnnotationQueryBuilder annotationQueryBuilder;

  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  @Mock private Provider<WorkbenchConfig> configProvider;

  private DbParticipantCohortStatus makeStatus(
      long cohortReviewId, long participantId, CohortStatus status) {
    DbParticipantCohortStatusKey key = new DbParticipantCohortStatusKey();
    key.setCohortReviewId(cohortReviewId);
    key.setParticipantId(participantId);
    DbParticipantCohortStatus result = new DbParticipantCohortStatus();
    result.setStatusEnum(status);
    result.setParticipantKey(key);
    return result;
  }

  @Before
  public void setUp() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    cdrVersion = cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    DbWorkspace workspace = new DbWorkspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    workspace = workspaceDao.save(workspace);

    DbCohort cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort.setName("males");
    cohort.setType("AOU");
    Gson gson = new Gson();
    cohort.setCriteria(gson.toJson(SearchRequests.males()));
    cohort = cohortDao.save(cohort);

    DbCohort cohort2 = new DbCohort();
    cohort2.setWorkspaceId(workspace.getWorkspaceId());
    cohort2.setName("all genders");
    cohort2.setType("AOU");
    cohort2.setCriteria(gson.toJson(SearchRequests.allGenders()));
    cohortDao.save(cohort2);

    cohortReview = new DbCohortReview();
    cohortReview.setCdrVersionId(cdrVersion.getCdrVersionId());
    cohortReview.setCohortId(cohort2.getCohortId());
    cohortReview.setMatchedParticipantCount(3);
    cohortReview.setReviewedCount(2);
    cohortReview.setReviewSize(3);
    cohortReview = cohortReviewDao.save(cohortReview);

    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), 1L, CohortStatus.INCLUDED));
    participantCohortStatusDao.save(
        makeStatus(cohortReview.getCohortReviewId(), 2L, CohortStatus.EXCLUDED));

    ConceptService conceptService = new ConceptService(conceptDao, domainInfoDao, surveyModuleDao);

    this.cohortMaterializationService =
        new CohortMaterializationService(
            fieldSetQueryBuilder,
            annotationQueryBuilder,
            participantCohortStatusDao,
            cdrBigQuerySchemaConfigService,
            conceptService,
            configProvider);
  }

  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
        "person",
        "concept",
        "condition_occurrence",
        "observation",
        "vocabulary",
        "cb_search_all_events");
  }

  @Override
  public String getTestDataDirectory() {
    return MATERIALIZED_DATA;
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

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortBadSpec() {
    cohortMaterializationService.materializeCohort(null, "badSpec", null, makeRequest(1000));
  }

  @Test
  public void testMaterializeCohortOneMaleSpec() {
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, new Gson().toJson(SearchRequests.males()), null, makeRequest(1000));
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortOneMale() {
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(1000));
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortICD9Group() {
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.icd9Codes(), null, 0, makeRequest(1000));
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortTemporalGroup() {
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.temporalRequest(), null, 0, makeRequest(1000));
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNullStatusFilter() {
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, makeRequest(2));
    // With a null status filter, everyone but excluded participants are returned.
    assertPersonIds(response, 1L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotExcludedFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(
        ImmutableList.of(
            CohortStatus.NOT_REVIEWED, CohortStatus.INCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    // With a not excluded status filter, ID 2 is not returned.
    assertPersonIds(response, 1L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewJustExcludedFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(ImmutableList.of(CohortStatus.EXCLUDED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewJustIncludedFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(ImmutableList.of(CohortStatus.INCLUDED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewIncludedAndExcludedFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(ImmutableList.of(CohortStatus.EXCLUDED, CohortStatus.INCLUDED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewJustNotReviewedFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(ImmutableList.of(CohortStatus.NOT_REVIEWED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotReviewedAndIncludedFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.NOT_REVIEWED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 1L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotReviewedAndNeedsFurtherReviewFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(
        ImmutableList.of(CohortStatus.NEEDS_FURTHER_REVIEW, CohortStatus.NOT_REVIEWED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewNotReviewedAndExcludedFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(ImmutableList.of(CohortStatus.EXCLUDED, CohortStatus.NOT_REVIEWED));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWithReviewAllFilter() {
    MaterializeCohortRequest request = makeRequest(2);
    request.setStatusFilter(
        ImmutableList.of(
            CohortStatus.EXCLUDED,
            CohortStatus.NOT_REVIEWED,
            CohortStatus.INCLUDED,
            CohortStatus.NEEDS_FURTHER_REVIEW));
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNotNull();
    request.setPageToken(response.getNextPageToken());
    MaterializeCohortResponse response2 =
        cohortMaterializationService.materializeCohort(
            cohortReview, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response2, 102246L);
    assertThat(response2.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPaging() {
    MaterializeCohortRequest request = makeRequest(2);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNotNull();
    request.setPageToken(response.getNextPageToken());
    MaterializeCohortResponse response2 =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, request);
    assertPersonIds(response2, 102246L);
    assertThat(response2.getNextPageToken()).isNull();

    try {
      // Pagination token doesn't match, this should fail.
      cohortMaterializationService.materializeCohort(
          null, SearchRequests.males(), null, 1, request);
      fail("Exception expected");
    } catch (BadRequestException e) {
      // expected
    }

    PaginationToken token = PaginationToken.fromBase64(response.getNextPageToken());
    PaginationToken invalidToken = new PaginationToken(-1L, token.getParameterHash());
    request.setPageToken(invalidToken.toBase64());
    try {
      // Invalid offset, this should fail.
      cohortMaterializationService.materializeCohort(
          null, SearchRequests.males(), null, 0, request);
      fail("Exception expected");
    } catch (BadRequestException e) {
      // expected
    }
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdOnly() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNumberFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setValueNumber(new BigDecimal(1L));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNotFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setValueNumber(new BigDecimal(1L));
    ResultFilters resultFilters = makeResultFilters(filter);
    resultFilters.setIfNot(true);
    tableQuery.setFilters(resultFilters);
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNumberGreaterThanFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setOperator(Operator.GREATER_THAN);
    filter.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNumberGreaterThanOrEqualToFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setOperator(Operator.GREATER_THAN_OR_EQUAL_TO);
    filter.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNumberLessThanFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setOperator(Operator.LESS_THAN);
    filter.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNumberLessThanOrEqualToFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setOperator(Operator.LESS_THAN_OR_EQUAL_TO);
    filter.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNumberNotEqualFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setOperator(Operator.NOT_EQUAL);
    filter.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithNumbersFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_id");
    filter.setOperator(Operator.IN);
    filter.setValueNumbers(ImmutableList.of(new BigDecimal(1L), new BigDecimal(2L)));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_source_value");
    filter.setValue("psv");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringFilterNullNonMatch() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("ethnicity_source_value");
    filter.setValue("esv");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringGreaterThanNullNonMatch() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("ethnicity_source_value");
    filter.setOperator(Operator.GREATER_THAN);
    filter.setValue("esf");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringLessThanNullNonMatch() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("ethnicity_source_value");
    filter.setOperator(Operator.LESS_THAN);
    filter.setValue("esv");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringIsNull() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("ethnicity_source_value");
    filter.setOperator(Operator.EQUAL);
    filter.setValueNull(true);
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringNotEqualNullNonMatch() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("ethnicity_source_value");
    filter.setOperator(Operator.NOT_EQUAL);
    filter.setValue("esv");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringIsNotNull() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("ethnicity_source_value");
    filter.setOperator(Operator.NOT_EQUAL);
    filter.setValueNull(true);
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetOrderByGenderConceptId() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id", "gender_concept_id"));
    tableQuery.setOrderBy(ImmutableList.of("gender_concept_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.of("person_id", 1L, "gender_concept_id", 8507L);
    ImmutableMap<String, Object> p2Map = ImmutableMap.of("person_id", 2L, "gender_concept_id", 2L);
    ImmutableMap<String, Object> p3Map =
        ImmutableMap.of("person_id", 102246L, "gender_concept_id", 8532L);
    assertResults(response, p2Map, p1Map, p3Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetOrderByGenderConceptIdDescending() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id", "gender_concept_id"));
    tableQuery.setOrderBy(ImmutableList.of("descending(gender_concept_id)"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.of("person_id", 1L, "gender_concept_id", 8507L);
    ImmutableMap<String, Object> p2Map = ImmutableMap.of("person_id", 2L, "gender_concept_id", 2L);
    ImmutableMap<String, Object> p3Map =
        ImmutableMap.of("person_id", 102246L, "gender_concept_id", 8532L);
    assertResults(response, p3Map, p1Map, p2Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringLikeFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_source_value");
    filter.setOperator(Operator.LIKE);
    filter.setValue("p%");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringLikeFilterNoMatch() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_source_value");
    filter.setOperator(Operator.LIKE);
    filter.setValue("p");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringsFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_source_value");
    filter.setOperator(Operator.IN);
    filter.setValues(ImmutableList.of("foobar", "psv"));
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithStringNonMatchFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter = new ColumnFilter();
    filter.setColumnName("person_source_value");
    filter.setValue("foobar");
    tableQuery.setFilters(makeResultFilters(filter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithAllOfFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter1 = new ColumnFilter();
    filter1.setColumnName("person_source_value");
    filter1.setValue("psv");
    ColumnFilter filter2 = new ColumnFilter();
    filter2.setColumnName("person_id");
    filter2.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeAllOf(filter1, filter2));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetAnyOfFilters() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter1 = new ColumnFilter();
    filter1.setColumnName("year_of_birth");
    filter1.setValueNumber(new BigDecimal(1980));
    ColumnFilter filter2 = new ColumnFilter();
    filter2.setColumnName("person_id");
    filter2.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeAnyOf(filter1, filter2));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetPersonIdWithMultiLevelFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter filter1 = new ColumnFilter();
    filter1.setColumnName("year_of_birth");
    filter1.setValueNumber(new BigDecimal(1987));
    ColumnFilter filter2 = new ColumnFilter();
    filter2.setColumnName("person_id");
    filter2.setValueNumber(new BigDecimal(4L));
    ColumnFilter filter3 = new ColumnFilter();
    filter3.setColumnName("person_id");
    filter3.setOperator(Operator.LESS_THAN_OR_EQUAL_TO);
    filter3.setValueNumber(new BigDecimal(3L));
    ResultFilters resultFilters = new ResultFilters();
    ResultFilters anyOf = makeAnyOf(filter1, filter2);
    anyOf.setIfNot(true);
    resultFilters.setAllOf(ImmutableList.of(makeResultFilters(filter3), anyOf));
    tableQuery.setFilters(resultFilters);
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonFieldSetAllColumns() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 1L)
            .put("gender_source_value", "1")
            .put("race_source_value", "1")
            .put("gender_concept_id", 8507L)
            .put("year_of_birth", 1980L)
            .put("month_of_birth", 8L)
            .put("day_of_birth", 1L)
            .put("race_concept_id", 1L)
            .put("ethnicity_concept_id", 1L)
            .put("location_id", 1L)
            .put("provider_id", 1L)
            .put("care_site_id", 1L)
            .put("person_source_value", "psv")
            .put("gender_source_concept_id", 1L)
            .put("race_source_concept_id", 1L)
            .put("ethnicity_source_value", "esv")
            .put("ethnicity_source_concept_id", 1L)
            .build();
    assertResults(response, p1Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFieldSetAllColumns() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.<String, Object>builder()
            .put("observation_id", 5L)
            .put("person_id", 1L)
            .put("observation_concept_id", 5L)
            .put("observation_date", "2009-12-03")
            .put("observation_datetime", "2009-12-03 05:00:00 UTC")
            .put("observation_type_concept_id", 5L)
            .put("value_as_number", 5.0)
            .put("value_as_string", "5")
            .put("value_as_concept_id", 5L)
            .put("qualifier_concept_id", 5L)
            .put("unit_concept_id", 5L)
            .put("provider_id", 5L)
            .put("visit_occurrence_id", 5L)
            .put("observation_source_value", "5")
            .put("observation_source_concept_id", 5L)
            .put("unit_source_value", "5")
            .put("qualifier_source_value", "5")
            .build();
    assertResults(response, p1Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationPersonNotFound() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.females(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDateEqual() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_date");
    columnFilter.setValueDate("2009-12-03");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDateMismatch() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_date");
    columnFilter.setValueDate("2009-12-04");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDateGreaterThan() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_date");
    columnFilter.setOperator(Operator.GREATER_THAN);
    columnFilter.setValueDate("2009-12-02");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDateGreaterThanOrEqualTo() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_date");
    columnFilter.setOperator(Operator.GREATER_THAN_OR_EQUAL_TO);
    columnFilter.setValueDate("2009-12-03");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDateLessThan() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_date");
    columnFilter.setOperator(Operator.LESS_THAN);
    columnFilter.setValueDate("2009-12-04");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDateLessThanOrEqualTo() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_date");
    columnFilter.setOperator(Operator.LESS_THAN_OR_EQUAL_TO);
    columnFilter.setValueDate("2009-12-03");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDatetimeEqual() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_datetime");
    columnFilter.setValueDate("2009-12-03 05:00:00 UTC");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDatetimeMismatch() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_datetime");
    columnFilter.setValueDate("2009-12-03 05:00:01 UTC");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDatetimeGreaterThan() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_datetime");
    columnFilter.setOperator(Operator.GREATER_THAN);
    columnFilter.setValueDate("2009-12-03 04:59:59 UTC");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDatetimeGreaterThanOrEqualTo() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_datetime");
    columnFilter.setOperator(Operator.GREATER_THAN_OR_EQUAL_TO);
    columnFilter.setValueDate("2009-12-03 05:00:00 UTC");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDatetimeLessThan() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_datetime");
    columnFilter.setOperator(Operator.LESS_THAN);
    columnFilter.setValueDate("2009-12-03 05:00:01 UTC");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortObservationFilterObservationDatetimeLessThanOrEqualTo() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("observation");
    tableQuery.setColumns(ImmutableList.of("observation_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("observation_datetime");
    columnFilter.setOperator(Operator.LESS_THAN_OR_EQUAL_TO);
    columnFilter.setValueDate("2009-12-03 05:00:00 UTC");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000));
    assertResults(response, ImmutableMap.of("observation_id", 5L));
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonConceptSelectColumns() {
    // Here person is in the inner query; gender_concept, gender_concept.vocabulary,
    // and gender_concept.vocabulary.vocabulary_concept are only referenced in the select clause
    // and are in the outer query.
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(
        ImmutableList.of(
            "person_id",
            "gender_concept.concept_name",
            "gender_concept.vocabulary_id",
            "gender_concept.vocabulary.vocabulary_name",
            "gender_concept.vocabulary.vocabulary_reference",
            "gender_concept.vocabulary.vocabulary_concept.concept_name"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("person_id");
    columnFilter.setOperator(Operator.NOT_EQUAL);
    columnFilter.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 1L)
            .put("gender_concept.concept_name", "MALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    ImmutableMap<String, Object> p2Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 102246L)
            .put("gender_concept.concept_name", "FEMALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    assertResults(response, p1Map, p2Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWhereInner() {
    // person and gender_concept are in the inner query (since gender_concept is in the where
    // clause.)
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(
        ImmutableList.of(
            "person_id",
            "gender_concept.concept_name",
            "gender_concept.vocabulary_id",
            "gender_concept.vocabulary.vocabulary_name",
            "gender_concept.vocabulary.vocabulary_reference",
            "gender_concept.vocabulary.vocabulary_concept.concept_name"));
    ColumnFilter columnFilter1 = new ColumnFilter();
    columnFilter1.setColumnName("person_id");
    columnFilter1.setOperator(Operator.NOT_EQUAL);
    columnFilter1.setValueNumber(new BigDecimal(2L));
    ColumnFilter columnFilter2 = new ColumnFilter();
    columnFilter2.setColumnName("gender_concept.vocabulary_id");
    columnFilter2.setValue("Gender");
    tableQuery.setFilters(makeAllOf(columnFilter1, columnFilter2));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 1L)
            .put("gender_concept.concept_name", "MALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    ImmutableMap<String, Object> p2Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 102246L)
            .put("gender_concept.concept_name", "FEMALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    assertResults(response, p1Map, p2Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortWhereNestedInner() {
    // person and gender_concept.vocabulary are in the inner query, and thus so is gender_concept.
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(
        ImmutableList.of(
            "person_id",
            "gender_concept.concept_name",
            "gender_concept.vocabulary_id",
            "gender_concept.vocabulary.vocabulary_name",
            "gender_concept.vocabulary.vocabulary_reference",
            "gender_concept.vocabulary.vocabulary_concept.concept_name"));
    ColumnFilter columnFilter1 = new ColumnFilter();
    columnFilter1.setColumnName("person_id");
    columnFilter1.setOperator(Operator.NOT_EQUAL);
    columnFilter1.setValueNumber(new BigDecimal(2L));
    ColumnFilter columnFilter2 = new ColumnFilter();
    columnFilter2.setColumnName("gender_concept.vocabulary.vocabulary_name");
    columnFilter2.setValue("Gender vocabulary");
    tableQuery.setFilters(makeAllOf(columnFilter1, columnFilter2));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 1L)
            .put("gender_concept.concept_name", "MALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    ImmutableMap<String, Object> p2Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 102246L)
            .put("gender_concept.concept_name", "FEMALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    assertResults(response, p1Map, p2Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortOrderByInner() {
    // person and gender_concept are in the inner query (since gender_concept is in order by).
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(
        ImmutableList.of(
            "person_id",
            "gender_concept.concept_name",
            "gender_concept.vocabulary_id",
            "gender_concept.vocabulary.vocabulary_name",
            "gender_concept.vocabulary.vocabulary_reference",
            "gender_concept.vocabulary.vocabulary_concept.concept_name"));
    ColumnFilter columnFilter1 = new ColumnFilter();
    columnFilter1.setColumnName("person_id");
    columnFilter1.setOperator(Operator.NOT_EQUAL);
    columnFilter1.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(columnFilter1));
    tableQuery.setOrderBy(ImmutableList.of("gender_concept.concept_name"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 1L)
            .put("gender_concept.concept_name", "MALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    ImmutableMap<String, Object> p2Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 102246L)
            .put("gender_concept.concept_name", "FEMALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    assertResults(response, p2Map, p1Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortOrderByNestedInner() {
    // person and gender_concept.vocabulary are in the inner query, and thus so is gender_concept.
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(
        ImmutableList.of(
            "person_id",
            "gender_concept.concept_name",
            "gender_concept.vocabulary_id",
            "gender_concept.vocabulary.vocabulary_name",
            "gender_concept.vocabulary.vocabulary_reference",
            "gender_concept.vocabulary.vocabulary_concept.concept_name"));
    ColumnFilter columnFilter1 = new ColumnFilter();
    columnFilter1.setColumnName("person_id");
    columnFilter1.setOperator(Operator.NOT_EQUAL);
    columnFilter1.setValueNumber(new BigDecimal(2L));
    tableQuery.setFilters(makeResultFilters(columnFilter1));
    tableQuery.setOrderBy(
        ImmutableList.of("DESCENDING(gender_concept.vocabulary.vocabulary_name)", "person_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    ImmutableMap<String, Object> p1Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 1L)
            .put("gender_concept.concept_name", "MALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    ImmutableMap<String, Object> p2Map =
        ImmutableMap.<String, Object>builder()
            .put("person_id", 102246L)
            .put("gender_concept.concept_name", "FEMALE")
            .put("gender_concept.vocabulary_id", "Gender")
            .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
            .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
            .put(
                "gender_concept.vocabulary.vocabulary_concept.concept_name",
                "Gender vocabulary concept")
            .build();
    assertResults(response, p1Map, p2Map);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonConceptFilter() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    ColumnFilter columnFilter = new ColumnFilter();
    columnFilter.setColumnName("gender_concept.concept_name");
    columnFilter.setValue("FEMALE");
    tableQuery.setFilters(makeResultFilters(columnFilter));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 102246L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortPersonConceptOrderBy() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("person");
    tableQuery.setColumns(ImmutableList.of("person_id"));
    tableQuery.setOrderBy(
        ImmutableList.of("gender_concept.vocabulary_id", "descending(person_id)"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000));
    assertPersonIds(response, 102246L, 1L, 2L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortConceptSetNoConcepts() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("condition_occurrence");
    tableQuery.setColumns(
        ImmutableList.of("person_id", "condition_concept_id", "condition_source_concept_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    cohortMaterializationService.materializeCohort(
        null,
        SearchRequests.allGenders(),
        ImmutableSet.of(123456L),
        0,
        makeRequest(fieldSet, 1000));
  }

  @Test
  public void testMaterializeCohortConceptSetNoMatchingConcepts() {
    conceptDao.save(
        new DbConcept().conceptId(2L).standardConcept(ConceptService.STANDARD_CONCEPT_CODE));

    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("condition_occurrence");
    tableQuery.setColumns(
        ImmutableList.of("person_id", "condition_concept_id", "condition_source_concept_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null, SearchRequests.allGenders(), ImmutableSet.of(2L), 0, makeRequest(fieldSet, 1000));
    assertResults(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortConceptSetOneStandardConcept() {
    conceptDao.save(
        new DbConcept().conceptId(192819L).standardConcept(ConceptService.STANDARD_CONCEPT_CODE));

    TableQuery tableQuery =
        new TableQuery()
            .tableName("condition_occurrence")
            .columns(ImmutableList.of("condition_occurrence_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null,
            SearchRequests.allGenders(),
            ImmutableSet.of(192819L),
            0,
            makeRequest(fieldSet, 1000));
    assertConditionOccurrenceIds(response, 12751439L, 12751440L);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortConceptSetOneStandardConceptMismatch() {
    conceptDao.save(
        new DbConcept().conceptId(44829697L).standardConcept(ConceptService.STANDARD_CONCEPT_CODE));
    final MaterializeCohortResponse response = materializeCohort();
    assertResults(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortConceptSetOneSourceConcept() {
    conceptDao.save(new DbConcept().conceptId(44829697L));
    MaterializeCohortResponse response = materializeCohort();
    assertConditionOccurrenceIds(response, 12751439L, 12751440L);
    assertThat(response.getNextPageToken()).isNull();
  }

  private MaterializeCohortResponse materializeCohort() {
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("condition_occurrence");
    tableQuery.setColumns(ImmutableList.of("condition_occurrence_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    return cohortMaterializationService.materializeCohort(
        null,
        SearchRequests.allGenders(),
        ImmutableSet.of(44829697L),
        0,
        makeRequest(fieldSet, 1000));
  }

  @Test
  public void testMaterializeCohortConceptSetOneSourceConceptMismatch() {
    DbConcept concept = new DbConcept();
    concept.setConceptId(192819L);
    conceptDao.save(concept);
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("condition_occurrence");
    tableQuery.setColumns(ImmutableList.of("condition_occurrence_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null,
            SearchRequests.allGenders(),
            ImmutableSet.of(192819L),
            0,
            makeRequest(fieldSet, 1000));
    assertResults(response);
    assertThat(response.getNextPageToken()).isNull();
  }

  @Test
  public void testMaterializeCohortConceptSetLotsOfConceptsPaging() {
    conceptDao.save(new DbConcept().conceptId(1L));
    conceptDao.save(
        new DbConcept().conceptId(6L).standardConcept(ConceptService.STANDARD_CONCEPT_CODE));
    conceptDao.save(
        new DbConcept().conceptId(7L).standardConcept(ConceptService.STANDARD_CONCEPT_CODE));
    conceptDao.save(
        new DbConcept().conceptId(192819L).standardConcept(ConceptService.STANDARD_CONCEPT_CODE));
    conceptDao.save(new DbConcept().conceptId(44829697L));
    TableQuery tableQuery = new TableQuery();
    tableQuery.setTableName("condition_occurrence");
    tableQuery.setColumns(ImmutableList.of("condition_occurrence_id"));
    tableQuery.setOrderBy(ImmutableList.of("condition_occurrence_id"));
    FieldSet fieldSet = new FieldSet();
    fieldSet.setTableQuery(tableQuery);
    MaterializeCohortRequest request = makeRequest(fieldSet, 4);
    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            null,
            SearchRequests.allGenders(),
            ImmutableSet.of(1L, 6L, 7L, 192819L, 44829697L, 12345L),
            0,
            request);
    assertConditionOccurrenceIds(response, 1L, 6L, 7L, 12751439L);
    assertThat(response.getNextPageToken()).isNotNull();
    request.setPageToken(response.getNextPageToken());
    MaterializeCohortResponse response2 =
        cohortMaterializationService.materializeCohort(
            null,
            SearchRequests.allGenders(),
            ImmutableSet.of(1L, 6L, 7L, 192819L, 44829697L, 12345L),
            0,
            request);
    assertConditionOccurrenceIds(response2, 12751440L);
    assertThat(response2.getNextPageToken()).isNull();
  }

  private ResultFilters makeResultFilters(ColumnFilter columnFilter) {
    ResultFilters result = new ResultFilters();
    result.setColumnFilter(columnFilter);
    return result;
  }

  private ResultFilters makeAnyOf(ColumnFilter... columnFilters) {
    ResultFilters result = new ResultFilters();
    result.setAnyOf(
        Arrays.stream(columnFilters).map(this::makeResultFilters).collect(Collectors.toList()));
    return result;
  }

  private ResultFilters makeAllOf(ColumnFilter... columnFilters) {
    ResultFilters result = new ResultFilters();
    result.setAllOf(
        Arrays.stream(columnFilters).map(this::makeResultFilters).collect(Collectors.toList()));
    return result;
  }

  private void assertResults(
      MaterializeCohortResponse response, ImmutableMap<String, Object>... results) {
    if (response.getResults().size() != results.length) {
      fail(
          "Expected "
              + results.length
              + ", got "
              + response.getResults().size()
              + "; actual results: "
              + response.getResults());
    }
    for (int i = 0; i < response.getResults().size(); i++) {
      MapDifference<String, Object> difference =
          Maps.difference((Map<String, Object>) response.getResults().get(i), results[i]);
      if (!difference.areEqual()) {
        fail(
            "Result "
                + i
                + " had difference: "
                + difference.entriesDiffering()
                + "; unexpected entries: "
                + difference.entriesOnlyOnLeft()
                + "; missing entries: "
                + difference.entriesOnlyOnRight());
      }
    }
  }

  private void assertPersonIds(MaterializeCohortResponse response, long... personIds) {
    List<Object> expectedResults = new ArrayList<>();
    for (long personId : personIds) {
      expectedResults.add(ImmutableMap.of(CohortMaterializationService.PERSON_ID, personId));
    }
    assertThat(response.getResults()).isEqualTo(expectedResults);
  }

  private void assertConditionOccurrenceIds(
      MaterializeCohortResponse response, long... conditionOccurrenceIds) {
    List<Object> expectedResults = new ArrayList<>();
    for (long conditionOccurrenceId : conditionOccurrenceIds) {
      expectedResults.add(ImmutableMap.of("condition_occurrence_id", conditionOccurrenceId));
    }
    assertThat(response.getResults()).isEqualTo(expectedResults);
  }
}
