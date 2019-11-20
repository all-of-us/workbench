package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CriteriaMenuOption;
import org.pmiops.workbench.model.CriteriaMenuSubOption;
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
import org.pmiops.workbench.model.StandardFlag;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(BeforeAfterSpringTestRunner.class)
// Note: normally we shouldn't need to explicitly import our own @TestConfiguration. This might be
// a bad interaction with BeforeAfterSpringTestRunner.
@Import({TestJpaConfig.class, CohortBuilderControllerBQTest.Configuration.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerBQTest extends BigQueryBaseTest {

  @TestConfiguration
  @Import({
    BigQueryTestService.class,
    CloudStorageServiceImpl.class,
    CohortQueryBuilder.class,
    SearchGroupItemQueryBuilder.class,
    CdrVersionService.class
  })
  @MockBean({FireCloudService.class})
  static class Configuration {
    @Bean
    public DbUser user() {
      DbUser user = new DbUser();
      user.setEmail("bob@gmail.com");
      return user;
    }
  }

  private CohortBuilderController controller;

  private CdrVersion cdrVersion;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private CloudStorageService cloudStorageService;

  @Autowired private CohortQueryBuilder cohortQueryBuilder;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private CdrVersionService cdrVersionService;

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;

  @Autowired private FireCloudService firecloudService;

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @Mock private Provider<WorkbenchConfig> configProvider;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Override
  public List<String> getTableNames() {
    return Arrays.asList("person", "death", "cb_search_person", "cb_search_all_events");
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
    when(configProvider.get()).thenReturn(testConfig);

    when(firecloudService.isUserMemberOfGroup(anyString(), anyString())).thenReturn(true);

    ElasticSearchService elasticSearchService =
        new ElasticSearchService(cbCriteriaDao, cloudStorageService, configProvider);

    controller =
        new CohortBuilderController(
            bigQueryService,
            cohortQueryBuilder,
            cbCriteriaDao,
            cbCriteriaAttributeDao,
            cdrVersionDao,
            cdrVersionService,
            elasticSearchService,
            configProvider);

    cdrVersion = new CdrVersion();
    cdrVersion.setCdrVersionId(1L);
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    cdrVersion = cdrVersionDao.save(cdrVersion);
  }

  private static SearchParameter icd9() {
    return new SearchParameter()
        .domain(DomainType.CONDITION.toString())
        .type(CriteriaType.ICD9CM.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(1L);
  }

  private static SearchParameter icd10() {
    return new SearchParameter()
        .domain(DomainType.CONDITION.toString())
        .type(CriteriaType.ICD10CM.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(9L);
  }

  private static SearchParameter snomed() {
    return new SearchParameter()
        .domain(DomainType.CONDITION.toString())
        .type(CriteriaType.SNOMED.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(4L);
  }

  private static SearchParameter drug() {
    return new SearchParameter()
        .domain(DomainType.DRUG.toString())
        .type(CriteriaType.ATC.toString())
        .group(false)
        .ancestorData(true)
        .standard(true)
        .conceptId(11L);
  }

  private static SearchParameter measurement() {
    return new SearchParameter()
        .domain(DomainType.MEASUREMENT.toString())
        .type(CriteriaType.LOINC.toString())
        .subtype(CriteriaSubType.LAB.toString())
        .group(false)
        .ancestorData(false)
        .standard(true)
        .conceptId(3L);
  }

  private static SearchParameter visit() {
    return new SearchParameter()
        .domain(DomainType.VISIT.toString())
        .type(CriteriaType.VISIT.toString())
        .group(false)
        .ancestorData(false)
        .standard(true)
        .conceptId(1L);
  }

  private static SearchParameter procedure() {
    return new SearchParameter()
        .domain(DomainType.PROCEDURE.toString())
        .type(CriteriaType.CPT4.toString())
        .group(false)
        .ancestorData(false)
        .standard(false)
        .conceptId(10L);
  }

  private static SearchParameter bloodPressure() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.BP.toString())
        .ancestorData(false)
        .standard(false)
        .group(false);
  }

  private static SearchParameter hrDetail() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HR_DETAIL.toString())
        .ancestorData(false)
        .standard(false)
        .group(false)
        .conceptId(903126L);
  }

  private static SearchParameter hr() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HR.toString())
        .ancestorData(false)
        .standard(false)
        .group(false)
        .conceptId(1586218L);
  }

  private static SearchParameter height() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HEIGHT.toString())
        .group(false)
        .conceptId(903133L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter weight() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WEIGHT.toString())
        .group(false)
        .conceptId(903121L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter bmi() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.BMI.toString())
        .group(false)
        .conceptId(903124L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter waistCircumference() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WC.toString())
        .group(false)
        .conceptId(903135L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter hipCircumference() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HC.toString())
        .group(false)
        .conceptId(903136L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter pregnancy() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.PREG.toString())
        .group(false)
        .conceptId(903120L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter wheelchair() {
    return new SearchParameter()
        .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WHEEL.toString())
        .group(false)
        .conceptId(903111L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter age() {
    return new SearchParameter()
        .domain(DomainType.PERSON.toString())
        .type(CriteriaType.AGE.toString())
        .group(false)
        .ancestorData(false)
        .standard(true);
  }

  private static SearchParameter male() {
    return new SearchParameter()
        .domain(DomainType.PERSON.toString())
        .type(CriteriaType.GENDER.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(8507L);
  }

  private static SearchParameter race() {
    return new SearchParameter()
        .domain(DomainType.PERSON.toString())
        .type(CriteriaType.RACE.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(1L);
  }

  private static SearchParameter ethnicity() {
    return new SearchParameter()
        .domain(DomainType.PERSON.toString())
        .type(CriteriaType.ETHNICITY.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(9898L);
  }

  private static SearchParameter deceased() {
    return new SearchParameter()
        .domain(DomainType.PERSON.toString())
        .type(CriteriaType.DECEASED.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .value("Deceased");
  }

  private static SearchParameter survey() {
    return new SearchParameter()
        .domain(DomainType.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.SURVEY.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(22L);
  }

  private static Modifier ageModifier() {
    return new Modifier()
        .name(ModifierType.AGE_AT_EVENT)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(Arrays.asList("25"));
  }

  private static Modifier visitModifier() {
    return new Modifier()
        .name(ModifierType.ENCOUNTERS)
        .operator(Operator.IN)
        .operands(Arrays.asList("1"));
  }

  private static Modifier occurrencesModifier() {
    return new Modifier()
        .name(ModifierType.NUM_OF_OCCURRENCES)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(Arrays.asList("1"));
  }

  private static Modifier eventDateModifier() {
    return new Modifier()
        .name(ModifierType.EVENT_DATE)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(Arrays.asList("2009-12-03"));
  }

  private static List<Attribute> wheelchairAttributes() {
    return Arrays.asList(
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(Arrays.asList("4023190")));
  }

  private static List<Attribute> bpAttributes() {
    return Arrays.asList(
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
  }

  private static CBCriteria drugCriteriaParent() {
    return new CBCriteria()
        .parentId(99999)
        .domainId(DomainType.DRUG.toString())
        .type(CriteriaType.ATC.toString())
        .conceptId("21600932")
        .group(true)
        .standard(true)
        .selectable(true);
  }

  private static CBCriteria drugCriteriaChild(long parentId) {
    return new CBCriteria()
        .parentId(parentId)
        .domainId(DomainType.DRUG.toString())
        .type(CriteriaType.RXNORM.toString())
        .conceptId("1520218")
        .group(false)
        .selectable(true);
  }

  private static CBCriteria icd9CriteriaParent() {
    return new CBCriteria()
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
        .conceptId("2")
        .synonyms("[CONDITION_rank1]");
  }

  private static CBCriteria icd9CriteriaChild(long parentId) {
    return new CBCriteria()
        .parentId(parentId)
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
  }

  @Test
  public void findCriteriaMenuOptions() throws Exception {
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .standard(false));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .standard(false));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .standard(true));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.SNOMED.toString())
            .standard(false));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.PROCEDURE.toString())
            .type(CriteriaType.CPT4.toString())
            .standard(false));
    List<CriteriaMenuOption> options =
        controller.findCriteriaMenuOptions(cdrVersion.getCdrVersionId()).getBody().getItems();
    assertThat(options).hasSize(2);
    CriteriaMenuOption option1 =
        new CriteriaMenuOption()
            .domain(DomainType.CONDITION.toString())
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.ICD10CM.toString())
                    .standardFlags(Arrays.asList(new StandardFlag().standard(false))))
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.ICD9CM.toString())
                    .standardFlags(Arrays.asList(new StandardFlag().standard(false))))
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.SNOMED.toString())
                    .standardFlags(
                        Arrays.asList(
                            new StandardFlag().standard(false),
                            new StandardFlag().standard(true))));
    CriteriaMenuOption option2 =
        new CriteriaMenuOption()
            .domain(DomainType.PROCEDURE.toString())
            .addTypesItem(
                new CriteriaMenuSubOption()
                    .type(CriteriaType.CPT4.toString())
                    .standardFlags(Arrays.asList(new StandardFlag().standard(false))));
    assertThat(options).contains(option1);
    assertThat(options).contains(option2);
  }

  @Test
  public void validateAttribute() throws Exception {
    SearchParameter demo = age();
    Attribute attribute = new Attribute().name(AttrName.NUM);
    demo.attributes(Collections.singletonList(attribute));
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Collections.singletonList(demo), new ArrayList<>());
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(bre, "Bad Request: attribute operator null is not valid.");
    }

    attribute.operator(Operator.BETWEEN);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(bre, "Bad Request: attribute operands are empty.");
    }

    attribute.operands(Collections.singletonList("20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(
          bre,
          "Bad Request: attribute NUM can only have 2 operands when using the BETWEEN operator");
    }

    attribute.operands(Arrays.asList("s", "20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(bre, "Bad Request: attribute NUM operands must be numeric.");
    }

    attribute.operands(Arrays.asList("10", "20"));
    attribute.operator(Operator.EQUAL);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(
          bre, "Bad Request: attribute NUM must have one operand when using the EQUAL operator.");
    }
  }

  private static void assertExceptionMessage(Exception exception, String expectedErrorMessage) {
    assertThat(exception.getMessage()).isEqualTo(expectedErrorMessage);
  }

  @Test
  public void validateModifiers() throws Exception {
    Modifier modifier = ageModifier().operator(null).operands(new ArrayList<>());
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9()), Arrays.asList(modifier));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(bre, "Bad Request: modifier operator null is not valid.");
    }

    modifier.operator(Operator.BETWEEN);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(bre, "Bad Request: modifier operands are empty.");
    }

    modifier.operands(Arrays.asList("20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(
          bre,
          "Bad Request: modifier AGE_AT_EVENT can only have 2 operands when using the BETWEEN operator");
    }

    modifier.operands(Arrays.asList("s", "20"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(bre, "Bad Request: modifier AGE_AT_EVENT operands must be numeric.");
    }

    modifier.operands(Arrays.asList("10", "20"));
    modifier.operator(Operator.EQUAL);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(
          bre,
          "Bad Request: modifier AGE_AT_EVENT must have one operand when using the EQUAL operator.");
    }

    modifier.name(ModifierType.EVENT_DATE);
    modifier.operands(Arrays.asList("10"));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(bre, "Bad Request: modifier EVENT_DATE must be a valid date.");
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChild() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void temporalGroupExceptions() throws Exception {
    SearchGroupItem icd9SGI =
        new SearchGroupItem().type(DomainType.CONDITION.toString()).addSearchParametersItem(icd9());

    SearchGroup temporalGroup = new SearchGroup().items(Arrays.asList(icd9SGI)).temporal(true);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(
          bre, "Bad Request: search group item temporal group null is not valid.");
    }

    icd9SGI.temporalGroup(0);
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertExceptionMessage(
          bre,
          "Bad Request: Search Group Items must provided for 2 different temporal groups(0 or 1).");
    }
  }

  @Test
  public void firstMentionOfICD9WithModifiersOrSnomed5DaysAfterICD10WithModifiers()
      throws Exception {
    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd9())
            .temporalGroup(0)
            .addModifiersItem(ageModifier());
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1)
            .addModifiersItem(visitModifier());
    SearchGroupItem snomedSGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(snomed())
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
    CBCriteria drugNode1 = drugCriteriaParent();
    saveCriteriaWithPath("0", drugNode1);
    CBCriteria drugNode2 = drugCriteriaChild(drugNode1.getId());
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);
    insertCriteriaAncestor(1520218, 1520218);
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug().conceptId(21600932L))
            .temporalGroup(0);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1)
            .addModifiersItem(visitModifier());

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
  public void anyMentionOfCPTParent5DaysAfterICD10Child() throws Exception {
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

    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(
                icd9().type(CriteriaType.CPT4.toString()).group(true).conceptId(0L))
            .temporalGroup(0)
            .addModifiersItem(visitModifier());
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .addSearchParametersItem(icd10())
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
    SearchGroupItem cptSGI =
        new SearchGroupItem()
            .type(DomainType.PROCEDURE.toString())
            .addSearchParametersItem(procedure())
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(DomainType.VISIT.toString())
            .addSearchParametersItem(visit())
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
    CBCriteria drugCriteria = drugCriteriaChild(1).conceptId("11");
    saveCriteriaWithPath("0", drugCriteria);
    insertCriteriaAncestor(11, 11);

    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
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
    CBCriteria drugCriteria = drugCriteriaChild(1).conceptId("11");
    saveCriteriaWithPath("0", drugCriteria);
    insertCriteriaAncestor(11, 11);

    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
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
    insertCriteriaAncestor(11, 11);

    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(1);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(DomainType.VISIT.toString())
            .addSearchParametersItem(visit())
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
    CBCriteria drugCriteria1 = drugCriteriaParent();
    saveCriteriaWithPath("0", drugCriteria1);
    CBCriteria drugCriteria2 = drugCriteriaChild(drugCriteria1.getId());
    saveCriteriaWithPath(drugCriteria1.getPath(), drugCriteria2);
    insertCriteriaAncestor(1520218, 1520218);

    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(DomainType.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(DomainType.VISIT.toString())
            .addSearchParametersItem(visit())
            .temporalGroup(0);
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(DomainType.DRUG.toString())
            .addSearchParametersItem(drug().group(true).conceptId(21600932L))
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
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9()), Arrays.asList(ageModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEncounter() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9()), Arrays.asList(visitModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEventBetween() throws Exception {
    Modifier modifier =
        ageModifier().operator(Operator.BETWEEN).operands(Arrays.asList("37", "39"));

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9()), Arrays.asList(modifier));

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9()),
            Arrays.asList(ageModifier(), occurrencesModifier().operands(Arrays.asList("2"))));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditioChildAgeAtEventAndOccurrencesAndEventDate()
      throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9(), icd9().conceptId(2L)),
            Arrays.asList(ageModifier(), occurrencesModifier(), eventDateModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEventDate() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9()),
            Arrays.asList(eventDateModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionNumOfOccurrences() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9()),
            Arrays.asList(occurrencesModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9Child() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(), Arrays.asList(icd9()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionParent() throws Exception {
    CBCriteria criteriaParent = icd9CriteriaParent();
    saveCriteriaWithPath("0", criteriaParent);
    CBCriteria criteriaChild = icd9CriteriaChild(criteriaParent.getId());
    saveCriteriaWithPath(criteriaParent.getPath(), criteriaChild);

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9().group(true).conceptId(2L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    cbCriteriaDao.delete(criteriaParent.getId());
    cbCriteriaDao.delete(criteriaChild.getId());
  }

  @Test
  public void countSubjectsDemoGender() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(male()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoRace() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(race()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoEthnicity() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(ethnicity()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoDec() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(deceased()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoAge() throws Exception {
    Integer lo = getTestPeriod().getYears() - 1;
    Integer hi = getTestPeriod().getYears() + 1;
    SearchParameter demo = age();
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
    SearchParameter demoAgeSearchParam = age();
    Integer lo = getTestPeriod().getYears() - 1;
    Integer hi = getTestPeriod().getYears() + 1;

    demoAgeSearchParam.attributes(
        Arrays.asList(
            new Attribute()
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList(lo.toString(), hi.toString()))));

    SearchRequest searchRequests =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(male()), new ArrayList<>());

    SearchGroupItem anotherSearchGroupItem =
        new SearchGroupItem()
            .type(DomainType.CONDITION.toString())
            .searchParameters(Arrays.asList(icd9().conceptId(3L)))
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
    SearchGroupItem excludeSearchGroupItem =
        new SearchGroupItem()
            .type(DomainType.PERSON.toString())
            .searchParameters(Arrays.asList(male()));
    SearchGroup excludeSearchGroup = new SearchGroup().addItemsItem(excludeSearchGroupItem);

    SearchRequest searchRequests =
        createSearchRequests(
            DomainType.PERSON.toString(), Arrays.asList(male()), new ArrayList<>());
    searchRequests.getExcludes().add(excludeSearchGroup);

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 0);
  }

  @Test
  public void countSubjectsICD9ParentAndICD10ChildCondition() throws Exception {
    CBCriteria criteriaParent = icd9CriteriaParent();
    saveCriteriaWithPath("0", criteriaParent);
    CBCriteria criteriaChild = icd9CriteriaChild(criteriaParent.getId());
    saveCriteriaWithPath(criteriaParent.getPath(), criteriaChild);

    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(icd9().group(true).conceptId(2L), icd10().conceptId(6L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
    cbCriteriaDao.delete(criteriaParent.getId());
    cbCriteriaDao.delete(criteriaChild.getId());
  }

  @Test
  public void countSubjectsCPTProcedure() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PROCEDURE.toString(),
            Arrays.asList(procedure().conceptId(4L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsSnomedChildCondition() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.CONDITION.toString(),
            Arrays.asList(snomed().standard(true).conceptId(6L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsSnomedParentProcedure() throws Exception {
    SearchParameter snomed = snomed().group(true).standard(true).conceptId(4302541L);

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
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.VISIT.toString(), Arrays.asList(visit().conceptId(10L)), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsVisitModifiers() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.VISIT.toString(),
            Arrays.asList(visit().conceptId(10L)),
            Arrays.asList(occurrencesModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsDrugChild() throws Exception {
    insertCriteriaAncestor(11, 11);
    SearchRequest searchRequest =
        createSearchRequests(DomainType.DRUG.toString(), Arrays.asList(drug()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void countSubjectsDrugParent() throws Exception {
    CBCriteria drugNode1 = drugCriteriaParent();
    saveCriteriaWithPath("0", drugNode1);
    CBCriteria drugNode2 = drugCriteriaChild(drugNode1.getId());
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);

    insertCriteriaAncestor(1520218, 1520218);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.DRUG.toString(),
            Arrays.asList(drug().group(true).conceptId(21600932L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
    cbCriteriaDao.delete(drugNode1.getId());
    cbCriteriaDao.delete(drugNode2.getId());
  }

  @Test
  public void countSubjectsDrugParentAndChild() throws Exception {
    CBCriteria drugNode1 = drugCriteriaParent();
    saveCriteriaWithPath("0", drugNode1);
    CBCriteria drugNode2 = drugCriteriaChild(drugNode1.getId());
    saveCriteriaWithPath(drugNode1.getPath(), drugNode2);

    insertCriteriaAncestor(1520218, 1520218);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.DRUG.toString(),
            Arrays.asList(drug().group(true).conceptId(21600932L), drug()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
    cbCriteriaDao.delete(drugNode1.getId());
    cbCriteriaDao.delete(drugNode2.getId());
  }

  @Test
  public void countSubjectsDrugChildEncounter() throws Exception {
    insertCriteriaAncestor(11, 11);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.DRUG.toString(), Arrays.asList(drug()), Arrays.asList(visitModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void countSubjectsDrugChildAgeAtEvent() throws Exception {
    insertCriteriaAncestor(11, 11);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.DRUG.toString(), Arrays.asList(drug()), Arrays.asList(ageModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    jdbcTemplate.execute("drop table cb_criteria_ancestor");
  }

  @Test
  public void countSubjectsLabEncounter() throws Exception {
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.MEASUREMENT.toString(),
            Arrays.asList(measurement()),
            Arrays.asList(visitModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalBetween() throws Exception {
    SearchParameter lab = measurement();
    lab.attributes(
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(Arrays.asList("0", "1"))));
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.MEASUREMENT.toString(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalIn() throws Exception {
    SearchParameter lab = measurement();
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
    SearchParameter lab = measurement();
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
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.MEASUREMENT.toString(),
            Arrays.asList(measurement()),
            Arrays.asList(ageModifier()));
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameter() throws Exception {
    SearchParameter lab1 = measurement();
    SearchParameter lab2 = measurement().conceptId(9L);
    SearchParameter lab3 = measurement().conceptId(9L);
    Attribute labCategorical =
        new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("77"));
    lab3.attributes(Arrays.asList(labCategorical));
    SearchRequest searchRequest =
        createSearchRequests(lab1.getDomain(), Arrays.asList(lab1, lab2, lab3), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameterSourceAndStandard() throws Exception {
    SearchParameter icd9 = icd9();
    SearchParameter snomed = snomed().standard(true);
    SearchRequest searchRequest =
        createSearchRequests(icd9.getDomain(), Arrays.asList(icd9, snomed), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressure() throws Exception {
    SearchParameter pm = bloodPressure().attributes(bpAttributes());
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
    SearchParameter pm = bloodPressure().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetail() throws Exception {
    SearchParameter bpPm = bloodPressure().attributes(bpAttributes());

    List<Attribute> hrAttributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(Arrays.asList("71")));
    SearchParameter hrPm = hrDetail().attributes(hrAttributes);

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
    SearchParameter bpPm = bloodPressure().attributes(bpAttributes());
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(bpPm), new ArrayList<>());

    List<Attribute> hrAttributes =
        Arrays.asList(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(Arrays.asList("71")));
    SearchParameter hrPm = hrDetail().attributes(hrAttributes);
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
    SearchParameter hrIrrPm = hr().attributes(irrAttributes);
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
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(),
            Arrays.asList(hrDetail().conceptId(1586218L)),
            new ArrayList<>());
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
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(),
            Arrays.asList(hrDetail().conceptId(1586218L).attributes(attributes)),
            new ArrayList<>());
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
    SearchParameter pm = height().attributes(attributes);
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
    SearchParameter pm = weight().attributes(attributes);
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
    SearchParameter pm = bmi().attributes(attributes);
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
    SearchParameter pm = waistCircumference().attributes(attributes);
    SearchParameter pm1 = hipCircumference().attributes(attributes);
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
    SearchParameter pm = pregnancy().attributes(attributes);
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWheelChairUser() throws Exception {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
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
            .subtype(CriteriaSubType.SURVEY.toString())
            .group(true)
            .selectable(true)
            .standard(false)
            .conceptId("22");
    saveCriteriaWithPath("0", surveyNode);
    CBCriteria questionNode =
        new CBCriteria()
            .parentId(surveyNode.getId())
            .domainId(DomainType.SURVEY.toString())
            .type(CriteriaType.PPI.toString())
            .subtype(CriteriaSubType.QUESTION.toString())
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
            .subtype(CriteriaSubType.ANSWER.toString())
            .group(false)
            .selectable(true)
            .standard(false)
            .name("USA")
            .conceptId("5");
    saveCriteriaWithPath(questionNode.getPath(), answerNode);

    // Survey
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.SURVEY.toString(), Arrays.asList(survey()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    // Question
    SearchParameter ppiQuestion =
        survey().subtype(CriteriaSubType.QUESTION.toString()).conceptId(1585899L);
    searchRequest =
        createSearchRequests(ppiQuestion.getType(), Arrays.asList(ppiQuestion), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    // value source concept id
    List<Attribute> attributes =
        Arrays.asList(
            new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("7")));
    SearchParameter ppiValueAsConceptId =
        survey().subtype(CriteriaSubType.ANSWER.toString()).conceptId(5L).attributes(attributes);
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
        survey().subtype(CriteriaSubType.ANSWER.toString()).conceptId(5L).attributes(attributes);
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
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    SearchRequest searchRequest =
        createSearchRequests(
            DomainType.PHYSICAL_MEASUREMENT.toString(),
            Collections.singletonList(pm),
            new ArrayList<>());

    DemoChartInfoListResponse response =
        controller.getDemoChartInfo(cdrVersion.getCdrVersionId(), searchRequest).getBody();
    assertThat(response.getItems()).hasSize(2);
    assertThat(response.getItems().get(0))
        .isEqualTo(new DemoChartInfo().gender("MALE").race("Asian").ageRange("45-64").count(1L));
    assertThat(response.getItems().get(1))
        .isEqualTo(
            new DemoChartInfo().gender("MALE").race("Caucasian").ageRange("18-44").count(1L));
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

  private void insertCriteriaAncestor(int ancestorId, int descendentId) {
    jdbcTemplate.execute(
        "create table cb_criteria_ancestor(ancestor_id bigint, descendant_id bigint)");
    jdbcTemplate.execute(
        "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values ("
            + ancestorId
            + ", "
            + descendentId
            + ")");
  }

  private Period getTestPeriod() {
    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    return new Period(birthDate, now);
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
    criteria = cbCriteriaDao.save(criteria);
    String pathEnd = String.valueOf(criteria.getId());
    criteria.path(path.isEmpty() ? pathEnd : path + "." + pathEnd);
    cbCriteriaDao.save(criteria);
  }
}
