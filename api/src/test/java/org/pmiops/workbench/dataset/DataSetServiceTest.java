package org.pmiops.workbench.dataset;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import java.sql.Timestamp;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetServiceImpl.QueryAndParameters;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.DomainWithDomainValues;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.TerraJobStatus;
import org.pmiops.workbench.test.CohortDefinitions;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DataSetServiceTest {

  private static final QueryJobConfiguration QUERY_JOB_CONFIGURATION_1 =
      QueryJobConfiguration.newBuilder(
              "SELECT person_id FROM `${projectId}.${dataSetId}.person` person")
          .addNamedParameter(
              "foo",
              QueryParameterValue.newBuilder()
                  .setType(StandardSQLTypeName.INT64)
                  .setValue(Long.toString(101L))
                  .build())
          .build();
  private static final String TEST_CDR_PROJECT_ID = "all-of-us-ehr-dev";
  private static final String TEST_CDR_DATA_SET_ID = "synthetic_cdr20180606";
  private static final String TEST_CDR_TABLE = TEST_CDR_PROJECT_ID + "." + TEST_CDR_DATA_SET_ID;
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @Autowired private CohortDao cohortDao;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private DSLinkingDao dsLinkingDao;
  @Autowired private DSDataDictionaryDao dsDataDictionaryDao;
  @Autowired private CohortQueryBuilder mockCohortQueryBuilder;
  @Autowired private BigQueryService mockBigQueryService;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private DataSetServiceImpl dataSetServiceImpl;
  @Autowired private WgsExtractCromwellSubmissionDao submissionDao;
  @Autowired private UserDao userDao;
  @Autowired private UserRecentResourceService userRecentResourceService;

  private DbWorkspace workspace;
  private DbCohort cohort;
  private DbDataset dbDataset;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    DataSetMapperImpl.class,
    DataSetServiceImpl.class,
    ConceptSetMapperImpl.class,
    CohortService.class,
    CohortMapperImpl.class,
    ConceptSetService.class
  })
  @MockBean({
    BigQueryService.class,
    DbUser.class,
    CommonMappers.class,
    CohortBuilderService.class,
    ConceptBigQueryService.class,
    CohortQueryBuilder.class,
    UserRecentResourceService.class
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

  @BeforeEach
  public void setUp() {
    workspace = workspaceDao.save(new DbWorkspace());
    cohort = cohortDao.save(buildSimpleCohort(workspace));
    when(mockCohortQueryBuilder.buildParticipantIdQuery(any()))
        .thenReturn(QUERY_JOB_CONFIGURATION_1);
    dbDataset = createDbDataSetEntry();
  }

  private DbCohort buildSimpleCohort(DbWorkspace workspace) {
    final CohortDefinition cohortDefinition = CohortDefinitions.males();
    final String cohortCriteria = new Gson().toJson(cohortDefinition);

    final DbCohort cohortDbModel = new DbCohort();
    cohortDbModel.setType("foo");
    cohortDbModel.setWorkspaceId(workspace.getWorkspaceId());
    cohortDbModel.setCriteria(cohortCriteria);
    cohortDbModel.setCreator(buildUser());
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

  private DbUser buildUser() {
    DbUser dbUser = new DbUser();
    dbUser.setFamilyName("Family Name");
    dbUser.setContactEmail("xyz@mock.com");
    return userDao.save(dbUser);
  }

  private static DataSetRequest buildEmptyRequest() {
    return new DataSetRequest()
        .cohortIds(Collections.emptyList())
        .conceptSetIds(Collections.emptyList())
        .domainValuePairs(Collections.emptyList())
        .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE));
  }

  @Test
  public void testThrowsForNoCohortOrConcept() {
    final DataSetRequest invalidRequest = buildEmptyRequest();
    invalidRequest.setDomainValuePairs(
        ImmutableList.of(new DomainValuePair().domain(Domain.CONDITION)));
    assertThrows(
        BadRequestException.class, () -> dataSetServiceImpl.domainToBigQueryConfig(invalidRequest));
  }

  @Test
  public void testGetsCohortQueryStringAndCollectsNamedParameters() {
    final DbCohort cohortDbModel = buildSimpleCohort(workspace);
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
    mockPersonIdQuery();
    Domain domain1 = Domain.CONDITION;
    DbConceptSet conceptSet1 = buildConceptSet(1L, domain1, true, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, domain1, true, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            domain1, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.map(String::trim).orElse(""))
        .isEqualTo(
            "( condition_source_concept_id IN  (SELECT DISTINCT c.concept_id\n"
                + "FROM `${projectId}.${dataSetId}.cb_criteria` c\n"
                + "JOIN (select cast(cr.id as string) as id\n"
                + "FROM `${projectId}.${dataSetId}.cb_criteria` cr\n"
                + "WHERE concept_id IN (1, 2)\n"
                + "AND full_text LIKE '%_rank1]%') a\n"
                + "ON (c.path LIKE CONCAT('%.', a.id, '.%') OR c.path LIKE CONCAT('%.', a.id) OR c.path LIKE CONCAT(a.id, '.%') OR c.path = a.id)\n"
                + "WHERE is_standard = 0\n"
                + "AND is_selectable = 1))");
  }

  @Test
  public void testBuildConceptIdListClause_differentDomains() {
    mockPersonIdQuery();
    DbConceptSet conceptSet1 =
        buildConceptSet(1L, Domain.CONDITION, true, ImmutableSet.of(1L, 2L, 3L));
    DbConceptSet conceptSet2 = buildConceptSet(2L, Domain.DRUG, true, ImmutableSet.of(4L, 5L, 6L));
    Optional<String> listClauseMaybe =
        dataSetServiceImpl.buildConceptIdListClause(
            Domain.CONDITION, ImmutableList.of(conceptSet1, conceptSet2));
    assertThat(listClauseMaybe.map(String::trim).orElse(""))
        .isEqualTo(
            "( condition_source_concept_id IN  (SELECT DISTINCT c.concept_id\n"
                + "FROM `${projectId}.${dataSetId}.cb_criteria` c\n"
                + "JOIN (select cast(cr.id as string) as id\n"
                + "FROM `${projectId}.${dataSetId}.cb_criteria` cr\n"
                + "WHERE concept_id IN (1, 2)\n"
                + "AND full_text LIKE '%_rank1]%') a\n"
                + "ON (c.path LIKE CONCAT('%.', a.id, '.%') OR c.path LIKE CONCAT('%.', a.id) OR c.path LIKE CONCAT(a.id, '.%') OR c.path = a.id)\n"
                + "WHERE is_standard = 0\n"
                + "AND is_selectable = 1))");
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

    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    dbConceptSet = conceptSetDao.save(dbConceptSet);

    final DataSetRequest dataSetRequest =
        new DataSetRequest()
            .conceptSetIds(ImmutableList.of(cohort.getCohortId()))
            .cohortIds(ImmutableList.of(dbConceptSet.getConceptSetId()))
            .domainValuePairs(ImmutableList.of(new DomainValuePair()))
            .name("blah")
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
            .cohortIds(ImmutableList.of(cohort.getCohortId()))
            .domainValuePairs(
                ImmutableList.of(new DomainValuePair().domain(Domain.PERSON).value("PERSON_ID")));

    final Map<String, QueryJobConfiguration> result =
        dataSetServiceImpl.domainToBigQueryConfig(dataSetRequest);
    assertThat(result).hasSize(1);
    assertThat(result.get("PERSON").getNamedParameters()).hasSize(1);
    assertThat(result.get("PERSON").getNamedParameters().get("foo_1").getValue()).isEqualTo("101");
  }

  @Test
  public void testFitbitDomainToBigQueryConfig() {
    mockLinkingTableQuery(
        ImmutableList.of(
            "FROM `" + TEST_CDR_TABLE + ".heart_rate_minute_level` heart_rate_minute_level"));
    setupDsLinkingTableForFitbit();
    final DataSetRequest dataSetRequest =
        new DataSetRequest()
            .conceptSetIds(ImmutableList.of())
            .cohortIds(ImmutableList.of(cohort.getCohortId()))
            .domainValuePairs(ImmutableList.of(new DomainValuePair()))
            .name("blah")
            .prePackagedConceptSet(
                ImmutableList.of(PrePackagedConceptSetEnum.FITBIT_HEART_RATE_LEVEL))
            .domainValuePairs(
                ImmutableList.of(
                    new DomainValuePair().domain(Domain.FITBIT_HEART_RATE_LEVEL).value("PERSON_ID"),
                    new DomainValuePair()
                        .domain(Domain.FITBIT_HEART_RATE_LEVEL)
                        .value("DATETIME")));

    final Map<String, QueryJobConfiguration> result =
        dataSetServiceImpl.domainToBigQueryConfig(dataSetRequest);
    assertThat(result).hasSize(1);
    assertThat(result.get(Domain.FITBIT_HEART_RATE_LEVEL.name()).getQuery())
        .contains("GROUP BY person_id, date");
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
    DbCohort cohort2 = cohortDao.save(buildSimpleCohort(workspace));

    DbDataset dataset = new DbDataset();
    dataset.setCohortIds(ImmutableList.of(cohort.getCohortId(), cohort2.getCohortId()));
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
    List<DomainWithDomainValues> observationDomainValueList =
        dataSetServiceImpl.getValueListFromDomain(1L, "OBSERVATION");
    assertThat(observationDomainValueList.get(0).getItems().size()).isEqualTo(2);

    List<DomainWithDomainValues> measurementDomainValueList =
        dataSetServiceImpl.getValueListFromDomain(2L, "PHYSICAL_MEASUREMENT_CSS");
    assertThat(measurementDomainValueList.get(0).getItems().size()).isEqualTo(1);
  }

  @Test
  public void testGetValueListFromDomainPersonHasEhrData() {
    mockPersonDomainTableFields();
    List<DomainWithDomainValues> personDomainValueList =
        dataSetServiceImpl.getValueListFromDomain(null, "PERSON_HAS_EHR_DATA");
    assertThat(personDomainValueList.get(0).getItems().size()).isEqualTo(2);
    assertThat(
        personDomainValueList.get(0).getDomain().equals(Domain.PERSON_HAS_EHR_DATA.toString()));
  }

  @Test
  public void testGetDataSets_cohort() {
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());

    DbDataset dbDataset = new DbDataset();
    dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset.setWorkspaceId(cohort.getWorkspaceId());
    dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    DataSet dataset = dataSetServiceImpl.saveDataSet(dbDataset);

    List<DataSet> datasets =
        dataSetServiceImpl.getDataSets(
            cohort.getWorkspaceId(), ResourceType.COHORT, cohort.getCohortId());
    assertThat(datasets).containsExactly(dataset);
  }

  @Test
  public void testGetDataSets_cohortWrongWorkspace() {
    assertThrows(
        NotFoundException.class,
        () -> {
          DbConceptSet dbConceptSet = new DbConceptSet();
          dbConceptSet.setConceptSetId(3L);
          dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());

          DbDataset dbDataset = new DbDataset();
          dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
          dbDataset.setWorkspaceId(cohort.getWorkspaceId());
          dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
          dataSetServiceImpl.saveDataSet(dbDataset);
          dataSetServiceImpl.getDataSets(101L, ResourceType.COHORT, cohort.getCohortId());
        });
  }

  @Test
  public void testGetDataSets_cohort_onlyvalidDataSet() {
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    dbConceptSet = conceptSetDao.save(dbConceptSet);

    DbDataset dbDataset = new DbDataset();
    dbDataset.setName("Data Set dirty 67");
    dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset.setWorkspaceId(cohort.getWorkspaceId());
    dbDataset.setInvalid(false);
    dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    DataSet dataset = dataSetServiceImpl.saveDataSet(dbDataset);

    DbDataset dbDataset_invalid = new DbDataset();
    dbDataset_invalid.setName("Data Set dirty");
    dbDataset_invalid.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset_invalid.setWorkspaceId(cohort.getWorkspaceId());
    dbDataset_invalid.setInvalid(true);
    dbDataset_invalid.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    dataSetServiceImpl.saveDataSet(dbDataset_invalid);

    List<DataSet> datasets =
        dataSetServiceImpl.getDataSets(
            cohort.getWorkspaceId(), ResourceType.COHORT, cohort.getCohortId());
    assertThat(datasets.size()).isEqualTo(1);
    assertThat(datasets).containsExactly(dataset);
  }

  @Test
  public void testGetDataSets_conceptSet() {
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    dbConceptSet = conceptSetDao.save(dbConceptSet);

    DbDataset dbDataset = new DbDataset();
    dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    dbDataset.setWorkspaceId(workspace.getWorkspaceId());
    dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    DataSet dataset = dataSetServiceImpl.saveDataSet(dbDataset);

    List<DataSet> datasets =
        dataSetServiceImpl.getDataSets(
            workspace.getWorkspaceId(), ResourceType.CONCEPT_SET, dbConceptSet.getConceptSetId());
    assertThat(datasets).containsExactly(dataset);
  }

  @Test
  public void testGetDataSets_conceptSet_onlyValidDataSet() {
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    dbConceptSet = conceptSetDao.save(dbConceptSet);

    DbDataset dbDataset = new DbDataset();
    dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    dbDataset.setWorkspaceId(workspace.getWorkspaceId());
    dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset.setInvalid(false);
    DataSet dataset = dataSetServiceImpl.saveDataSet(dbDataset);

    DbDataset dbDataset_invalid = new DbDataset();
    dbDataset_invalid.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    dbDataset_invalid.setWorkspaceId(workspace.getWorkspaceId());
    dbDataset_invalid.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset_invalid.setInvalid(true);
    dataSetServiceImpl.saveDataSet(dbDataset_invalid);

    List<DataSet> datasets =
        dataSetServiceImpl.getDataSets(
            workspace.getWorkspaceId(), ResourceType.CONCEPT_SET, dbConceptSet.getConceptSetId());
    assertThat(datasets.size()).isEqualTo(1);
    assertThat(datasets).containsExactly(dataset);
  }

  @Test
  public void testGetDataSets_conceptSetWrongWorkspace() {
    assertThrows(
        NotFoundException.class,
        () -> {
          long WORKSPACE_ID = 1L;
          DbConceptSet dbConceptSet = new DbConceptSet();
          dbConceptSet.setConceptSetId(3L);
          dbConceptSet.setWorkspaceId(WORKSPACE_ID);
          dbConceptSet = conceptSetDao.save(dbConceptSet);
          DbDataset dbDataset = new DbDataset();
          dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
          dbDataset.setWorkspaceId(WORKSPACE_ID);
          dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
          dataSetServiceImpl.saveDataSet(dbDataset);
          dataSetServiceImpl.getDataSets(
              101L, ResourceType.CONCEPT_SET, dbConceptSet.getConceptSetId());
        });
  }

  @Test
  public void testUpdateDataSet_wrongWorkspace() {
    assertThrows(
        NotFoundException.class,
        () -> {
          DbDataset dbDataset = new DbDataset();
          dbDataset.setDataSetId(1L);
          dbDataset.setWorkspaceId(2L);
          DataSetRequest request = buildEmptyRequest();
          dataSetServiceImpl.updateDataSet(
              dbDataset.getWorkspaceId(), dbDataset.getDataSetId(), request);
        });
  }

  @Test
  public void testDeleteDataSet_wrongWorkspace() {
    assertThrows(
        NotFoundException.class,
        () -> {
          DbDataset dbDataset = new DbDataset();
          dbDataset.setDataSetId(1L);
          dbDataset.setWorkspaceId(2L);
          dataSetServiceImpl.deleteDataSet(dbDataset.getDataSetId(), dbDataset.getWorkspaceId());
        });
  }

  //  valid extraction exists but has an empty string directory (extractions that were created
  // before
  //      merged)

  @Test
  public void test_getExtractionDirectory() {
    final String outputDir = "gs://gcs_dir/vcfs";
    DbDataset dbDataset = new DbDataset();
    dbDataset = dataSetDao.save(dbDataset);

    DbWgsExtractCromwellSubmission dbSubmission = new DbWgsExtractCromwellSubmission();
    dbSubmission.setTerraStatusEnum(TerraJobStatus.RUNNING);
    dbSubmission.setOutputDir(outputDir + "/");
    dbSubmission.setDataset(dbDataset);
    submissionDao.save(dbSubmission);

    assertThat(dataSetServiceImpl.getExtractionDirectory(dbDataset.getDataSetId()).get())
        .isEqualTo(outputDir);
  }

  @Test
  public void test_getExtractionDirectory_moreRecent() {
    final String outputDir = "gs://gcs_dir/vcfs";
    DbDataset dbDataset = new DbDataset();
    dbDataset = dataSetDao.save(dbDataset);

    DbWgsExtractCromwellSubmission dbSubmission = new DbWgsExtractCromwellSubmission();
    dbSubmission.setCreationTime(new Timestamp(CLOCK.instant().toEpochMilli()));
    dbSubmission.setTerraStatusEnum(TerraJobStatus.RUNNING);
    dbSubmission.setOutputDir("should not be fetched");
    dbSubmission.setDataset(dbDataset);
    submissionDao.save(dbSubmission);

    CLOCK.increment(100000);
    DbWgsExtractCromwellSubmission moreRecentSubmission = new DbWgsExtractCromwellSubmission();
    moreRecentSubmission.setCreationTime(new Timestamp(CLOCK.instant().toEpochMilli()));
    moreRecentSubmission.setTerraStatusEnum(TerraJobStatus.RUNNING);
    moreRecentSubmission.setOutputDir(outputDir);
    moreRecentSubmission.setDataset(dbDataset);
    submissionDao.save(moreRecentSubmission);

    assertThat(dataSetServiceImpl.getExtractionDirectory(dbDataset.getDataSetId()).get())
        .isEqualTo(outputDir);
  }

  @Test
  public void test_getExtractionDirectory_datasetDoesNotExist() {
    assertThat(dataSetServiceImpl.getExtractionDirectory(123L).isPresent()).isFalse();
  }

  @Test
  public void test_getExtractionDirectory_noSubmission() {
    DbDataset dbDataset = new DbDataset();
    dbDataset = dataSetDao.save(dbDataset);

    assertThat(dataSetServiceImpl.getExtractionDirectory(dbDataset.getDataSetId()).isPresent())
        .isFalse();
  }

  @Test
  public void test_getExtractionDirectory_failedSubmissionOnly() {
    final String outputDir = "gs://gcs_dir/vcfs";
    DbDataset dbDataset = new DbDataset();
    dbDataset = dataSetDao.save(dbDataset);

    DbWgsExtractCromwellSubmission dbSubmission = new DbWgsExtractCromwellSubmission();
    dbSubmission.setTerraStatusEnum(TerraJobStatus.FAILED);
    dbSubmission.setOutputDir(outputDir + "/");
    dbSubmission.setDataset(dbDataset);
    submissionDao.save(dbSubmission);

    assertThat(dataSetServiceImpl.getExtractionDirectory(dbDataset.getDataSetId()).isPresent())
        .isFalse();
  }

  @Test
  public void test_getExtractionDirectory_emptyDirectory() {
    DbDataset dbDataset = new DbDataset();
    dbDataset = dataSetDao.save(dbDataset);

    DbWgsExtractCromwellSubmission dbSubmission = new DbWgsExtractCromwellSubmission();
    dbSubmission.setTerraStatusEnum(TerraJobStatus.SUCCEEDED);
    dbSubmission.setOutputDir("");
    dbSubmission.setDataset(dbDataset);
    submissionDao.save(dbSubmission);

    assertThat(dataSetServiceImpl.getExtractionDirectory(dbDataset.getDataSetId()).isPresent())
        .isFalse();
  }

  @Test
  public void test_userRecentModifiedEntry_saveDataSet() {
    DataSet dataset = dataSetServiceImpl.saveDataSet(dbDataset);
    verify(userRecentResourceService)
        .updateDataSetEntry(dbDataset.getWorkspaceId(), dbDataset.getCreatorId(), dataset.getId());
  }

  @Test
  public void test_userRecentModifiedEntry_deleteDataSet() {
    dbDataset = dataSetDao.save(dbDataset);
    long dataSetId = dbDataset.getDataSetId();
    dataSetServiceImpl.deleteDataSet(dbDataset.getWorkspaceId(), dataSetId);
    verify(userRecentResourceService)
        .deleteDataSetEntry(dbDataset.getWorkspaceId(), dbDataset.getCreatorId(), dataSetId);
  }

  private void mockDomainTableFields() {
    FieldList observationList =
        FieldList.of(
            ImmutableList.of(
                Field.of("OMOP_SQL_Condition", LegacySQLTypeName.STRING),
                Field.of("JOIN_VALUE", LegacySQLTypeName.STRING)));

    FieldList measurementList =
        FieldList.of(ImmutableList.of(Field.of("OMOP_SQL_M", LegacySQLTypeName.STRING)));
    doReturn(observationList)
        .when(mockBigQueryService)
        .getTableFieldsFromDomain(Domain.OBSERVATION);
    doReturn(measurementList)
        .when(mockBigQueryService)
        .getTableFieldsFromDomain(Domain.MEASUREMENT);
  }

  private void mockPersonDomainTableFields() {
    FieldList personFieldList =
        FieldList.of(
            ImmutableList.of(
                Field.of("person_id", LegacySQLTypeName.INTEGER),
                Field.of("sex_at_birth", LegacySQLTypeName.STRING)));
    doReturn(personFieldList).when(mockBigQueryService).getTableFieldsFromDomain(Domain.PERSON);
  }

  private String normalizeDomainName(Domain d) {
    return StringUtils.capitalize(d.name().toLowerCase());
  }

  private void setupDsLinkingTableForFitbit() {
    DbDSLinking dbDSLinkingFitbitPersonId = new DbDSLinking();
    dbDSLinkingFitbitPersonId.setDenormalizedName("PERSON_ID");
    dbDSLinkingFitbitPersonId.setDomain(normalizeDomainName(Domain.FITBIT_HEART_RATE_LEVEL));
    dbDSLinkingFitbitPersonId.setOmopSql("heart_rate_minute_level.PERSON_ID\n");
    dbDSLinkingFitbitPersonId.setJoinValue(
        "FROM `" + TEST_CDR_TABLE + ".heart_rate_minute_level` heart_rate_minute_level");
    dsLinkingDao.save(dbDSLinkingFitbitPersonId);

    DbDSLinking dbDSLinkingFitbitDate = new DbDSLinking();
    dbDSLinkingFitbitDate.setDenormalizedName("DATETIME");
    dbDSLinkingFitbitDate.setDomain(normalizeDomainName(Domain.FITBIT_HEART_RATE_LEVEL));
    dbDSLinkingFitbitDate.setOmopSql("CAST(heart_rate_minute_level.datetime as DATE) as date");
    dbDSLinkingFitbitDate.setJoinValue(
        "FROM `" + TEST_CDR_TABLE + ".heart_rate_minute_level` heart_rate_minute_level");
    dsLinkingDao.save(dbDSLinkingFitbitDate);
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

  private DbDataset createDbDataSetEntry() {
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(3L);
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());

    DbDataset dbDataset = new DbDataset();
    dbDataset.setName("Data Set dirty 67");
    dbDataset.setCohortIds(ImmutableList.of(cohort.getCohortId()));
    dbDataset.setWorkspaceId(cohort.getWorkspaceId());
    dbDataset.setCreatorId(cohort.getCreator().getUserId());
    dbDataset.setInvalid(false);
    dbDataset.setConceptSetIds(ImmutableList.of(dbConceptSet.getConceptSetId()));
    return dbDataset;
  }
}
