package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.db.model.CommonStorageEnums.domainToStorage;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
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
  private static final QueryJobConfiguration QUERY_JOB_CONFIGURATION_1 =
      QueryJobConfiguration.newBuilder(
              "SELECT * FROM person_id from `${projectId}.${dataSetId}.person` person")
          .addNamedParameter(
              "foo",
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
  static class Configuration {}

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
    when(cohortDao.findCohortByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(cohort);
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

  private ConceptSet buildConceptSet(long conceptSetId, Domain domain, Set<Long> conceptIds) {
    ConceptSet result = new ConceptSet();
    result.setConceptSetId(conceptSetId);
    result.setDomain(domainToStorage(domain));
    result.setConceptIds(conceptIds);
    return result;
  }

  private static DataSetRequest buildEmptyRequest() {
    final DataSetRequest invalidRequest = new DataSetRequest();
    invalidRequest.setDomainValuePairs(Collections.emptyList());
    return invalidRequest;
  }

  @Test(expected = BadRequestException.class)
  public void testThrowsForNoCohortOrConcept() {
    final DataSetRequest invalidRequest = buildEmptyRequest();
    dataSetServiceImpl.generateQueryJobConfigurationsByDomainName(invalidRequest);
  }

  @Test
  public void testGetsCohortQueryStringAndCollectsNamedParameters() {
    final Cohort cohortDbModel = buildSimpleCohort();
    final QueryAndParameters queryAndParameters =
        dataSetServiceImpl.getCohortQueryStringAndCollectNamedParameters(cohortDbModel);
    assertThat(queryAndParameters.getQuery()).isNotEmpty();
    assertThat(queryAndParameters.getNamedParameterValues()).isNotEmpty();
  }

  @Test
  public void testRejectsConceptSetListWithNoConcepts() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(Collections.emptySet());
    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1));
    assertThat(isValid).isFalse();
  }

  @Test
  public void testAcceptsTwoDomainsWithConcepts() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final ConceptSet conceptSet2 = new ConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(isValid).isTrue();
  }

  @Test
  public void testRejectsSomeDomainsWithConceptsSomeWithout() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final ConceptSet conceptSet2 = new ConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final ConceptSet conceptSet3 = new ConceptSet();
    conceptSet3.setDomain((short) Domain.DRUG.ordinal());
    conceptSet3.setConceptIds(Collections.emptySet());

    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1, conceptSet2, conceptSet3));
    assertThat(isValid).isFalse();
  }

  @Test
  public void testAcceptsEmptyConceptSetIfDomainIsPopulated() {
    final ConceptSet conceptSet1 = new ConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final ConceptSet conceptSet2 = new ConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final ConceptSet conceptSet3 = new ConceptSet();
    conceptSet3.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet3.setConceptIds(Collections.emptySet());

    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1, conceptSet2, conceptSet3));
    assertThat(isValid).isTrue();
  }

  @Test
  public void testRejectsEmptyConceptSetList() {
    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            Collections.emptyList());
    assertThat(isValid).isFalse();
  }

  @Test
  public void testBuildConceptIdListClause_same() {
    Domain domain1 = Domain.CONDITION;
    ConceptSet conceptSet1 = buildConceptSet(1L, domain1, ImmutableSet.of(1L, 2L, 3L));
    ConceptSet conceptSet2 = buildConceptSet(2L, domain1, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            domain1, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.isPresent()).isTrue();
    assertThat(listClauseMaybe.get().equals("1 2 3 4 5 6"));
  }

  @Test
  public void testBuildConceptIdListClause_differentDomains() {
    ConceptSet conceptSet1 = buildConceptSet(1L, Domain.CONDITION, ImmutableSet.of(1L, 2L, 3L));
    ConceptSet conceptSet2 = buildConceptSet(2L, Domain.DRUG, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            Domain.CONDITION, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.isPresent()).isTrue();
    assertThat(listClauseMaybe.get().equals("1 2 3"));
  }

  @Test
  public void testBuildConceptIdListClause_noClauseForPersonDomain() {
    ConceptSet conceptSet1 = buildConceptSet(1L, Domain.CONDITION, ImmutableSet.of(1L, 2L, 3L));
    ConceptSet conceptSet2 = buildConceptSet(2L, Domain.DRUG, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            Domain.PERSON, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.isPresent()).isFalse();
  }

  @Test
  public void testcapitalizeFirstCharacterOnly_uppercaseWord() {
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("QWERTY")).isEqualTo("Qwerty");
  }

  @Test
  public void testcapitalizeFirstCharacterOnly_mixedCaseString() {
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("aLl YouR baSE"))
        .isEqualTo("All your base");
  }

  @Test
  public void testcapitalizeFirstCharacterOnly_singleLetterStrings() {
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("a"))
        .isEqualTo("A");
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("B"))
        .isEqualTo("B");
  }

  @Test
  public void testcapitalizeFirstCharacterOnly_emptyString() {
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly(""))
        .isEqualTo("");
  }

  @Test
  public void testCapitalizeFirstCharacterOnly_emoji() {
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("\uD83D\uDCAF"))
        .isEqualTo("\uD83D\uDCAF");
    assertThat((DataSetServiceImpl.capitalizeFirstCharacterOnly("マリオに感謝しますが、私たちの王女は別の城にいます")))
        .isEqualTo("マリオに感謝しますが、私たちの王女は別の城にいます");
  }

}
