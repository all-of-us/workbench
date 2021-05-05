package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetServiceImpl;
import org.pmiops.workbench.dataset.DataSetServiceImpl.QueryAndParameters;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

// TODO(calbach): Move this test to the correct package.
@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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
  @Autowired private DataSetDao dataSetDao;
  @Autowired private DSLinkingDao dsLinkingDao;
  @Autowired private DSDataDictionaryDao dsDataDictionaryDao;
  @Autowired private DataSetMapper dataSetMapper;
  @Autowired private CohortQueryBuilder mockCohortQueryBuilder;

  @MockBean private BigQueryService mockBigQueryService;
  @MockBean private CohortDao mockCohortDao;

  private DbCohort cohort;
  private DataSetServiceImpl dataSetServiceImpl;

  @TestConfiguration
  @Import({DataSetMapperImpl.class})
  @MockBean({
    CdrBigQuerySchemaConfigService.class,
    CommonMappers.class,
    CohortService.class,
    ConceptBigQueryService.class,
    ConceptSetDao.class,
    ConceptSetService.class,
    CohortQueryBuilder.class,
    DataSetDao.class,
    DSLinkingDao.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    WorkbenchConfig workbenchConfig() {
      return WorkbenchConfig.createEmptyConfig();
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
            mockCohortQueryBuilder,
            dataSetDao,
            dsLinkingDao,
            dsDataDictionaryDao,
            dataSetMapper,
            CLOCK);

    cohort = buildSimpleCohort();
    when(cohortDao.findCohortByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(cohort);
    when(mockCohortQueryBuilder.buildParticipantIdQuery(any()))
        .thenReturn(QUERY_JOB_CONFIGURATION_1);
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

  private DbConceptSet buildConceptSet(
      long conceptSetId, Domain domain, boolean standard, Set<Long> conceptIds) {
    DbConceptSet result = new DbConceptSet();
    result.setConceptSetId(conceptSetId);
    result.setDomain(domainToStorage(domain));
    result.setConceptSetConceptIds(
        conceptIds.stream()
            .map(c -> DbConceptSetConceptId.builder().addConceptId(c).addStandard(standard).build())
            .collect(Collectors.toSet()));
    return result;
  }

  private static DataSetRequest buildEmptyRequest() {
    final DataSetRequest invalidRequest = new DataSetRequest();
    invalidRequest.setDomainValuePairs(Collections.emptyList());
    invalidRequest.setPrePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE));
    return invalidRequest;
  }

  @Test(expected = BadRequestException.class)
  public void testThrowsForNoCohortOrConcept() {
    final DataSetRequest invalidRequest = buildEmptyRequest();
    invalidRequest.setDomainValuePairs(
        ImmutableList.of(new DomainValuePair().domain(Domain.CONDITION)));
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
    conceptSet1.setConceptSetConceptIds(Collections.emptySet());
    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1));
    assertThat(isValid).isFalse();
  }

  @Test
  public void testAcceptsTwoDomainsWithConcepts() {
    final DbConceptSet conceptSet1 = new DbConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder().addConceptId(1L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder().addConceptId(2L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder().addConceptId(3L).addStandard(true).build();
    conceptSet1.setConceptSetConceptIds(
        ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2, dbConceptSetConceptId3));

    final DbConceptSet conceptSet2 = new DbConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    DbConceptSetConceptId dbConceptSetConceptId4 =
        DbConceptSetConceptId.builder().addConceptId(4L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId5 =
        DbConceptSetConceptId.builder().addConceptId(5L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId6 =
        DbConceptSetConceptId.builder().addConceptId(6L).addStandard(true).build();
    conceptSet2.setConceptSetConceptIds(
        ImmutableSet.of(dbConceptSetConceptId4, dbConceptSetConceptId5, dbConceptSetConceptId6));

    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(isValid).isTrue();
  }

  @Test
  public void testRejectsSomeDomainsWithConceptsSomeWithout() {
    final DbConceptSet conceptSet1 = new DbConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder().addConceptId(1L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder().addConceptId(2L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder().addConceptId(3L).addStandard(true).build();
    conceptSet1.setConceptSetConceptIds(
        ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2, dbConceptSetConceptId3));

    final DbConceptSet conceptSet2 = new DbConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    DbConceptSetConceptId dbConceptSetConceptId4 =
        DbConceptSetConceptId.builder().addConceptId(4L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId5 =
        DbConceptSetConceptId.builder().addConceptId(5L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId6 =
        DbConceptSetConceptId.builder().addConceptId(6L).addStandard(true).build();
    conceptSet2.setConceptSetConceptIds(
        ImmutableSet.of(dbConceptSetConceptId4, dbConceptSetConceptId5, dbConceptSetConceptId6));

    final DbConceptSet conceptSet3 = new DbConceptSet();
    conceptSet3.setDomain((short) Domain.DRUG.ordinal());
    conceptSet3.setConceptSetConceptIds(Collections.emptySet());

    final boolean isValid =
        dataSetServiceImpl.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            ImmutableList.of(conceptSet1, conceptSet2, conceptSet3));
    assertThat(isValid).isFalse();
  }

  @Test
  public void testAcceptsEmptyConceptSetIfDomainIsPopulated() {
    final DbConceptSet conceptSet1 = new DbConceptSet();
    conceptSet1.setDomain((short) Domain.DEVICE.ordinal());
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder().addConceptId(1L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder().addConceptId(2L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder().addConceptId(3L).addStandard(true).build();
    conceptSet1.setConceptSetConceptIds(
        ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2, dbConceptSetConceptId3));

    final DbConceptSet conceptSet2 = new DbConceptSet();
    conceptSet2.setDomain((short) Domain.PERSON.ordinal());
    DbConceptSetConceptId dbConceptSetConceptId4 =
        DbConceptSetConceptId.builder().addConceptId(4L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId5 =
        DbConceptSetConceptId.builder().addConceptId(5L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId6 =
        DbConceptSetConceptId.builder().addConceptId(6L).addStandard(true).build();
    conceptSet2.setConceptSetConceptIds(
        ImmutableSet.of(dbConceptSetConceptId4, dbConceptSetConceptId5, dbConceptSetConceptId6));

    final DbConceptSet conceptSet3 = new DbConceptSet();
    conceptSet3.setDomain((short) Domain.DEVICE.ordinal());
    conceptSet3.setConceptSetConceptIds(Collections.emptySet());

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
    DbConceptSet conceptSet1 = buildConceptSet(1L, domain1, true, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, domain1, true, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            domain1, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.map(String::trim).orElse(""))
        .isEqualTo(
            "( condition_concept_id in  (select distinct c.concept_id\n"
                + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
                + "join (select cast(cr.id as string) as id\n"
                + "from `${projectId}.${dataSetId}.cb_criteria` cr\n"
                + "where domain_id = 'CONDITION'\n"
                + "and is_standard = 1\n"
                + "and concept_id in (3, 2, 1, 6, 5, 4)\n"
                + "and is_selectable = 1\n"
                + "and full_text like '%[condition_rank1]%') a\n"
                + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%'))\n"
                + "where domain_id = 'CONDITION'\n"
                + "and is_standard = 1\n"
                + "and is_selectable = 1))");
  }

  @Test
  public void testBuildConceptIdListClause_differentDomains() {
    DbConceptSet conceptSet1 =
        buildConceptSet(1L, Domain.CONDITION, true, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, Domain.DRUG, true, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            Domain.CONDITION, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.map(String::trim).orElse(""))
        .isEqualTo(
            "( condition_concept_id in  (select distinct c.concept_id\n"
                + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
                + "join (select cast(cr.id as string) as id\n"
                + "from `${projectId}.${dataSetId}.cb_criteria` cr\n"
                + "where domain_id = 'CONDITION'\n"
                + "and is_standard = 1\n"
                + "and concept_id in (3, 2, 1)\n"
                + "and is_selectable = 1\n"
                + "and full_text like '%[condition_rank1]%') a\n"
                + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%'))\n"
                + "where domain_id = 'CONDITION'\n"
                + "and is_standard = 1\n"
                + "and is_selectable = 1))");
  }

  @Test
  public void testBuildConceptIdListClause_noClauseForPersonDomain() {
    DbConceptSet conceptSet1 =
        buildConceptSet(1L, Domain.CONDITION, true, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, Domain.DRUG, true, ImmutableSet.of(4L, 5L, 6L));
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
            .domainValuePairs(ImmutableList.of(new DomainValuePair()))
            .name("blah")
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
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

  @Test
  public void testFITBITDomainToBigQueryConfig() {
    mockLinkingTableQuery(
        ImmutableList.of(
            "FROM `" + TEST_CDR_TABLE + ".heart_rate_minute_level` heart_rate_minute_level"));
    mockDsLinkingTableForFitbit();
    final DataSetRequest dataSetRequest =
        new DataSetRequest()
            .conceptSetIds(Collections.emptyList())
            .cohortIds(Collections.emptyList())
            .domainValuePairs(ImmutableList.of(new DomainValuePair()))
            .name("blah")
            .prePackagedConceptSet(
                ImmutableList.of(PrePackagedConceptSetEnum.FITBIT_HEART_RATE_LEVEL))
            .cohortIds(Collections.emptyList())
            .domainValuePairs(
                ImmutableList.of(
                    new DomainValuePair().domain(Domain.FITBIT_HEART_RATE_LEVEL).value("PERSON_ID"),
                    new DomainValuePair()
                        .domain(Domain.FITBIT_HEART_RATE_LEVEL)
                        .value("DATETIME")));
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
    assertThat(result.get(Domain.FITBIT_HEART_RATE_LEVEL.name()).getQuery())
        .contains("GROUP BY PERSON_ID, DATE");
  }

  @Test
  public void testDataDictionary() {
    createDbDsDataDictionaryEntry();
    DataDictionaryEntry dataDictionaryEntry =
        dataSetServiceImpl.findDataDictionaryEntry("gender", "PERSON");
    assertThat(dataDictionaryEntry).isNotNull();
    assertThat(dataDictionaryEntry.getDescription()).isEqualTo("Gender testing");
  }

  @Test
  public void testGetPersonIdsWithWholeGenome_cohorts() {
    mockPersonIdQuery();
    when(cohortDao.findAllByCohortIdIn(anyList()))
        .thenReturn(ImmutableList.of(buildSimpleCohort(), buildSimpleCohort()));

    DbDataset dataset = new DbDataset();
    dataset.setCohortIds(ImmutableList.of(1L, 2L));
    dataSetServiceImpl.getPersonIdsWithWholeGenome(dataset);

    // Two participant criteria, one per cohort.
    verify(mockCohortQueryBuilder)
        .buildUnionedParticipantIdQuery(argThat(criteriaList -> criteriaList.size() == 2));
  }

  @Test
  public void testGetPersonIdsWithWholeGenome_allParticipants() {
    mockPersonIdQuery();

    DbDataset dataset = new DbDataset();
    dataset.setIncludesAllParticipants(true);
    dataSetServiceImpl.getPersonIdsWithWholeGenome(dataset);

    // Expect one participant criteria: "has WGS".
    // Note: this is dipping too much into implementation, but options are limited with the high
    // amount of mocking in this test.
    verify(mockCohortQueryBuilder)
        .buildUnionedParticipantIdQuery(argThat(criteriaList -> criteriaList.size() == 1));
  }

  @Test
  public void testGetValueListFromDomain() {
    mockDomainTableFields();
    List<DomainValue> conditionDomainValueList =
        dataSetServiceImpl.getValueListFromDomain("CONDITION");
    assertThat(conditionDomainValueList.size()).isEqualTo(2);

    List<DomainValue> measurementDomainValueList =
        dataSetServiceImpl.getValueListFromDomain("PHYSICAL_MEASUREMENT_CSS");
    assertThat(measurementDomainValueList.size()).isEqualTo(1);
  }

  @Test
  public void testGetDbDataSets_cohort() {
    when(cohortDao.findById(cohort.getCohortId())).thenReturn(Optional.of(cohort));

    DbDataset dbDataset = new DbDataset();
    dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset.setWorkspaceId(cohort.getWorkspaceId());
    dataSetServiceImpl.saveDataSet(dbDataset);

    when(dataSetDao.findDataSetsByCohortIdsAndWorkspaceId(
            cohort.getCohortId(), cohort.getWorkspaceId()))
        .thenReturn(ImmutableList.of(dbDataset));

    List<DbDataset> dbDatasets =
        dataSetServiceImpl.getDbDataSets(
            cohort.getWorkspaceId(), ResourceType.COHORT, cohort.getCohortId());
    assertThat(dbDatasets.size()).isEqualTo(1);
    assertThat(dbDatasets.get(0)).isEqualTo(dbDataset);
  }

  @Test(expected = NotFoundException.class)
  public void testGetDbDataSets_cohortWrongWorkspace() {
    when(cohortDao.findById(cohort.getCohortId())).thenReturn(Optional.of(cohort));

    DbDataset dbDataset = new DbDataset();
    dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset.setWorkspaceId(cohort.getWorkspaceId());
    dataSetServiceImpl.saveDataSet(dbDataset);

    dataSetServiceImpl.getDbDataSets(101L, ResourceType.COHORT, cohort.getCohortId());
  }

  @Test
  public void testGetDbDataSets_conceptSet() {
    long WORKSPACE_ID = 1L;

    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(WORKSPACE_ID);

    when(conceptSetDao.findById(dbConceptSet.getConceptSetId())).thenReturn(Optional.of(dbConceptSet));

    DbDataset dbDataset = new DbDataset();
    dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    dbDataset.setWorkspaceId(WORKSPACE_ID);
    dataSetServiceImpl.saveDataSet(dbDataset);

    when(dataSetDao.findDataSetsByConceptSetIdsAndWorkspaceId(
            dbConceptSet.getConceptSetId(), WORKSPACE_ID))
        .thenReturn(ImmutableList.of(dbDataset));

    List<DbDataset> dbDatasets =
        dataSetServiceImpl.getDbDataSets(
            WORKSPACE_ID, ResourceType.CONCEPT_SET, dbConceptSet.getConceptSetId());
    assertThat(dbDatasets.size()).isEqualTo(1);
    assertThat(dbDatasets.get(0)).isEqualTo(dbDataset);
  }

  @Test(expected = NotFoundException.class)
  public void testGetDbDataSets_conceptSetWrongWorkspace() {
    long WORKSPACE_ID = 1L;

    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(WORKSPACE_ID);

    when(conceptSetDao.findById(dbConceptSet.getConceptSetId())).thenReturn(Optional.of(dbConceptSet));

    DbDataset dbDataset = new DbDataset();
    dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    dbDataset.setWorkspaceId(WORKSPACE_ID);
    dataSetServiceImpl.saveDataSet(dbDataset);

    dataSetServiceImpl.getDbDataSets(
        101L, ResourceType.CONCEPT_SET, dbConceptSet.getConceptSetId());
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateDataSet_wrongWorkspace() {
    DbDataset dbDataset = new DbDataset();
    dbDataset.setDataSetId(1L);
    dbDataset.setWorkspaceId(2L);

    when(dataSetDao.findByDataSetIdAndWorkspaceId(
            dbDataset.getDataSetId(), dbDataset.getWorkspaceId()))
        .thenReturn(Optional.empty());

    DataSetRequest request = buildEmptyRequest();
    dataSetServiceImpl.updateDataSet(dbDataset.getWorkspaceId(), dbDataset.getDataSetId(), request);
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteDataSet_wrongWorkspace() {
    DbDataset dbDataset = new DbDataset();
    dbDataset.setDataSetId(1L);
    dbDataset.setWorkspaceId(2L);

    when(dataSetDao.findByDataSetIdAndWorkspaceId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    dataSetServiceImpl.deleteDataSet(dbDataset.getDataSetId(), dbDataset.getWorkspaceId());
  }

  private void mockDomainTableFields() {
    FieldList conditionList =
        FieldList.of(
            ImmutableList.of(
                Field.of("OMOP_SQL_Condition", LegacySQLTypeName.STRING),
                Field.of("JOIN_VALUE", LegacySQLTypeName.STRING)));

    FieldList measurementList =
        FieldList.of(ImmutableList.of(Field.of("OMOP_SQL_M", LegacySQLTypeName.STRING)));
    doReturn(conditionList).when(mockBigQueryService).getTableFieldsFromDomain(Domain.CONDITION);
    doReturn(measurementList)
        .when(mockBigQueryService)
        .getTableFieldsFromDomain(Domain.MEASUREMENT);
  }

  private void mockDsLinkingTableForFitbit() {
    DbDSLinking dbDSLinkingFitbit_personId = new DbDSLinking();
    dbDSLinkingFitbit_personId.setDenormalizedName("PERSON_ID");
    dbDSLinkingFitbit_personId.setDomain(Domain.FITBIT_HEART_RATE_LEVEL.name());
    dbDSLinkingFitbit_personId.setOmopSql("heart_rate_minute_level.PERSON_ID\n");
    dbDSLinkingFitbit_personId.setJoinValue(
        "FROM `" + TEST_CDR_TABLE + ".heart_rate_minute_level` heart_rate_minute_level");

    DbDSLinking dbDSLinkingFitbit_date = new DbDSLinking();
    dbDSLinkingFitbit_date.setDenormalizedName("DATETIME");
    dbDSLinkingFitbit_date.setDomain(Domain.FITBIT_HEART_RATE_LEVEL.name());
    dbDSLinkingFitbit_date.setOmopSql("CAST(heart_rate_minute_level.datetime as DATE) as date");
    dbDSLinkingFitbit_date.setJoinValue(
        "FROM `" + TEST_CDR_TABLE + ".heart_rate_minute_level` heart_rate_minute_level");
    doReturn(ImmutableList.of(dbDSLinkingFitbit_personId, dbDSLinkingFitbit_date))
        .when(dsLinkingDao)
        .findByDomainAndDenormalizedNameIn(
            StringUtils.capitalize(Domain.FITBIT_HEART_RATE_LEVEL.name().toLowerCase()),
            ImmutableList.of("CORE_TABLE_FOR_DOMAIN", "PERSON_ID", "DATETIME"));
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

  private void mockPersonIdQuery() {
    final TableResult tableResultMock = mock(TableResult.class);

    final FieldList schema =
        FieldList.of(ImmutableList.of(Field.of("person_id", LegacySQLTypeName.STRING)));

    doReturn(
            ImmutableList.of(
                FieldValueList.of(
                    ImmutableList.of(FieldValue.of(Attribute.PRIMITIVE, "1")), schema),
                FieldValueList.of(
                    ImmutableList.of(FieldValue.of(Attribute.PRIMITIVE, "2")), schema)))
        .when(tableResultMock)
        .getValues();

    doReturn(tableResultMock).when(mockBigQueryService).executeQuery(any());
  }

  private void createDbDsDataDictionaryEntry() {
    DbDSDataDictionary dsDataDictionary = new DbDSDataDictionary();
    dsDataDictionary.setDomain("PERSON");
    dsDataDictionary.setFieldName("gender");
    dsDataDictionary.setDescription("Gender testing");
    dsDataDictionary.setFieldType("string");
    dsDataDictionary.setRelevantOmopTable("person");
    dsDataDictionaryDao.save(dsDataDictionary);
  }
}
