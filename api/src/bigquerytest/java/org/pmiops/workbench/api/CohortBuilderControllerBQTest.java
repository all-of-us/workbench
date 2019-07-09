package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Provider;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({
  QueryBuilderFactory.class,
  BigQueryService.class,
  CloudStorageServiceImpl.class,
  CohortQueryBuilder.class,
  SearchGroupItemQueryBuilder.class,
  TestJpaConfig.class,
  CdrVersionService.class
})
@MockBean({FireCloudService.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerBQTest extends BigQueryBaseTest {

  private CohortBuilderController controller;

  private CdrVersion cdrVersion;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private CloudStorageService cloudStorageService;

  @Autowired private CohortQueryBuilder cohortQueryBuilder;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CriteriaDao criteriaDao;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private CdrVersionService cdrVersionService;

  @Autowired private CriteriaAttributeDao criteriaAttributeDao;

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @Mock private Provider<WorkbenchConfig> configProvider;

  @Mock private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
        "person", "death", "criteria", "criteria_ancestor", "search_person", "search_all_domains");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    WorkbenchConfig testConfig = new WorkbenchConfig();
    testConfig.elasticsearch = new WorkbenchConfig.ElasticsearchConfig();
    testConfig.elasticsearch.enableElasticsearchBackend = false;
    testConfig.cohortbuilder = new WorkbenchConfig.CohortBuilderConfig();
    testConfig.cohortbuilder.enableListSearch = true;
    when(configProvider.get()).thenReturn(testConfig);

    ElasticSearchService elasticSearchService =
        new ElasticSearchService(criteriaDao, cloudStorageService, configProvider);

    controller =
        new CohortBuilderController(
            bigQueryService,
            cohortQueryBuilder,
            criteriaDao,
            cbCriteriaDao,
            criteriaAttributeDao,
            cbCriteriaAttributeDao,
            cdrVersionDao,
            genderRaceEthnicityConceptProvider,
            cdrVersionService,
            elasticSearchService,
            configProvider);

    cdrVersion = new CdrVersion();
    cdrVersion.setCdrVersionId(1L);
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    cdrVersionDao.save(cdrVersion);
  }

  @Test
  public void validateAttribute() throws Exception {
    SearchParameter demo =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    Attribute attribute = new Attribute().name(AttrName.NUM);
    demo.attributes(Arrays.asList(attribute));
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(demo), new ArrayList<>());
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals("Bad Request: attribute operator null is not valid.", bre.getMessage());
    }

    attribute.operator(Operator.BETWEEN);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals("Bad Request: attribute operands are empty.", bre.getMessage());
    }

    attribute.operands(Arrays.asList("20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals(
          "Bad Request: attribute NUM can only have 2 operands when using the BETWEEN operator",
          bre.getMessage());
    }

    attribute.operands(Arrays.asList("s", "20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals("Bad Request: attribute NUM operands must be numeric.", bre.getMessage());
    }

    attribute.operands(Arrays.asList("10", "20"));
    attribute.operator(Operator.EQUAL);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals(
          "Bad Request: attribute NUM must have one operand when using the EQUAL operator.",
          bre.getMessage());
    }
  }

  @Test
  public void validateModifiers() throws Exception {
    SearchParameter searchParameter =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(searchParameter),
            Arrays.asList(modifier));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals("Bad Request: modifier operator null is not valid.", bre.getMessage());
    }

    modifier.operator(Operator.BETWEEN);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals("Bad Request: modifier operands are empty.", bre.getMessage());
    }

    modifier.operands(Arrays.asList("20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals(
          "Bad Request: modifier AGE_AT_EVENT can only have 2 operands when using the BETWEEN operator",
          bre.getMessage());
    }

    modifier.operands(Arrays.asList("s", "20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals(
          "Bad Request: modifier AGE_AT_EVENT operands must be numeric.", bre.getMessage());
    }

    modifier.operands(Arrays.asList("10", "20"));
    modifier.operator(Operator.EQUAL);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals(
          "Bad Request: modifier AGE_AT_EVENT must have one operand when using the EQUAL operator.",
          bre.getMessage());
    }

    modifier.name(ModifierType.EVENT_DATE);
    modifier.operands(Arrays.asList("10"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals("Bad Request: modifier EVENT_DATE must be a valid date.", bre.getMessage());
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChild() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .value("001.1")
            .standard(false)
            .ancestorData(false)
            .conceptId(1L);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void temporalGroupExceptions() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(1L);

    SearchGroupItem icd9SGI =
        new SearchGroupItem().type(DomainType.CONDITION.toString()).addSearchParametersItem(icd9);

    SearchGroup temporalGroup = new SearchGroup().items(Arrays.asList(icd9SGI)).temporal(true);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals(
          "Bad Request: search group item temporal group null is not valid.", bre.getMessage());
    }

    icd9SGI.temporalGroup(0);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertEquals(
          "Bad Request: Search Group Items must provided for 2 different temporal groups(0 or 1).",
          bre.getMessage());
    }
  }

  @Test
  public void firstMentionOfICD9WithModifiersOrSnomed5DaysAfterICD10WithModifiers()
      throws Exception {
    Modifier ageModifier =
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("25"));
    Modifier visitsModifier =
        new Modifier()
            .name(ModifierType.ENCOUNTERS)
            .operator(Operator.IN)
            .operands(Arrays.asList("1"));

    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(1L);
    SearchParameter icd10 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(9L);
    SearchParameter snomed =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(4L);

    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd9)
            .temporalGroup(0)
            .addModifiersItem(ageModifier);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd10)
            .temporalGroup(1)
            .addModifiersItem(visitsModifier);
    SearchGroupItem snomedSGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(snomed)
            .temporalGroup(0);

    // First Mention Of (ICD9 w/modifiers or Snomed) 5 Days After ICD10 w/modifiers
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(icd9SGI, snomedSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    // matches icd9SGI in group 0 and icd10SGI in group 1
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfICD9WithOccurrencesOrSnomed5DaysAfterICD10() throws Exception {
    Modifier occurrencesModifier =
        new Modifier()
            .name(ModifierType.NUM_OF_OCCURRENCES)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("2"));

    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(1L);
    SearchParameter icd10 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(9L);
    SearchParameter snomed =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(4L);

    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd9)
            .temporalGroup(0)
            .addModifiersItem(occurrencesModifier);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd10)
            .temporalGroup(1);
    SearchGroupItem snomedSGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(snomed)
            .temporalGroup(0);

    // First Mention Of (ICD9 w/modifiers or Snomed) 5 Days After ICD10 w/modifiers
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(icd9SGI, snomedSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    // matches icd9SGI in group 0 and icd10SGI in group 1
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfDrug5DaysBeforeICD10WithModifiers() throws Exception {
    CBCriteria drugNode1 =
        new CBCriteria()
            .parentId(99999)
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .conceptId("21600932")
            .group(true)
            .selectable(true);
    saveCriteriaWithPath("0", drugNode1);
    CBCriteria drugNode2 =
        new CBCriteria()
            .parentId(drugNode1.getId())
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .conceptId("1520218")
            .group(false)
            .selectable(true);
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);
    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (1520218, 1520218)");

    Modifier visitsModifier =
        new Modifier()
            .name(ModifierType.ENCOUNTERS)
            .operator(Operator.IN)
            .operands(Arrays.asList("1"));

    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .standard(false)
            .ancestorData(true)
            .group(true)
            .conceptId(21600932L);
    SearchParameter icd10 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(9L);

    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug)
            .temporalGroup(0);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd10)
            .temporalGroup(1)
            .addModifiersItem(visitsModifier);

    // First Mention Of Drug 5 Days Before ICD10 w/modifiers
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(drugSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_BEFORE)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(drugNode1.getId());
    cbCriteriaDao.delete(drugNode2.getId());
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void anyMentionOfICD9Parent5DaysAfterICD10Child() throws Exception {
    CBCriteria icd9Parent =
        new CBCriteria()
            .ancestorData(false)
            .code("001")
            .conceptId("0")
            .domainId(DomainType.CONDITION.toString())
            .group(true)
            .selectable(true)
            .standard(false)
            .synonyms("+[CONDITION_rank1]");
    saveCriteriaWithPath("0", icd9Parent);
    CBCriteria icd9Child =
        new CBCriteria()
            .ancestorData(false)
            .code("001.1")
            .conceptId("1")
            .domainId(DomainType.CONDITION.toString())
            .group(false)
            .selectable(true)
            .standard(false)
            .synonyms("+[CONDITION_rank1]");
    saveCriteriaWithPath(icd9Parent.getPath(), icd9Child);

    Modifier visitsModifier =
        new Modifier()
            .name(ModifierType.ENCOUNTERS)
            .operator(Operator.IN)
            .operands(Arrays.asList("1"));
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(true)
            .value("001")
            .conceptId(0L);
    SearchParameter icd10 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .standard(false)
            .ancestorData(false)
            .group(false)
            .conceptId(9L);

    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd9)
            .temporalGroup(0)
            .addModifiersItem(visitsModifier);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd10)
            .temporalGroup(1);

    // Any Mention Of ICD9 5 Days After ICD10
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(icd9SGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.ANY_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(icd9Parent.getId());
    cbCriteriaDao.delete(icd9Child.getId());
  }

  @Test
  public void anyMentionOfCPTWithIn5DaysOfVisit() throws Exception {
    SearchParameter cpt =
        new SearchParameter()
            .domain(DomainType.PROCEDURE.toString())
            .type(CriteriaType.CPT4.toString())
            .group(false)
            .ancestorData(false)
            .standard(false)
            .conceptId(10L);
    SearchParameter visit =
        new SearchParameter()
            .domain(DomainType.VISIT.toString())
            .type(CriteriaType.VISIT.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(1L);

    SearchGroupItem cptSGI =
        new SearchGroupItem()
            .type(DomainType.PROCEDURE.toString())
            .addSearchParametersItem(cpt)
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(DomainType.VISIT.toString())
            .addSearchParametersItem(visit)
            .temporalGroup(1);

    // Any Mention Of ICD10 Parent within 5 Days of visit
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(visitSGI, cptSGI))
            .temporal(true)
            .mention(TemporalMention.ANY_MENTION)
            .time(TemporalTime.WITHIN_X_DAYS_OF)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfDrugDuringSameEncounterAsMeasurement() throws Exception {
    CBCriteria drugCriteria =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId("11");
    saveCriteriaWithPath("0", drugCriteria);
    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (11, 11)");

    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId(11L);
    SearchParameter measurement =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);

    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug)
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement)
            .temporalGroup(1);

    // First Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(drugSGI, measurementSGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(drugCriteria.getId());
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurement() throws Exception {
    CBCriteria drugCriteria =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId("11");
    saveCriteriaWithPath("0", drugCriteria);
    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (11, 11)");

    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId(11L);
    SearchParameter measurement =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);

    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug)
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement)
            .temporalGroup(1);

    // Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(drugSGI, measurementSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(drugCriteria.getId());
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurementOrVisit() throws Exception {
    CBCriteria drugCriteria =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId("11");
    saveCriteriaWithPath("0", drugCriteria);
    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (11, 11)");

    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId(11L);
    SearchParameter measurement =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    SearchParameter visit =
        new SearchParameter()
            .domain(DomainType.VISIT.toString())
            .type(CriteriaType.VISIT.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(1L);

    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug)
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement)
            .temporalGroup(1);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(DomainType.VISIT.toString())
            .addSearchParametersItem(visit)
            .temporalGroup(1);

    // Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(drugSGI, measurementSGI, visitSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(drugCriteria.getId());
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void lastMentionOfMeasurementOrVisit5DaysAfterDrug() throws Exception {
    CBCriteria drugCriteria1 =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(true)
            .ancestorData(true)
            .standard(true)
            .conceptId("21600932");
    saveCriteriaWithPath("0", drugCriteria1);
    CBCriteria drugCriteria2 =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .group(false)
            .selectable(true)
            .ancestorData(true)
            .standard(true)
            .conceptId("1520218");
    saveCriteriaWithPath(drugCriteria1.getPath(), drugCriteria2);
    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (1520218, 1520218)");

    SearchParameter measurement =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    SearchParameter visit =
        new SearchParameter()
            .domain(DomainType.VISIT.toString())
            .type(CriteriaType.VISIT.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(1L);
    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(true)
            .ancestorData(true)
            .standard(true)
            .conceptId(21600932L);

    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement)
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(DomainType.VISIT.toString())
            .addSearchParametersItem(visit)
            .temporalGroup(0);
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug)
            .temporalGroup(1);

    // Last Mention Of Measurement or Visit 5 days after Drug
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(drugSGI, measurementSGI, visitSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(drugCriteria1.getId());
    cbCriteriaDao.delete(drugCriteria2.getId());
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEvent() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .value("001.1")
            .standard(false)
            .ancestorData(false)
            .conceptId(1L);

    Modifier modifier =
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("25"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEncounter() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.name())
            .group(false)
            .value("001.1")
            .standard(false)
            .ancestorData(false)
            .ancestorData(false)
            .conceptId(1L);

    Modifier modifier =
        new Modifier()
            .name(ModifierType.ENCOUNTERS)
            .operator(Operator.IN)
            .operands(Arrays.asList("1"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEventBetween() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .value("001.1")
            .ancestorData(false)
            .conceptId(1L);

    Modifier modifier =
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.BETWEEN)
            .operands(Arrays.asList("37", "39"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9), Arrays.asList(modifier));

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .value("001.1")
            .ancestorData(false)
            .conceptId(1L);

    Modifier modifier1 =
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("25"));
    Modifier modifier2 =
        new Modifier()
            .name(ModifierType.NUM_OF_OCCURRENCES)
            .operator(Operator.EQUAL)
            .operands(Arrays.asList("2"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9),
            Arrays.asList(modifier1, modifier2));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditioChildAgeAtEventAndOccurrencesAndEventDate()
      throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .value("001.1")
            .ancestorData(false)
            .conceptId(1L);
    SearchParameter icd92 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .value("001.1")
            .ancestorData(false)
            .conceptId(2L);

    Modifier modifier1 =
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("25"));
    Modifier modifier2 =
        new Modifier()
            .name(ModifierType.NUM_OF_OCCURRENCES)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("1"));
    Modifier modifier3 =
        new Modifier()
            .name(ModifierType.EVENT_DATE)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("2009-12-03"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9, icd92),
            Arrays.asList(modifier1, modifier2, modifier3));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEventDate() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .value("001.1")
            .ancestorData(false)
            .conceptId(1L);

    Modifier modifier =
        new Modifier()
            .name(ModifierType.EVENT_DATE)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("2009-12-03"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionNumOfOccurrences() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .value("001.1")
            .ancestorData(false)
            .conceptId(1L);

    Modifier modifier2 =
        new Modifier()
            .name(ModifierType.NUM_OF_OCCURRENCES)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("1"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9), Arrays.asList(modifier2));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ParentAndChild() throws Exception {
    SearchParameter icd9Child =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .value("001.1")
            .conceptId(1L)
            .ancestorData(false);
    SearchParameter icd9Parent =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(true)
            .standard(false)
            .value("001")
            .conceptId(1L)
            .ancestorData(false);

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9Child, icd9Parent),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionParent() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(true)
            .standard(false)
            .value("001")
            .ancestorData(false)
            .conceptId(2L);

    CBCriteria criteriaParent =
        new CBCriteria()
            .parentId(99999)
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .code("001")
            .name("Cholera")
            .count("19")
            .group(true)
            .selectable(true)
            .ancestorData(false)
            .conceptId("2");
    saveCriteriaWithPath("0", criteriaParent);
    CBCriteria criteriaChild =
        new CBCriteria()
            .parentId(criteriaParent.getId())
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .code("001.1")
            .name("Cholera")
            .count("19")
            .group(false)
            .selectable(true)
            .ancestorData(false)
            .conceptId("1");
    saveCriteriaWithPath(criteriaParent.getPath(), criteriaChild);

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(criteriaParent.getId());
    cbCriteriaDao.delete(criteriaChild.getId());
  }

  @Test
  public void countSubjectsDemoGender() throws Exception {
    SearchParameter demo =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .conceptId(8507L);

    SearchRequest searchRequest =
        createSearchRequests(DomainType.PERSON.toString(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoRace() throws Exception {
    SearchParameter demo =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.RACE.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .conceptId(1L);

    SearchRequest searchRequest =
        createSearchRequests(DomainType.PERSON.toString(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoEthnicity() throws Exception {
    SearchParameter demo =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.ETHNICITY.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .conceptId(9898L);

    SearchRequest searchRequest =
        createSearchRequests(DomainType.PERSON.toString(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoDec() throws Exception {
    SearchParameter demo =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.DECEASED.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .value("Deceased");

    SearchRequest searchRequest =
        createSearchRequests(DomainType.PERSON.toString(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoAge() throws Exception {
    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer lo = period.getYears() - 1;
    Integer hi = period.getYears() + 1;
    SearchParameter demo =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .group(false)
            .ancestorData(false)
            .standard(true);
    demo.attributes(
        Arrays.asList(
            new Attribute()
                .name(AttrName.AGE)
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList(lo.toString(), hi.toString()))));
    SearchRequest searchRequests =
        createSearchRequests(DomainType.PERSON.toString(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsICD9AndDemo() throws Exception {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .standard(false)
            .ancestorData(false)
            .conceptId(3L)
            .value("003.1");

    SearchParameter demoGenderSearchParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .conceptId(8507L);

    SearchParameter demoAgeSearchParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.AGE.toString())
            .standard(true)
            .ancestorData(false)
            .group(false);

    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer lo = period.getYears() - 1;
    Integer hi = period.getYears() + 1;

    demoAgeSearchParam.attributes(
        Arrays.asList(
            new Attribute()
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList(lo.toString(), hi.toString()))));

    SearchRequest searchRequests =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(demoGenderSearchParam), new ArrayList<>());

    SearchGroupItem anotherSearchGroupItem =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .searchParameters(Arrays.asList(icd9))
            .modifiers(new ArrayList<>());

    SearchGroupItem anotherNewSearchGroupItem =
        new SearchGroupItem()
            .type(DomainType.PERSON.toString())
            .searchParameters(Arrays.asList(demoAgeSearchParam))
            .modifiers(new ArrayList<>());

    searchRequests.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);
    searchRequests.getIncludes().get(0).addItemsItem(anotherNewSearchGroupItem);

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsDemoExcluded() throws Exception {
    SearchParameter demoGenderSearchParam =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(8507L);

    SearchParameter demoGenderSearchParamExclude =
        new SearchParameter()
            .domain(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(8507L);

    SearchGroupItem excludeSearchGroupItem =
        new SearchGroupItem()
            .type(DomainType.PERSON.toString())
            .searchParameters(Arrays.asList(demoGenderSearchParamExclude));
    SearchGroup excludeSearchGroup = new SearchGroup().addItemsItem(excludeSearchGroupItem);

    SearchRequest searchRequests =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(demoGenderSearchParam), new ArrayList<>());
    searchRequests.getExcludes().add(excludeSearchGroup);

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 0);
  }

  @Test
  public void countSubjectsICD9ParentAndICD10ChildCondition() throws Exception {
    SearchParameter icd9Parent =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(true)
            .standard(false)
            .ancestorData(false)
            .conceptId(1L)
            .value("001");

    CBCriteria criteriaParent =
        new CBCriteria()
            .parentId(99999)
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .code("001")
            .name("Cholera")
            .count("19")
            .group(true)
            .selectable(true)
            .ancestorData(false)
            .conceptId("2");
    saveCriteriaWithPath("0", criteriaParent);
    CBCriteria criteriaChild =
        new CBCriteria()
            .parentId(criteriaParent.getId())
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false)
            .code("001.1")
            .name("Cholera")
            .count("19")
            .group(false)
            .selectable(true)
            .ancestorData(false)
            .conceptId("1");
    saveCriteriaWithPath(criteriaParent.getPath(), criteriaChild);

    SearchParameter icd10 =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .group(false)
            .standard(false)
            .ancestorData(false)
            .conceptId(6L);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9Parent, icd10), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
    cbCriteriaDao.delete(criteriaParent.getId());
    cbCriteriaDao.delete(criteriaChild.getId());
  }

  @Test
  public void countSubjectsCPTProcedure() throws Exception {
    SearchParameter cpt =
        new SearchParameter()
            .domain(DomainType.PROCEDURE.toString())
            .type(CriteriaType.CPT4.toString())
            .group(false)
            .standard(false)
            .ancestorData(false)
            .conceptId(4L);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PROCEDURE.toString(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsSnomedChildCondition() throws Exception {
    SearchParameter snomed =
        new SearchParameter()
            .domain(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .conceptId(6L);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(snomed), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsSnomedParentProcedure() throws Exception {
    SearchParameter snomed =
        new SearchParameter()
            .domain(DomainType.PROCEDURE.toString())
            .type(CriteriaType.SNOMED.toString())
            .group(true)
            .standard(true)
            .ancestorData(false)
            .conceptId(4302541L);

    CBCriteria criteriaParent =
        new CBCriteria()
            .parentId(99999)
            .domainId(DomainType.PROCEDURE.toString())
            .type(CriteriaType.SNOMED.toString())
            .standard(true)
            .code("386637004")
            .name("Obstetric procedure")
            .count("36673")
            .group(true)
            .selectable(true)
            .ancestorData(false)
            .conceptId("4302541")
            .synonyms("+[PROCEDURE_rank1]");
    saveCriteriaWithPath("0", criteriaParent);
    CBCriteria criteriaChild =
        new CBCriteria()
            .parentId(criteriaParent.getId())
            .domainId(DomainType.PROCEDURE.toString())
            .type(CriteriaType.SNOMED.toString())
            .standard(true)
            .code("386639001")
            .name("Termination of pregnancy")
            .count("50")
            .group(false)
            .selectable(true)
            .ancestorData(false)
            .conceptId("4")
            .synonyms("+[PROCEDURE_rank1]");
    saveCriteriaWithPath(criteriaParent.getPath(), criteriaChild);

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(snomed), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(criteriaParent.getId());
    cbCriteriaDao.delete(criteriaChild.getId());
  }

  @Test
  public void countSubjectsVisit() throws Exception {
    SearchParameter visit =
        new SearchParameter()
            .domain(DomainType.VISIT.toString())
            .type(CriteriaType.VISIT.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .conceptId(10L);
    SearchRequest searchRequest =
        createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsVisitModifiers() throws Exception {
    SearchParameter visit =
        new SearchParameter()
            .domain(DomainType.VISIT.toString())
            .type(CriteriaType.VISIT.toString())
            .group(false)
            .standard(true)
            .ancestorData(false)
            .conceptId(10L);
    Modifier modifier =
        new Modifier()
            .name(ModifierType.NUM_OF_OCCURRENCES)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("1"));
    SearchRequest searchRequest =
        createSearchRequests(visit.getType(), Arrays.asList(visit), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsDrugChild() throws Exception {
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id bigint, descendant_id bigint)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (11, 11)");
    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId(11L);
    SearchRequest searchRequest =
        createSearchRequests(drug.getType(), Arrays.asList(drug), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void countSubjectsDrugParent() throws Exception {
    CBCriteria drugNode1 =
        new CBCriteria()
            .parentId(99999)
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .conceptId("21600932")
            .group(true)
            .standard(true)
            .selectable(true);
    saveCriteriaWithPath("0", drugNode1);
    CBCriteria drugNode2 =
        new CBCriteria()
            .parentId(drugNode1.getId())
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .conceptId("1520218")
            .group(false)
            .standard(true)
            .selectable(true);
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);

    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (1520218, 1520218)");
    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(true)
            .conceptId(21600932L)
            .ancestorData(true)
            .standard(true);
    SearchRequest searchRequest =
        createSearchRequests(drug.getDomain(), Arrays.asList(drug), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
    cbCriteriaDao.delete(drugNode1.getId());
    cbCriteriaDao.delete(drugNode2.getId());
  }

  @Test
  public void countSubjectsDrugParentAndChild() throws Exception {
    CBCriteria drugNode1 =
        new CBCriteria()
            .parentId(99999)
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .conceptId("21600932")
            .group(true)
            .standard(true)
            .selectable(true);
    saveCriteriaWithPath("0", drugNode1);
    CBCriteria drugNode2 =
        new CBCriteria()
            .parentId(drugNode1.getId())
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .conceptId("1520218")
            .group(false)
            .standard(true)
            .selectable(true);
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);

    // Use jdbcTemplate to create/insert data into the ancestor table
    // The codebase currently doesn't have a need to implement a DAO for this table
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (1520218, 1520218)");

    SearchParameter drugChild =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId(11L);
    SearchParameter drugParent =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(true)
            .conceptId(21600932L)
            .ancestorData(true)
            .standard(true);
    SearchRequest searchRequest =
        createSearchRequests(
            drugParent.getType(), Arrays.asList(drugParent, drugChild), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
    cbCriteriaDao.delete(drugNode1.getId());
    cbCriteriaDao.delete(drugNode2.getId());
  }

  @Test
  public void countSubjectsDrugChildEncounter() throws Exception {
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id bigint, descendant_id bigint)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (11, 11)");
    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId(11L);
    Modifier modifier =
        new Modifier()
            .name(ModifierType.ENCOUNTERS)
            .operator(Operator.IN)
            .operands(Arrays.asList("1"));
    SearchRequest searchRequest =
        createSearchRequests(drug.getType(), Arrays.asList(drug), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void countSubjectsDrugChildAgeAtEvent() throws Exception {
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id bigint, descendant_id bigint)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (11, 11)");
    SearchParameter drug =
        new SearchParameter()
            .domain(DomainType.DRUG.toString())
            .type(CriteriaType.ATC.toString())
            .group(false)
            .ancestorData(true)
            .standard(true)
            .conceptId(11L);
    Modifier modifier =
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("25"));
    SearchRequest searchRequest =
        createSearchRequests(drug.getType(), Arrays.asList(drug), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void countSubjectsLabEncounter() throws Exception {
    SearchParameter lab =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    Modifier modifier =
        new Modifier()
            .name(ModifierType.ENCOUNTERS)
            .operator(Operator.IN)
            .operands(Arrays.asList("1"));
    SearchRequest searchRequest =
        createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalBetween() throws Exception {
    SearchParameter lab =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    lab.attributes(
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList("0", "1"))));
    SearchRequest searchRequest =
        createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalIn() throws Exception {
    SearchParameter lab =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    lab.attributes(
        Arrays.asList(
            new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("1"))));
    SearchRequest searchRequest =
        createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothNumericalAndCategorical() throws Exception {
    SearchParameter lab =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    Attribute numerical =
        new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("0.1"));
    Attribute categorical =
        new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("1", "2"));
    lab.attributes(Arrays.asList(numerical, categorical));
    SearchRequest searchRequest =
        createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalAgeAtEvent() throws Exception {
    SearchParameter lab =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    Modifier modifier =
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("25"));
    SearchRequest searchRequest =
        createSearchRequests(lab.getDomain(), Arrays.asList(lab), Arrays.asList(modifier));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameter() throws Exception {
    SearchParameter lab1 =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(3L);
    SearchParameter lab2 =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(9L);
    SearchParameter lab3 =
        new SearchParameter()
            .domain(DomainType.MEASUREMENT.toString())
            .type(CriteriaType.LOINC.toString())
            .subtype(CriteriaSubType.LAB.toString())
            .group(false)
            .ancestorData(false)
            .standard(true)
            .conceptId(9L);
    Attribute labCategorical =
        new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("77"));
    lab3.attributes(Arrays.asList(labCategorical));
    SearchRequest searchRequest =
        createSearchRequests(lab1.getDomain(), Arrays.asList(lab1, lab2, lab3), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsBloodPressure() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList("90"))
                .conceptId(903118L),
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList("60", "80"))
                .conceptId(903115L));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BP.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(pm.getType(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureAny() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903118L),
            new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903115L));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BP.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetail() throws Exception {
    List<Attribute> bpAttributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList("90"))
                .conceptId(903118L),
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList("60", "80"))
                .conceptId(903115L));
    SearchParameter bpPm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BP.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .attributes(bpAttributes);

    List<Attribute> hrAttributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(Arrays.asList("71")));
    SearchParameter hrPm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.HR_DETAIL.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .conceptId(903126L)
            .attributes(hrAttributes);

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(),
            Arrays.asList(bpPm, hrPm),
            new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetailOrHeartRateIrr() throws Exception {
    List<Attribute> bpAttributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList("90"))
                .conceptId(903118L),
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList("60", "80"))
                .conceptId(903115L));
    SearchParameter bpPm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BP.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .attributes(bpAttributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(bpPm), new ArrayList<>());

    List<Attribute> hrAttributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(Arrays.asList("71")));
    SearchParameter hrPm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.HR_DETAIL.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .conceptId(903126L)
            .attributes(hrAttributes);
    SearchGroupItem anotherSearchGroupItem =
        new SearchGroupItem()
            .type(DomainType.PHYSICAL_MEASUREMENT.toString())
            .searchParameters(Arrays.asList(hrPm))
            .modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    List<Attribute> irrAttributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(Arrays.asList("4262985")));
    SearchParameter hrIrrPm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.HR.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .conceptId(1586218L)
            .attributes(irrAttributes);
    SearchGroupItem heartRateIrrSearchGroupItem =
        new SearchGroupItem()
            .type(DomainType.PHYSICAL_MEASUREMENT.toString())
            .searchParameters(Arrays.asList(hrIrrPm))
            .modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(heartRateIrrSearchGroupItem);

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsHeartRateAny() throws Exception {
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.HR_DETAIL.toString())
            .group(false)
            .conceptId(1586218L)
            .ancestorData(false)
            .standard(false);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsHeartRate() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList("45")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.HR_DETAIL.toString())
            .group(false)
            .conceptId(1586218L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsHeight() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList("168")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.HEIGHT.toString())
            .group(false)
            .conceptId(903133L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsWeight() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList("201")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.WEIGHT.toString())
            .group(false)
            .conceptId(903121L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBMI() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList("263")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BMI.toString())
            .group(false)
            .conceptId(903124L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWaistCircumferenceAndHipCircumference() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(Arrays.asList("31")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.WC.toString())
            .group(false)
            .conceptId(903135L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchParameter pm1 =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.HC.toString())
            .group(false)
            .conceptId(903136L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm, pm1), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectPregnant() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(Arrays.asList("45877994")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.PREG.toString())
            .group(false)
            .conceptId(903120L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWheelChairUser() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(Arrays.asList("4023190")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.WHEEL.toString())
            .group(false)
            .conceptId(903111L)
            .ancestorData(false)
            .standard(false)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsPPI() throws Exception {
    CBCriteria surveyNode =
        new CBCriteria()
            .parentId(0)
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .group(true)
            .selectable(true)
            .standard(false);
    saveCriteriaWithPath("0", surveyNode);
    CBCriteria questionNode =
        new CBCriteria()
            .parentId(surveyNode.getId())
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .group(true)
            .selectable(true)
            .standard(false)
            .name("In what country were you born?")
            .conceptId("1585899")
            .synonyms("[SURVEY_rank1]");
    saveCriteriaWithPath(surveyNode.getPath(), questionNode);
    CBCriteria answerNode =
        new CBCriteria()
            .parentId(questionNode.getId())
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .group(false)
            .selectable(true)
            .standard(false)
            .name("USA")
            .conceptId("5");
    saveCriteriaWithPath(questionNode.getPath(), answerNode);

    // Survey
    SearchParameter ppiSurvey =
        new SearchParameter()
            .domain(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .ancestorData(false)
            .standard(false)
            .group(true);
    SearchRequest searchRequest =
        createSearchRequests(ppiSurvey.getType(), Arrays.asList(ppiSurvey), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    // Question
    SearchParameter ppiQuestion =
        new SearchParameter()
            .domain(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .ancestorData(false)
            .standard(false)
            .group(true)
            .conceptId(1585899L);
    searchRequest =
        createSearchRequests(ppiQuestion.getType(), Arrays.asList(ppiQuestion), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    // value source concept id
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("7")));
    SearchParameter ppiValueAsConceptId =
        new SearchParameter()
            .domain(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .conceptId(5L)
            .attributes(attributes);
    searchRequest =
        createSearchRequests(
            ppiValueAsConceptId.getType(), Arrays.asList(ppiValueAsConceptId), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    // value as number
    attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(Arrays.asList("7")));
    SearchParameter ppiValueAsNumer =
        new SearchParameter()
            .domain(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.BASICS.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .conceptId(5L)
            .attributes(attributes);
    searchRequest =
        createSearchRequests(
            ppiValueAsNumer.getType(), Arrays.asList(ppiValueAsNumer), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    cbCriteriaDao.delete(surveyNode.getId());
    cbCriteriaDao.delete(questionNode.getId());
    cbCriteriaDao.delete(answerNode.getId());
  }

  @Test
  public void getDemoChartInfo() throws Exception {
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(Arrays.asList("4023190")));
    SearchParameter pm =
        new SearchParameter()
            .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.WHEEL.toString())
            .ancestorData(false)
            .standard(false)
            .group(false)
            .conceptId(903111L)
            .attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());

    DemoChartInfoListResponse response =
        controller.getDemoChartInfo(cdrVersion.getCdrVersionId(), searchRequest).getBody();
    assertEquals(2, response.getItems().size());
    assertEquals(
        new DemoChartInfo().gender("MALE").race("Asian").ageRange("45-64").count(1L),
        response.getItems().get(0));
    assertEquals(
        new DemoChartInfo().gender("MALE").race("Caucasian").ageRange("19-44").count(1L),
        response.getItems().get(1));
  }

  @Test
  public void filterBigQueryConfig_WithoutTableName() throws Exception {
    final String statement = "my statement ${projectId}.${dataSetId}.myTableName";
    QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(statement).setUseLegacySql(false).build();
    final String expectedResult = "my statement " + getTablePrefix() + ".myTableName";
    assertThat(expectedResult)
        .isEqualTo(bigQueryService.filterBigQueryConfig(queryJobConfiguration).getQuery());
  }

  protected String getTablePrefix() {
    CdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
    return cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset();
  }

  private void assertMessageException(
      SearchRequest searchRequest, String message, Object... messageParams) {
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      String expected = new MessageFormat(message).format(messageParams);
      assertEquals(expected, bre.getMessage());
    }
  }

  private SearchRequest createSearchRequests(
      String type, List<SearchParameter> parameters, List<Modifier> modifiers) {
    final SearchGroupItem searchGroupItem =
        new SearchGroupItem().type(type).searchParameters(parameters).modifiers(modifiers);

    final SearchGroup searchGroup = new SearchGroup().addItemsItem(searchGroupItem);

    List<SearchGroup> groups = new ArrayList<>();
    groups.add(searchGroup);
    return new SearchRequest().includes(groups);
  }

  private void assertParticipants(ResponseEntity response, Integer expectedCount) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Long participantCount = (Long) response.getBody();
    assertThat(participantCount).isEqualTo(expectedCount);
  }

  private void saveCriteriaWithPath(String path, CBCriteria criteria) {
    cbCriteriaDao.save(criteria);
    String pathEnd = String.valueOf(criteria.getId());
    criteria.path(path.isEmpty() ? pathEnd : path + "." + pathEnd);
    cbCriteriaDao.save(criteria);
  }
}
