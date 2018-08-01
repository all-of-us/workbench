package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({QueryBuilderFactory.class, BigQueryService.class, CohortBuilderController.class,
  ParticipantCounter.class, DomainLookupService.class, CohortQueryBuilder.class,
  TestJpaConfig.class, CdrVersionService.class})
@MockBean({FireCloudService.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerBQTest extends BigQueryBaseTest {

  private static final String TYPE_ICD9 = "ICD9";
  private static final String SUBTYPE_NONE = null;
  private static final String TYPE_ICD10 = "ICD10";
  private static final String TYPE_CPT = "CPT";
  private static final String TYPE_PM = "PM";
  private static final String TYPE_VISIT = "VISIT";
  private static final String TYPE_DRUG = "DRUG";
  private static final String SUBTYPE_CPT4 = "CPT4";
  private static final String SUBTYPE_ICD10CM = "ICD10CM";
  private static final String SUBTYPE_ICD10PCS = "ICD10PCS";
  private static final String SUBTYPE_BP = "BP";
  private static final String SUBTYPE_HR_DETAIL = "HR-DETAIL";
  private static final String SUBTYPE_HR = "HR";
  private static final String SUBTYPE_HEIGHT = "HEIGHT";
  private static final String SUBTYPE_WEIGHT = "WEIGHT";
  private static final String SUBTYPE_BMI = "BMI";
  private static final String SUBTYPE_WC = "WC";
  private static final String SUBTYPE_HC = "HC";
  private static final String SUBTYPE_PREG = "PREG";
  private static final String SUBTYPE_WHEEL = "WHEEL";
  private static final String DOMAIN_CONDITION = "Condition";
  private static final String DOMAIN_PROCEDURE = "Procedure";
  private static final String DOMAIN_MEASUREMENT = "Measurement";
  private static final String DOMAIN_OBSERVATION = "Observation";
  private static final String DOMAIN_DRUG = "Drug";

  @Autowired
  private CohortBuilderController controller;

  private CdrVersion cdrVersion;

  @Autowired
  private BigQueryService bigQueryService;

  @Autowired
  private CriteriaDao criteriaDao;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private TestWorkbenchConfig testWorkbenchConfig;

  private Criteria icd9ConditionChild;
  private Criteria icd9ConditionParent;
  private Criteria icd9ProcedureChild;
  private Criteria icd9ProcedureParent;
  private Criteria icd9MeasurementChild;
  private Criteria icd9MeasurementParent;
  private Criteria icd10ConditionChild;
  private Criteria icd10ConditionParent;
  private Criteria icd10ProcedureChild;
  private Criteria icd10ProcedureParent;
  private Criteria icd10MeasurementChild;
  private Criteria icd10MeasurementParent;
  private Criteria cptProcedure;
  private Criteria cptObservation;
  private Criteria cptMeasurement;
  private Criteria cptDrug;

  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
      "person",
      "concept",
      "condition_occurrence",
      "procedure_occurrence",
      "measurement",
      "observation",
      "drug_exposure",
      "phecode_criteria_icd",
      "concept_relationship",
      "death",
      "visit_occurrence",
      "concept_ancestor");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    cdrVersion = new CdrVersion();
    cdrVersion.setCdrVersionId(1L);
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    cdrVersionDao.save(cdrVersion);

    icd9ConditionParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE, "001"));
    icd9ConditionChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9ConditionParent.getId(), "001", DOMAIN_CONDITION));
    icd9ProcedureParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE, "002"));
    icd9ProcedureChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9ProcedureParent.getId(), "002", DOMAIN_PROCEDURE));
    icd9MeasurementParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE, "003"));
    icd9MeasurementChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9MeasurementParent.getId(), "003", DOMAIN_MEASUREMENT));
    icd10ConditionParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10CM, "A"));
    icd10ConditionChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10CM, icd10ConditionParent.getId(), "A09", DOMAIN_CONDITION));
    icd10ProcedureParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10PCS, "16"));
    icd10ProcedureChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10PCS, icd10ProcedureParent.getId(), "16070", DOMAIN_PROCEDURE));
    icd10MeasurementParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10CM, "R92"));
    icd10MeasurementChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10CM, icd10MeasurementParent.getId(), "R92.2", DOMAIN_MEASUREMENT));
    cptProcedure = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001T", DOMAIN_PROCEDURE));
    cptObservation = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001Z", DOMAIN_OBSERVATION));
    cptMeasurement = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001Q", DOMAIN_MEASUREMENT));
    cptDrug = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "90703", DOMAIN_DRUG));
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChild() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildrenAgeAtEvent() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    SearchParameter icd9Proc = createSearchParameter(icd9ProcedureChild, "001.1");
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9, icd9Proc), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventInvalidLong() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    String operand = "zz";
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList(operand));
    SearchRequest searchRequest =
      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));

    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo(
        "Please provide valid number for age at event.");
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventTooManyOperands() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    String operand1 = "1";
    String operand2 = "2";
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList(operand1, operand2));
    SearchRequest searchRequest =
      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));

    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo(String.format(
        "Modifier: age at event can only have 1 operand when using the %s operator",
        modifier.getOperator().name()));
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventBetween() throws Exception {
    DateTime birthDate = new DateTime(1980, 2, 17, 0, 0, 0, 0);
    DateTime eventDate = new DateTime(2008, 7, 22, 0, 0, 0, 0);

    Period period = new Period(birthDate, eventDate);
    period.getYears();

    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    String operand1 = Integer.toString(period.getYears() - 1);
    String operand2 = Integer.toString(period.getYears() + 1);
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.BETWEEN)
      .operands(Arrays.asList(operand1, operand2));
    SearchRequest searchRequest =
      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventBetweenTooManyOperands() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    String operand1 = "24";
    String operand2 = "26";
    String operand3 = "27";
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.BETWEEN)
      .operands(Arrays.asList(operand1, operand2, operand3));
    SearchRequest searchRequest =
      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));

    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo(String.format(
        "Modifier: age at event can only have 2 operands when using the %s operator",
        modifier.getOperator().name()));
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventBetweenNotEnoughOperands() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    String operand1 = "24";
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.BETWEEN)
      .operands(Arrays.asList(operand1));
    SearchRequest searchRequest =
      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));

    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo(String.format(
        "Modifier: age at event can only have 2 operands when using the %s operator",
        modifier.getOperator().name()));
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildNumOfOccurrencesMoreThanOne() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    Modifier modifier1 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.EQUAL)
      .operands(Arrays.asList("1"));
    Modifier modifier2 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.EQUAL)
      .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9), Arrays.asList(modifier1, modifier2));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo(
        "Please provide one number of occurrences modifier.");
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildNumOfOccurrencesInvalidLong() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    String operand = "c";
    Modifier modifier1 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.EQUAL)
      .operands(Arrays.asList(operand));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9), Arrays.asList(modifier1));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      assertThat(bre.getMessage()).isEqualTo(
        "Please provide valid number for number of occurrences.");
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    Modifier modifier1 = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));
    Modifier modifier2 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.EQUAL)
      .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9), Arrays.asList(modifier1, modifier2));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrencesAndEventDate() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    SearchParameter icd9Proc = createSearchParameter(icd9ProcedureChild, "001.1");
    Modifier modifier1 = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));
    Modifier modifier2 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("1"));
    Modifier modifier3 = new Modifier()
      .name(ModifierType.EVENT_DATE)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("2009-12-03"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9,icd9Proc), Arrays.asList(modifier1, modifier2, modifier3));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildEventDate() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    Modifier modifier = new Modifier()
      .name(ModifierType.EVENT_DATE)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("2009-12-03"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildEventDateInvalidDate() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    Modifier modifier = new Modifier()
      .name(ModifierType.EVENT_DATE)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("c"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9), Arrays.asList(modifier));
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertThat(bre.getMessage()).isEqualTo(
        "Please provide valid date for event date.");
    }
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildOccurrences() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    Modifier modifier2 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9), Arrays.asList(modifier2));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceParent() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ConditionParent, "001");
    SearchRequest searchRequest = createSearchRequests(icd9ConditionParent.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ProcedureOccurrenceChild() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ProcedureChild, "002.1");
    SearchRequest searchRequest = createSearchRequests(icd9ProcedureChild.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ProcedureOccurrenceParent() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9ProcedureParent, "002");
    SearchRequest searchRequest = createSearchRequests(icd9ProcedureParent.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9MeasurementChild() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9MeasurementChild, "003.1");
    SearchRequest searchRequest = createSearchRequests(icd9MeasurementChild.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9MeasurementParent() throws Exception {
    SearchParameter icd9 = createSearchParameter(icd9MeasurementParent, "003");
    SearchRequest searchRequest = createSearchRequests(icd9MeasurementParent.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoGender() throws Exception {
    Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
    SearchParameter demo = createSearchParameter(demoGender, null);
    SearchRequest searchRequest = createSearchRequests(demoGender.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoEthnicity() throws Exception {
    Criteria demoEthnicity = createDemoCriteria("DEMO", "ETH", "9898");
    SearchParameter demo = createSearchParameter(demoEthnicity, null);
    SearchRequest searchRequest = createSearchRequests(demoEthnicity.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoDec() throws Exception {
    Criteria demoGender = createDemoCriteria("DEMO", "DEC", null);
    SearchParameter demo = createSearchParameter(demoGender, "Deceased");
    SearchRequest searchRequest = createSearchRequests(demoGender.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoAge() throws Exception {
    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    Criteria demoAge = createDemoCriteria("DEMO", "AGE", null);
    SearchParameter demo = createSearchParameter(demoAge, null);
    demo.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));
    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 2);
  }

  @Test
  public void countSubjectsDemoAgeBetween() throws Exception {
    Criteria demoAge = createDemoCriteria("DEMO", "AGE", null);
    SearchParameter demo = createSearchParameter(demoAge, null);
    demo.attributes(Arrays.asList(
      new Attribute().operator(Operator.BETWEEN).operands(Arrays.asList("15","99"))
    ));
    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 2);
  }

  @Test
  public void countSubjectsDemoGenderAndAge() throws Exception {
    Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
    SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    Criteria demoAge = createDemoCriteria("DEMO", "AGE", null);
    SearchParameter demoAgeSearchParam = createSearchParameter(demoAge, null);
    demoAgeSearchParam.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));

    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demoGenderSearchParam, demoAgeSearchParam), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsICD9AndDemo() throws Exception {
    Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
    SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    Criteria demoAge = createDemoCriteria("DEMO", "AGE", null);
    SearchParameter demoAgeSearchParam = createSearchParameter(demoAge, null);
    demoAgeSearchParam.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));

    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demoGenderSearchParam, demoAgeSearchParam), new ArrayList<>());

    SearchParameter icd9 = createSearchParameter(icd9MeasurementChild, "003.1");
    SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type(icd9.getType()).searchParameters(Arrays.asList(icd9)).modifiers(new ArrayList<>());

    searchRequests.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsDemoExcluded() throws Exception {
    Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
    SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

    SearchParameter demoGenderSearchParamExclude = createSearchParameter(demoGender, null);

    SearchGroupItem excludeSearchGroupItem = new SearchGroupItem().type(demoGender.getType())
      .searchParameters(Arrays.asList(demoGenderSearchParamExclude));
    SearchGroup excludeSearchGroup = new SearchGroup().addItemsItem(excludeSearchGroupItem);

    SearchRequest searchRequests = createSearchRequests(demoGender.getType(), Arrays.asList(demoGenderSearchParam), new ArrayList<>());
    searchRequests.getExcludes().add(excludeSearchGroup);

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 0);
  }

  @Test
  public void countSubjectsICD10ConditionOccurrenceChild() throws Exception {
    SearchParameter icd10 = createSearchParameter(icd10ConditionChild, "A09");
    SearchRequest searchRequest = createSearchRequests(icd10ConditionChild.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ConditionOccurrenceParent() throws Exception {
    SearchParameter icd10 = createSearchParameter(icd10ConditionParent, "A");
    SearchRequest searchRequest = createSearchRequests(icd10ConditionParent.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ProcedureOccurrenceChild() throws Exception {
    SearchParameter icd10 = createSearchParameter(icd10ProcedureChild, "16070");
    SearchRequest searchRequest = createSearchRequests(icd10ProcedureChild.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ProcedureOccurrenceParent() throws Exception {
    SearchParameter icd10 = createSearchParameter(icd10ProcedureParent, "16");
    SearchRequest searchRequest = createSearchRequests(icd10ProcedureParent.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10MeasurementChild() throws Exception {
    SearchParameter icd10 = createSearchParameter(icd10MeasurementChild, "R92.2");
    SearchRequest searchRequest = createSearchRequests(icd10MeasurementChild.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10MeasurementParent() throws Exception {
    SearchParameter icd10 = createSearchParameter(icd10MeasurementParent, "R92");
    SearchRequest searchRequest = createSearchRequests(icd10MeasurementParent.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTProcedureOccurrence() throws Exception {
    SearchParameter cpt = createSearchParameter(cptProcedure, "0001T");
    SearchRequest searchRequest = createSearchRequests(cptProcedure.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTObservation() throws Exception {
    SearchParameter cpt = createSearchParameter(cptObservation, "0001Z");
    SearchRequest searchRequest = createSearchRequests(cptObservation.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTMeasurement() throws Exception {
    SearchParameter cpt = createSearchParameter(cptMeasurement, "0001Q");
    SearchRequest searchRequest = createSearchRequests(cptMeasurement.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTDrugExposure() throws Exception {
    SearchParameter cpt = createSearchParameter(cptDrug, "90703");
    SearchRequest searchRequest = createSearchRequests(cptDrug.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsVisitChild() throws Exception {
    Criteria visitCriteria = new Criteria().type(TYPE_VISIT).group(false).conceptId("10");
    SearchParameter visit = createSearchParameter(visitCriteria, null);
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsVisitParent() throws Exception {
    Criteria visitCriteria = new Criteria().type(TYPE_VISIT).group(true).conceptId("1");
    SearchParameter visit = createSearchParameter(visitCriteria, null);
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsVisitChildOrParent() throws Exception {
    Criteria visitChildCriteria = new Criteria().type(TYPE_VISIT).group(false).conceptId("1");
    Criteria visitParentCriteria = new Criteria().type(TYPE_VISIT).group(true).conceptId("1");
    SearchParameter visitChild = createSearchParameter(visitChildCriteria, null);
    SearchParameter visitParent = createSearchParameter(visitParentCriteria, null);
    SearchRequest searchRequest = createSearchRequests(visitChild.getType(), Arrays.asList(visitChild, visitParent), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsVisitChildNullConceptId() throws Exception {
    Criteria visitCriteria = new Criteria().type(TYPE_VISIT).group(false);
    SearchParameter visit = createSearchParameter(visitCriteria, null);
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException e) {
      // success
      assertEquals("Please provide a valid concept Id", e.getMessage());
    }
  }

  @Test
  public void countSubjectsVisitChildModifiers() throws Exception {
    Criteria visitCriteria = new Criteria().type(TYPE_VISIT).group(false).conceptId("10");
    SearchParameter visit = createSearchParameter(visitCriteria, null);
    Modifier modifier = new Modifier()
    .name(ModifierType.NUM_OF_OCCURRENCES)
    .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
    .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugChild() throws Exception {
    Criteria drugCriteria = new Criteria().type(TYPE_DRUG).group(false).conceptId("11");
    SearchParameter drug = createSearchParameter(drugCriteria, null);
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugChildModifiers() throws Exception {
    Criteria drugCriteria = new Criteria().type(TYPE_DRUG).group(false).conceptId("11");
    SearchParameter drug = createSearchParameter(drugCriteria, null);
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressure() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L),
      new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("60","80")).conceptId(903115L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_BP, "BP Name", attributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureAny() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Systolic").operator(Operator.ANY).operands(new ArrayList<>()).conceptId(903118L),
      new Attribute().name("Diastolic").operator(Operator.ANY).operands(new ArrayList<>()).conceptId(903115L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_BP, "BP Name", attributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetailOrHeartRateIrr() throws Exception {
    List<Attribute> bpAttributes = Arrays.asList(
      new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L),
      new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("60","80")).conceptId(903115L)
    );
    SearchParameter bpSearchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_BP, "BP Name", bpAttributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(bpSearchParameter), new ArrayList<>());

    List<Attribute> hrAttributes = Arrays.asList(
      new Attribute().name("Heart Rate").operator(Operator.EQUAL).operands(Arrays.asList("71")).conceptId(903126L)
    );
    SearchParameter hrSearchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_HR_DETAIL, "Heart Rate Detail", hrAttributes);
    SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type(TYPE_PM).searchParameters(Arrays.asList(hrSearchParameter)).modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    SearchParameter heartRateIrr = createPMSearchCriteria(TYPE_PM, SUBTYPE_HR, "Heart Rate Irr", "1586218", "irregularity-detected");
    SearchGroupItem heartRateIrrSearchGroupItem = new SearchGroupItem().type(TYPE_PM).searchParameters(Arrays.asList(heartRateIrr)).modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(heartRateIrrSearchGroupItem);

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsHeartRateNoIrr() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TYPE_PM, SUBTYPE_HR, "Heart Rate Irr", "1586218", "no-irregularity-detected");
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsHeight() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Height").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("168")).conceptId(903133L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_HEIGHT, "Height", attributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsWeight() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Weight").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("201")).conceptId(903121L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_WEIGHT, "Weight", attributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBMI() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("BMI").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("263")).conceptId(903124L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_BMI, "BMI", attributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWaistCircumference() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Waist").operator(Operator.EQUAL).operands(Arrays.asList("31")).conceptId(903135L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_WC, "Waist", attributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectHipCircumference() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Hip").operator(Operator.EQUAL).operands(Arrays.asList("33")).conceptId(903136L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TYPE_PM, SUBTYPE_HC, "Hip", attributes);
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectPregnant() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TYPE_PM, SUBTYPE_PREG, "Pregnancy", "903120", "pregnant");
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectNotPregnant() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TYPE_PM, SUBTYPE_PREG, "Pregnancy", "903120", "not-pregnant");
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWheelChairUser() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TYPE_PM, SUBTYPE_WHEEL, "Wheel Chair User", "903111", "wheelchair-user");
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectNotWheelChairUser() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TYPE_PM, SUBTYPE_WHEEL, "Wheel Chair User", "903111", "not-wheelchair-user");
    SearchRequest searchRequest = createSearchRequests(TYPE_PM, Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBadOperand() throws Exception {
    Attribute attribute = new Attribute().name("Heart Rate Detail").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903126L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HR_DETAIL, "Heart Rate");

    attribute = new Attribute().name("BMI").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903124L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_BMI, "BMI");

    attribute = new Attribute().name("Height").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903133L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HEIGHT, "Height");

    attribute = new Attribute().name("Weight").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903121L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WEIGHT, "Weight");

    attribute = new Attribute().name("Waist").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903135L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WC, "Waist Circumference");

    attribute = new Attribute().name("Hip").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903136L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HC, "Hip Circumference");
  }

  @Test
  public void countSubjectsNoOperator() throws Exception {
    Attribute attribute = new Attribute().name("Heart Rate Detail").operands(Arrays.asList("10")).conceptId(903126L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HR_DETAIL, "Heart Rate");

    attribute = new Attribute().name("BMI").operands(Arrays.asList("10")).conceptId(903124L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_BMI, "BMI");

    attribute = new Attribute().name("Height").operands(Arrays.asList("10")).conceptId(903133L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HEIGHT, "Height");

    attribute = new Attribute().name("Weight").operands(Arrays.asList("10")).conceptId(903121L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WEIGHT, "Weight");

    attribute = new Attribute().name("Waist").operands(Arrays.asList("10")).conceptId(903135L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WC, "Waist Circumference");

    attribute = new Attribute().name("Hip").operands(Arrays.asList("10")).conceptId(903136L);
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HC, "Hip Circumference");
  }

  @Test
  public void countSubjectsNoConceptId() throws Exception {
    Attribute attribute = new Attribute().name("Heart Rate Detail").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HR_DETAIL, "Heart Rate");

    attribute = new Attribute().name("BMI").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_BMI, "BMI");

    attribute = new Attribute().name("Height").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HEIGHT, "Height");

    attribute = new Attribute().name("Weight").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WEIGHT, "Weight");

    attribute = new Attribute().name("Waist").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WC, "Waist Circumference");

    attribute = new Attribute().name("Hip").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HC, "Hip Circumference");
  }

  @Test
  public void countSubjectsEmptyAttribute() throws Exception {
    Attribute attribute = new Attribute();
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HR_DETAIL, "Heart Rate");
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_BMI, "BMI");
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HEIGHT, "Height");
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WEIGHT, "Weight");
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_WC, "Waist Circumference");
    assertBadRequestExceptionAttributes(attribute, SUBTYPE_HC, "Hip Circumference");
  }

  @Test
  public void countSubjectsNoAttribute() throws Exception {
    assertBadRequestExceptionAttributes(null, SUBTYPE_HR_DETAIL, "Heart Rate");
    assertBadRequestExceptionAttributes(null, SUBTYPE_BMI, "BMI");
    assertBadRequestExceptionAttributes(null, SUBTYPE_HEIGHT, "Height");
    assertBadRequestExceptionAttributes(null, SUBTYPE_WEIGHT, "Weight");
    assertBadRequestExceptionAttributes(null, SUBTYPE_WC, "Waist Circumference");
    assertBadRequestExceptionAttributes(null, SUBTYPE_HC, "Hip Circumference");
  }

  @Test
  public void countSubjectsTestValidateNonAttribute() throws Exception {
    assertBadRequestExceptionNoAttributes(SUBTYPE_HR, "Heart Rate", null, null);
    assertBadRequestExceptionNoAttributes(SUBTYPE_HR, "Heart Rate", "12", null);
    assertBadRequestExceptionNoAttributes(SUBTYPE_HR, "Heart Rate", null, "val");
    assertBadRequestExceptionNoAttributes(SUBTYPE_PREG, "Pregnancy", null, null);
    assertBadRequestExceptionNoAttributes(SUBTYPE_PREG, "Pregnancy", "12", null);
    assertBadRequestExceptionNoAttributes(SUBTYPE_PREG, "Pregnancy", null, "val");
    assertBadRequestExceptionNoAttributes(SUBTYPE_WHEEL, "Wheel Chair User", null, null);
    assertBadRequestExceptionNoAttributes(SUBTYPE_WHEEL, "Wheel Chair User", "12", null);
    assertBadRequestExceptionNoAttributes(SUBTYPE_WHEEL, "Wheel Chair User", null, "val");
  }

  @Test
  public void countSubjectsBloodPressureBadSystolicOperand() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureBadDiastolicOperand() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureBadSystolicName() throws Exception {
    Attribute systolicAttr = new Attribute().name("Other").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureBadDiastolicName() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Other").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoSystolicOperator() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoDiastolicOperator() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoSystolicOperand() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoDiastolicOperand() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoSystolicConceptId() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90"));
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoDiastolicConceptId() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60"));
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureSystolicBetweenOneOperand() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.BETWEEN).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureDiastolicBetweenOneOperand() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureSysstolicBetweenThreeOperands() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.BETWEEN).operands(Arrays.asList("90","122","200")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureDiasstolicBetweenThreeOperands() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903118L);
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("90","122","200")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(systolicAttr, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoSystolicAttribute() throws Exception {
    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903115L);
    assertBadRequestExceptionBloodPressure(null, diastolicAttr);
  }

  @Test
  public void countSubjectsBloodPressureNoDiastolicAttribute() throws Exception {
    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903118L);
    assertBadRequestExceptionBloodPressure(systolicAttr, null);
  }

  @Test
  public void countSubjectsBloodPressureNoSystolicOrDiastolicAttribute() throws Exception {
    assertBadRequestExceptionBloodPressure(null, null);
  }

  @Test
  public void countSubjectsBloodPressureEmptyAttributes() throws Exception {
    assertBadRequestExceptionBloodPressure(new Attribute(), new Attribute());
  }

  @Test
  public void countSubjects_EmptyIcludesAndExcludes() throws Exception {
    try {
      controller.countParticipants(1L, new SearchRequest());
      fail("Should have thrown BadRequestException!");
    } catch (BadRequestException e) {
      assertEquals("Invalid SearchRequest: includes[] and excludes[] cannot both be empty", e.getMessage());
    }
  }

  @Test
  public void filterBigQueryConfig_WithoutTableName() throws Exception {
    final String statement = "my statement ${projectId}.${dataSetId}.myTableName";
    QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration.newBuilder(statement).setUseLegacySql(false).build();
    final String expectedResult = "my statement " + getTablePrefix() + ".myTableName";
    assertThat(expectedResult).isEqualTo(bigQueryService.filterBigQueryConfig(queryJobConfiguration).getQuery());
  }

  protected String getTablePrefix() {
    CdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
    return cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset();
  }

  private void assertBadRequestExceptionBloodPressure(Attribute systolicAttr, Attribute diastolicAttr) {
    List<Attribute> attributes = new ArrayList<>();
    if (systolicAttr != null) {
      attributes.add(systolicAttr);
    }
    if (diastolicAttr != null) {
      attributes.add(diastolicAttr);
    }
    Criteria hypotensive = new Criteria().type(TYPE_PM).subtype(SUBTYPE_BP)
      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
      .count("16").domainId("Measurement");
    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
    hypotensiveSP.attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestExeption!");
    } catch (BadRequestException e) {
      //success
      assertThat(e.getMessage()).isEqualTo("Please provide valid search attributes(name, operator, " +
        "operands and conceptId) for Systolic and Diastolic.");
    }
  }

  private void assertBadRequestExceptionAttributes(Attribute attribute, String subtype, String exceptionType) {
    List<Attribute> attributes = new ArrayList<>();
    if (attribute != null) {
      attributes.add(attribute);
    }
    Criteria criteria = new Criteria().type(TYPE_PM).subtype(subtype)
      .name("Name").group(false).selectable(true)
      .count("16").domainId("Measurement");
    SearchParameter searchParameter = createSearchParameter(criteria, null);
    searchParameter.attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestExeption!");
    } catch (BadRequestException e) {
      //success
      assertThat(e.getMessage()).isEqualTo("Please provide valid search attributes(operator, operands) for "
        + exceptionType + ".");
    }
  }

  private void assertBadRequestExceptionNoAttributes(String subtype, String exceptionType, String conceptId, String value) {
    Criteria criteria = new Criteria().type(TYPE_PM).subtype(subtype)
      .name("Name").group(false).selectable(true)
      .count("16").domainId("Measurement").conceptId(conceptId);
    SearchParameter searchParameter = createSearchParameter(criteria, value);
    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestExeption!");
    } catch (BadRequestException e) {
      //success
      assertThat(e.getMessage()).isEqualTo("Please provide valid conceptId and value for "
        + exceptionType + ".");
    }
  }

  private Criteria createCriteriaParent(String type, String subtype, String code) {
    return new Criteria().parentId(0).type(type).subtype(subtype)
      .code(code).name("Cholera").group(true).selectable(true)
      .count("28");
  }

  private Criteria createCriteriaChild(String type, String subtype, long parentId, String code, String domain) {
    return new Criteria().parentId(parentId).type(type).subtype(subtype)
      .code(code).name("Cholera").group(false).selectable(true)
      .count("16").domainId(domain).conceptId("44829696");
  }

  private Criteria createDemoCriteria(String type, String subtype, String conceptId) {
    return new Criteria().type(type).subtype(subtype).conceptId(conceptId);
  }

  private SearchParameter createPMSearchCriteriaWithAttributes(String type, String subtype, String criteriaName, List<Attribute> attributes) {
    Criteria criteria = new Criteria().type(type).subtype(subtype)
      .name(criteriaName).group(false).selectable(true)
      .count("16").domainId("Measurement");
    SearchParameter searchParameter = createSearchParameter(criteria, null);
    searchParameter.attributes(attributes);
    return searchParameter;
  }

  private SearchParameter createPMSearchCriteria(String type, String subtype, String criteriaName, String conceptId, String code) {
    Criteria criteria = new Criteria().type(type).subtype(subtype)
      .name(criteriaName).group(false).selectable(true)
      .count("16").domainId("Measurement").conceptId(conceptId);
    SearchParameter searchParameter = createSearchParameter(criteria, code);
    return searchParameter;
  }

  private SearchParameter createSearchParameter(Criteria criteria, String code) {
    return new SearchParameter()
      .type(criteria.getType())
      .subtype(criteria.getSubtype())
      .group(criteria.getGroup())
      .value(code)
      .domain(criteria.getDomainId())
      .conceptId(criteria.getConceptId() == null ? null : new Long(criteria.getConceptId()));
  }

  private SearchRequest createSearchRequests(String type,
                                             List<SearchParameter> parameters,
                                             List<Modifier> modifiers) {
    final SearchGroupItem searchGroupItem = new SearchGroupItem()
      .type(type)
      .searchParameters(parameters)
      .modifiers(modifiers);

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
}
