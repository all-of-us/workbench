package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.db.model.DbStorageEnums.domainToStorage;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.dataset.DataSetServiceImpl;
import org.pmiops.workbench.dataset.DataSetServiceImpl.QueryAndParameters;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
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
  private static final ImmutableList<Long> COHORT_IDS = ImmutableList.of(101L, 102L);
  private static final String TEST_CDR_PROJECT_ID = "all-of-us-ehr-dev";
  private static final String TEST_CDR_DATA_SET_ID = "synthetic_cdr20180606";
  private static final String TEST_CDR_TABLE = TEST_CDR_PROJECT_ID + "." + TEST_CDR_DATA_SET_ID;
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @Autowired private BigQueryService bigQueryService;
  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  @Autowired private CohortDao cohortDao;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private ConceptBigQueryService conceptBigQueryService;
  @Autowired private CohortQueryBuilder cohortQueryBuilder;
  @Autowired private DataDictionaryEntryDao dataDictionaryEntryDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private DataSetMapper dataSetMapper;

  @MockBean private BigQueryService mockBigQueryService;
  @MockBean private CohortDao mockCohortDao;

  private DataSetServiceImpl dataSetServiceImpl;

  @TestConfiguration
  @MockBean({
    CdrBigQuerySchemaConfigService.class,
    ConceptBigQueryService.class,
    ConceptSetDao.class,
    DataSetMapperImpl.class,
    CohortQueryBuilder.class,
    DataSetDao.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
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
            dataDictionaryEntryDao,
            dataSetDao,
            dataSetMapper,
            CLOCK);

    final DbCohort cohort = buildSimpleCohort();
    when(cohortDao.findCohortByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(cohort);
    when(cohortQueryBuilder.buildParticipantIdQuery(any())).thenReturn(QUERY_JOB_CONFIGURATION_1);
  }

  private DbCohort buildSimpleCohort() {
    final SearchRequest searchRequest = SearchRequests.males();
    final String cohortCriteria = new Gson().toJson(searchRequest);

    final DbCohort cohortDbModel = new DbCohort();
    cohortDbModel.setCohortId(101L);
    cohortDbModel.setType("foo");
    cohortDbModel.setWorkspaceId(1L);
    cohortDbModel.setCriteria(cohortCriteria);
    return cohortDbModel;
  }

  private DbConceptSet buildConceptSet(long conceptSetId, Domain domain, Set<Long> conceptIds) {
    DbConceptSet result = new DbConceptSet();
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
    dataSetServiceImpl.domainToBigQueryConfig(invalidRequest);
  }

  @Test
  public void testGetsCohortQueryStringAndCollectsNamedParameters() {
    final DbCohort cohortDbModel = buildSimpleCohort();
    final QueryAndParameters queryAndParameters =
        dataSetServiceImpl.getCohortQueryStringAndCollectNamedParameters(cohortDbModel);
    assertThat(queryAndParameters.getQuery()).isNotEmpty();
    assertThat(queryAndParameters.getNamedParameterValues()).isNotEmpty();
  }

  @Test
  public void testRejectsConceptSetListWithNoConcepts() {
    final DbConceptSet conceptSet1 = new DbConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(Collections.emptySet());
    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1));
    assertThat(isValid).isFalse();
  }

  @Test
  public void testAcceptsTwoDomainsWithConcepts() {
    final DbConceptSet conceptSet1 = new DbConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final DbConceptSet conceptSet2 = new DbConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(isValid).isTrue();
  }

  @Test
  public void testRejectsSomeDomainsWithConceptsSomeWithout() {
    final DbConceptSet conceptSet1 = new DbConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final DbConceptSet conceptSet2 = new DbConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final DbConceptSet conceptSet3 = new DbConceptSet();
    conceptSet3.setDomain((short) Domain.DRUG.ordinal());
    conceptSet3.setConceptIds(Collections.emptySet());

    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1, conceptSet2, conceptSet3));
    assertThat(isValid).isFalse();
  }

  @Test
  public void testAcceptsEmptyConceptSetIfDomainIsPopulated() {
    final DbConceptSet conceptSet1 = new DbConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet1.setConceptIds(ImmutableSet.of(1L, 2L, 3L));

    final DbConceptSet conceptSet2 = new DbConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    conceptSet2.setConceptIds(ImmutableSet.of(4L, 5L, 6L));

    final DbConceptSet conceptSet3 = new DbConceptSet();
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
    DbConceptSet conceptSet1 = buildConceptSet(1L, domain1, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, domain1, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            domain1, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.map(String::trim).orElse("")).isEqualTo("IN (1, 2, 3, 4, 5, 6)");
  }

  @Test
  public void testBuildConceptIdListClause_differentDomains() {
    DbConceptSet conceptSet1 = buildConceptSet(1L, Domain.CONDITION, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, Domain.DRUG, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            Domain.CONDITION, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.map(String::trim).orElse("")).isEqualTo("IN (1, 2, 3)");
  }

  @Test
  public void testBuildConceptIdListClause_noClauseForPersonDomain() {
    DbConceptSet conceptSet1 = buildConceptSet(1L, Domain.CONDITION, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, Domain.DRUG, ImmutableSet.of(4L, 5L, 6L));
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
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("a")).isEqualTo("A");
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("B")).isEqualTo("B");
  }

  @Test
  public void testcapitalizeFirstCharacterOnly_emptyString() {
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("")).isEqualTo("");
  }

  @Test
  public void testCapitalizeFirstCharacterOnly_emoji() {
    assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("\uD83D\uDCAF"))
        .isEqualTo("\uD83D\uDCAF");
    assertThat((DataSetServiceImpl.capitalizeFirstCharacterOnly("マリオに感謝しますが、私たちの王女は別の城にいます")))
        .isEqualTo("マリオに感謝しますが、私たちの王女は別の城にいます");
  }

  @Test
  public void testDomainToBigQueryConfig() {
    mockLinkingTableQuery(ImmutableList.of("FROM `" + TEST_CDR_TABLE + ".person` person"));
    final DataSetRequest dataSetRequest =
        new DataSetRequest()
            .conceptSetIds(Collections.emptyList())
            .cohortIds(Collections.emptyList())
            .domainValuePairs(Collections.emptyList())
            .name("blah")
            .prePackagedConceptSet(PrePackagedConceptSetEnum.NONE)
            .cohortIds(COHORT_IDS)
            .domainValuePairs(
                ImmutableList.of(new DomainValuePair().domain(Domain.PERSON).value("PERSON_ID")));
    final Gson gson = new Gson();
    final String cohortCriteriaJson =
        gson.toJson(
            new SearchRequest()
                .includes(Collections.emptyList())
                .excludes(Collections.emptyList())
                .dataFilters(Collections.emptyList()),
            SearchRequest.class);

    final DbCohort dbCohort = new DbCohort();
    dbCohort.setCriteria(cohortCriteriaJson);
    dbCohort.setCohortId(COHORT_IDS.get(0));

    doReturn(ImmutableList.of(dbCohort)).when(mockCohortDao).findAllByCohortIdIn(anyList());

    final Map<String, QueryJobConfiguration> result =
        dataSetServiceImpl.domainToBigQueryConfig(dataSetRequest);
    assertThat(result).hasSize(1);
    assertThat(result.get("PERSON").getNamedParameters()).hasSize(1);
    assertThat(result.get("PERSON").getNamedParameters().get("foo_101").getValue())
        .isEqualTo("101");
  }

  private void mockLinkingTableQuery(Collection<String> domainBaseTables) {
    final TableResult tableResultMock = mock(TableResult.class);

    final FieldList schema =
        FieldList.of(
            ImmutableList.of(
                Field.of("OMOP_SQL", LegacySQLTypeName.STRING),
                Field.of("JOIN_VALUE", LegacySQLTypeName.STRING)));

    doReturn(
            domainBaseTables.stream()
                .map(
                    domainBaseTable -> {
                      ArrayList<FieldValue> rows = new ArrayList<>();
                      rows.add(FieldValue.of(Attribute.PRIMITIVE, "PERSON_ID"));
                      rows.add(FieldValue.of(Attribute.PRIMITIVE, domainBaseTable));
                      return FieldValueList.of(rows, schema);
                    })
                .collect(ImmutableList.toImmutableList()))
        .when(tableResultMock)
        .getValues();

    doReturn(tableResultMock).when(mockBigQueryService).executeQuery(any());
  }
}
