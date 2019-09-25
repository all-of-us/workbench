package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.db.dao.DataSetServiceImpl.QueryAndParameters;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.test.SearchRequests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
public class DataSetServiceTest {

  private static final Long COHORT_ID_1 = 100L;
  private static final DomainValuePair DOMAIN_VALUE_PAIR_PERSON_PERSON_ID = new DomainValuePair()
      .domain(Domain.PERSON)
      .value("PERSON_ID");
  private static final Long CONCEPT_SET_ID_1 = 200L;
  private static final ImmutableList<DomainValuePair> DOMAIN_VALUE_PAIR_PERSON_LIST = ImmutableList.of(DOMAIN_VALUE_PAIR_PERSON_PERSON_ID);
  private static final QueryJobConfiguration QUERY_JOB_CONFIGURATION_1 =
      QueryJobConfiguration.newBuilder("SELECT * FROM person_id from `${projectId}.${dataSetId}.person` person")
      .addNamedParameter("foo",
          QueryParameterValue.newBuilder()
              .setType(StandardSQLTypeName.INT64)
              .setValue(Long.toString(101L))
              .build())
          .build();

  @Autowired private BigQueryService bigQueryService;
  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  @Autowired private CohortDao cohortDao;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private ConceptBigQueryService conceptBigQueryService;
  @Autowired private CohortQueryBuilder cohortQueryBuilder;
  @Autowired private DataSetDao dataSetDao;

  private DataSetServiceImpl dataSetServiceImpl;

  @TestConfiguration
  @MockBean({
      BigQueryService.class,
      CdrBigQuerySchemaConfigService.class,
      CohortDao.class,
      ConceptBigQueryService.class,
      ConceptSetDao.class,
      CohortQueryBuilder.class,
      DataSetDao.class
  })
  static class Configuration {

  }

  @Before
  public void setUp() {
    dataSetServiceImpl =
        new DataSetServiceImpl(
            bigQueryService,
            cdrBigQuerySchemaConfigService,
            cohortDao,
            conceptBigQueryService,
            conceptSetDao,
            cohortQueryBuilder,
            dataSetDao);

    final Cohort cohort = buildSimpleCohort();
    when(cohortDao.findCohortByNameAndWorkspaceId(anyString(), anyLong()))
        .thenReturn(cohort);
    when(cohortQueryBuilder.buildParticipantIdQuery(any()))
        .thenReturn(
            QueryJobConfiguration.newBuilder(
                "SELECT * FROM person_id from `${projectId}.${dataSetId}.person` person")
                .build());
    when(bigQueryService.filterBigQueryConfig(any(QueryJobConfiguration.class)))
        .thenReturn(QUERY_JOB_CONFIGURATION_1);
  }

  private Cohort buildSimpleCohort() {
    final SearchRequest searchRequest = SearchRequests.males();
    final String cohortCriteria = new Gson().toJson(searchRequest);

    final Cohort cohortDbModel = new Cohort();
    cohortDbModel.setCohortId(101L);
    cohortDbModel.setType("foo");
    cohortDbModel.setWorkspaceId(1L);
    cohortDbModel.setCriteria(cohortCriteria);
    return cohortDbModel;
  }

  private static DataSetRequest buildEmptyRequest() {
    final DataSetRequest invalidRequest = new DataSetRequest();
    invalidRequest.setValues(Collections.emptyList());
    return invalidRequest;
  }

  private static DataSetRequest buildTrivialRequest() {
    final DataSetRequest result = new DataSetRequest();
    result.setCohortIds(ImmutableList.of(COHORT_ID_1));
    result.setConceptSetIds(ImmutableList.of(CONCEPT_SET_ID_1));
    result.setValues(Collections.emptyList());
    result.setValues(ImmutableList.of(DOMAIN_VALUE_PAIR_PERSON_PERSON_ID));
    return result;
  }

  @Test(expected = BadRequestException.class)
  public void itThrowsForNoCohortOrConcept() throws Exception {
    final DataSetRequest invalidRequest = buildEmptyRequest();
    ImmutableMap<String, QueryJobConfiguration> configurationsByDomain =
        ImmutableMap.copyOf(dataSetServiceImpl.generateQueryJobConfigurationsByDomainName(invalidRequest));
  }

  @Test
  public void itGetsCohortQueryStringAndCollectNamedParameters() throws Exception {
    final Cohort cohortDbModel = buildSimpleCohort();
    final QueryAndParameters queryAndParameters = dataSetServiceImpl.getCohortQueryStringAndCollectNamedParameters(cohortDbModel);
    assertThat(queryAndParameters.getQuery()).isNotEmpty();
    assertThat(queryAndParameters.getNamedParameterValues()).isNotEmpty();
  }

  @Test
  public void itRejectsConceptSetListWithNoConcepts() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(Collections.emptySet());
    final boolean isValid = dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(ImmutableList.of(conceptSet1));
    assertThat(isValid).isFalse();
  }

  @Test
  public void itAcceptsTwoDomainsWithConcepts() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final ConceptSet conceptSet2 = new ConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final boolean isValid = dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(isValid).isTrue();
  }


  @Test
  public void itRejectsSomeDomainsWithConceptsSomeWithout() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final ConceptSet conceptSet2 = new ConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final ConceptSet conceptSet3 = new ConceptSet();
    conceptSet3.setDomain((short) Domain.DRUG.ordinal());
    conceptSet3.setConceptIds(Collections.emptySet());

    final boolean isValid = dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
        ImmutableList.of(conceptSet1, conceptSet2, conceptSet3));
    assertThat(isValid).isFalse();
  }

  @Test
  public void itAcceptsEmptyConceptSetIfDomainIsPopulated() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final ConceptSet conceptSet2 = new ConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final ConceptSet conceptSet3 = new ConceptSet();
    conceptSet3.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet3.setConceptIds(Collections.emptySet());

    final boolean isValid = dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
        ImmutableList.of(conceptSet1, conceptSet2, conceptSet3));
    assertThat(isValid).isTrue();
  }

  @Test
  public void itRejectsEmptyConceptSetList() {
    final boolean isValid = dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(Collections.emptyList());
    assertThat(isValid).isFalse();
  }
}
