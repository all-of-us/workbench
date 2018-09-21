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
import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.DemoQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.MeasurementQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.PMQueryBuilder;
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

import java.text.MessageFormat;
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

  private static final String NUMERICAL = MeasurementQueryBuilder.NUMERICAL;
  private static final String CATEGORICAL = MeasurementQueryBuilder.CATEGORICAL;
  private static final String BOTH = MeasurementQueryBuilder.BOTH;

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
      "concept_ancestor",
      "criteria");
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
  }

  @Test
  public void countSubjectsEmptyMessageException() throws Exception {
    //icd9 no search parameters
    SearchRequest searchRequest = createSearchRequests(TreeType.ICD9.name(), new ArrayList<>(), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.PARAMETERS);

    //demo no search parameters
    searchRequest = createSearchRequests(TreeType.DEMO.name(), new ArrayList<>(), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.PARAMETERS);

    //demo no attributes
    Criteria demoAge =
      createCriteriaChild(TreeType.DEMO.name(), TreeSubType.AGE.name(), 0, null, null, null);
    SearchParameter demo = createSearchParameter(demoAge, null);
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.ATTRIBUTES);

    //demo no attribute operands
    Attribute attr = new Attribute().operator(Operator.BETWEEN);
    demo.attributes(Arrays.asList(attr));
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.OPERANDS);

    //drug no search parameters
    searchRequest = createSearchRequests(TreeType.DRUG.name(), new ArrayList<>(), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.PARAMETERS);

    //measurement no search parameters
    searchRequest = createSearchRequests(TreeType.MEAS.name(), new ArrayList<>(), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.PARAMETERS);

    //measurement no attributes
    Criteria meas =
      createCriteriaChild(TreeType.MEAS.name(), TreeSubType.LAB.name(), 0, null, null, "1");
    SearchParameter measParam = createSearchParameter(meas, null);
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.ATTRIBUTES);

    //measurement no attribute operands
    measParam.attributes(Arrays.asList(new Attribute().operator(Operator.IN).name("name")));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.OPERANDS);

    //pm no search parameters
    searchRequest = createSearchRequests(TreeType.PM.name(), new ArrayList<>(), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.PARAMETERS);

    //pm no attributes
    Criteria pm =
      createCriteriaChild(TreeType.PM.name(), TreeSubType.HEIGHT.name(), 0, null, null, null);
    SearchParameter pmParam = createSearchParameter(pm, null);
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.ATTRIBUTES);

    //pm no attribute operands
    pmParam.attributes(Arrays.asList(new Attribute().operator(Operator.IN).name("name")));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.OPERANDS);

    //visit no search parameters
    searchRequest = createSearchRequests(TreeType.VISIT.name(), new ArrayList<>(), new ArrayList<>());
    assertEmptyMessageException(searchRequest, DemoQueryBuilder.PARAMETERS);
  }

  @Test
  public void countSubjectsNotValidMessageException() throws Exception {
    //icd9 no type
    Criteria icd9ConditionChild =
      createCriteriaChild(null, null, 0, "001", null, null);
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    SearchRequest searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.TYPE, null);

    //icd9 no subtype
    icd9.type(TreeType.ICD9.name());
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.SUBTYPE, null);

    //icd9 bad subtype
    icd9.subtype("badsubtype");
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.SUBTYPE, icd9.getSubtype());

    //icd9 no domain
    icd9.subtype(TreeSubType.CM.name());
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.DOMAIN, null);

    //icd9 bad domain
    icd9.domain("baddomain");
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.DOMAIN, icd9.getDomain());

    //icd9 child no concept id
    icd9.domain(DomainType.CONDITION.name());
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.CONCEPT_ID, null);

    //icd9 parent no code
    icd9.group(true).value(null);
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.CODE, null);

    //icd9 parent empty code
    icd9.group(true).value("");
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.CODE, icd9.getValue());

    Criteria demo =
      createCriteriaChild(null, null, 0, null, null, null);
    SearchParameter demoParam = createSearchParameter(demo, null);
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertNotValidMessageException(searchRequest, AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.TYPE, null);
  }

  private void assertNotValidMessageException(SearchRequest searchRequest, String... messageParams) {
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
      Object[] args = new Object[] {messageParams[0], messageParams[1], messageParams[2]};
      String expected = new MessageFormat(message).format(args);
      assertEquals(expected, bre.getMessage());
    }
  }

//  @Test
//  public void countSubjectsICD9ChildNoDomain() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", null, "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.DOMAIN, icd9.getDomain()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      icd9.setDomain("");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.DOMAIN, icd9.getDomain()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      icd9.setDomain("blah");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.DOMAIN, icd9.getDomain()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ChildNoConceptId() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1").conceptId(null);
//    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.CONCEPT_ID, icd9.getConceptId()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ParentCodeException() throws Exception {
//    Criteria icd9ConditionParent =
//      createCriteriaParent(TreeType.ICD9.name(), TreeSubType.CM.name(), null).domainId(DomainType.CONDITION.name());
//    SearchParameter icd9 = createSearchParameter(icd9ConditionParent, null);
//    SearchRequest searchRequest = createSearchRequests(icd9ConditionParent.getType(), Arrays.asList(icd9), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.CODE, icd9.getValue()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      icd9.setValue("");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.CODE, icd9.getValue()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ParentTypeException() throws Exception {
//    Criteria icd9ConditionParent =
//      createCriteriaParent(null, TreeSubType.CM.name(), "001").domainId(DomainType.CONDITION.name());
//    SearchParameter icd9 = createSearchParameter(icd9ConditionParent, "001");
//    SearchRequest searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.TYPE, icd9.getType()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      icd9.setType("");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.TYPE, icd9.getType()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      icd9.setType("blah");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.TYPE, icd9.getType()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ParentSubtypeException() throws Exception {
//    Criteria icd9ConditionParent =
//      createCriteriaParent(TreeType.ICD9.name(), null, "001").domainId(DomainType.CONDITION.name());
//    SearchParameter icd9 = createSearchParameter(icd9ConditionParent, "001");
//    SearchRequest searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.SUBTYPE, icd9.getSubtype()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      icd9.subtype("");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.SUBTYPE, icd9.getSubtype()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      icd9.subtype("blah");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.SUBTYPE, icd9.getSubtype()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChild() throws Exception {
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildrenAgeAtEvent() throws Exception {
    Criteria icd9ProcedureChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.PROC.name(), 0, "002", DomainType.PROCEDURE.name(), "2");
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
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
  public void countSubjectsICD9ConditionOccurrenceChildrenEncounter() throws Exception {
    Criteria icd9ProcedureChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.PROC.name(), 0, "002", DomainType.PROCEDURE.name(), "2");
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    SearchParameter icd9Proc = createSearchParameter(icd9ProcedureChild, "001.1");
    Modifier modifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9, icd9Proc), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

//  @Test
//  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventInvalidLong() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    String operand = "zz";
//    Modifier modifier = new Modifier()
//      .name(ModifierType.AGE_AT_EVENT)
//      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
//      .operands(Arrays.asList(operand));
//    SearchRequest searchRequest =
//      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));
//
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown BadRequestException!");
//    } catch (BadRequestException bre) {
//      String message = AbstractQueryBuilder.OPERANDS_NUMERIC_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.MODIFIER, AbstractQueryBuilder.modifierText.get(modifier.getName())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventTooManyOperands() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    String operand1 = "1";
//    String operand2 = "2";
//    Modifier modifier = new Modifier()
//      .name(ModifierType.AGE_AT_EVENT)
//      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
//      .operands(Arrays.asList(operand1, operand2));
//    SearchRequest searchRequest =
//      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));
//
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown BadRequestException!");
//    } catch (BadRequestException bre) {
//      String message = AbstractQueryBuilder.ONE_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.MODIFIER, AbstractQueryBuilder.modifierText.get(modifier.getName()), AbstractQueryBuilder.operatorText.get(modifier.getOperator())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventBetween() throws Exception {
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
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

//  @Test
//  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventBetweenTooManyOperands() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    String operand1 = "24";
//    String operand2 = "26";
//    String operand3 = "27";
//    Modifier modifier = new Modifier()
//      .name(ModifierType.AGE_AT_EVENT)
//      .operator(Operator.BETWEEN)
//      .operands(Arrays.asList(operand1, operand2, operand3));
//    SearchRequest searchRequest =
//      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));
//
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      String message = AbstractQueryBuilder.TWO_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.MODIFIER, AbstractQueryBuilder.modifierText.get(ModifierType.AGE_AT_EVENT), AbstractQueryBuilder.operatorText.get(Operator.BETWEEN)};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventBetweenNotEnoughOperands() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    String operand1 = "24";
//    Modifier modifier = new Modifier()
//      .name(ModifierType.AGE_AT_EVENT)
//      .operator(Operator.BETWEEN)
//      .operands(Arrays.asList(operand1));
//    SearchRequest searchRequest =
//      createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9), Arrays.asList(modifier));
//
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      String message = AbstractQueryBuilder.TWO_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.MODIFIER, AbstractQueryBuilder.modifierText.get(ModifierType.AGE_AT_EVENT), AbstractQueryBuilder.operatorText.get(Operator.BETWEEN)};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ConditionOccurrenceChildNumOfOccurrencesMoreThanOne() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    Modifier modifier1 = new Modifier()
//      .name(ModifierType.NUM_OF_OCCURRENCES)
//      .operator(Operator.EQUAL)
//      .operands(Arrays.asList("1"));
//    Modifier modifier2 = new Modifier()
//      .name(ModifierType.NUM_OF_OCCURRENCES)
//      .operator(Operator.EQUAL)
//      .operands(Arrays.asList("1"));
//    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
//      Arrays.asList(icd9), Arrays.asList(modifier1, modifier2));
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      String message = AbstractQueryBuilder.ONE_MODIFIER_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.modifierText.get(ModifierType.NUM_OF_OCCURRENCES)};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsICD9ConditionOccurrenceChildNumOfOccurrencesInvalidLong() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    String operand = "c";
//    Modifier modifier1 = new Modifier()
//      .name(ModifierType.NUM_OF_OCCURRENCES)
//      .operator(Operator.EQUAL)
//      .operands(Arrays.asList(operand));
//    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
//      Arrays.asList(icd9), Arrays.asList(modifier1));
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      String message = AbstractQueryBuilder.OPERANDS_NUMERIC_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.MODIFIER, AbstractQueryBuilder.modifierText.get(ModifierType.NUM_OF_OCCURRENCES)};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() throws Exception {
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
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
    Criteria icd9ProcedureChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.PROC.name(), 0, "002", DomainType.PROCEDURE.name(), "2");
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
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
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    Modifier modifier = new Modifier()
      .name(ModifierType.EVENT_DATE)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("2009-12-03"));
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
      Arrays.asList(icd9), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

//  @Test
//  public void countSubjectsICD9ConditionOccurrenceChildEventDateInvalidDate() throws Exception {
//    Criteria icd9ConditionChild =
//      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
//    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
//    Modifier modifier = new Modifier()
//      .name(ModifierType.EVENT_DATE)
//      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
//      .operands(Arrays.asList("c"));
//    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(),
//      Arrays.asList(icd9), Arrays.asList(modifier));
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown BadRequestException!");
//    } catch (BadRequestException bre) {
//      //success
//      String message = AbstractQueryBuilder.DATE_MODIFIER_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.MODIFIER, AbstractQueryBuilder.modifierText.get(modifier.getName())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildOccurrences() throws Exception {
    Criteria icd9ConditionChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "001", DomainType.CONDITION.name(), "1");
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
    Criteria icd9ConditionParent =
      criteriaDao.save(createCriteriaParent(TreeType.ICD9.name(), TreeSubType.CM.name(), "001"));
    Criteria icd9ConditionChild =
      criteriaDao.save(createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), icd9ConditionParent.getId(), "001", DomainType.CONDITION.name(), "1"));
    SearchParameter icd9 = createSearchParameter(icd9ConditionParent, "001");
    SearchRequest searchRequest = createSearchRequests(icd9ConditionParent.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    criteriaDao.delete(icd9ConditionChild);
    criteriaDao.delete(icd9ConditionParent);
  }

  @Test
  public void countSubjectsICD9ProcedureOccurrenceChild() throws Exception {
    Criteria icd9ProcedureChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.PROC.name(), 0, "002", DomainType.PROCEDURE.name(), "2");
    SearchParameter icd9 = createSearchParameter(icd9ProcedureChild, "002.1");
    SearchRequest searchRequest = createSearchRequests(icd9ProcedureChild.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ProcedureOccurrenceParent() throws Exception {
    Criteria icd9ProcedureParent =
      criteriaDao.save(createCriteriaParent(TreeType.ICD9.name(), TreeSubType.PROC.name(), "002"));
    Criteria icd9ProcedureChild =
      criteriaDao.save(createCriteriaChild(TreeType.ICD9.name(), TreeSubType.PROC.name(), icd9ProcedureParent.getId(), "002", DomainType.PROCEDURE.name(), "2"));
    SearchParameter icd9 = createSearchParameter(icd9ProcedureParent, "002");
    SearchRequest searchRequest = createSearchRequests(icd9ProcedureParent.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    criteriaDao.delete(icd9ProcedureChild);
    criteriaDao.delete(icd9ProcedureParent);
  }

  @Test
  public void countSubjectsICD9MeasurementChild() throws Exception {
    Criteria icd9MeasurementChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "003", DomainType.MEASUREMENT.name(), "3");
    SearchParameter icd9 = createSearchParameter(icd9MeasurementChild, "003.1");
    SearchRequest searchRequest = createSearchRequests(icd9MeasurementChild.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9MeasurementParent() throws Exception {
    Criteria icd9MeasurementParent =
      criteriaDao.save(createCriteriaParent(TreeType.ICD9.name(), TreeSubType.CM.name(), "003"));
    Criteria icd9MeasurementChild =
      criteriaDao.save(createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), icd9MeasurementParent.getId(), "003", DomainType.MEASUREMENT.name(), "3"));
    SearchParameter icd9 = createSearchParameter(icd9MeasurementParent, "003");
    SearchRequest searchRequest = createSearchRequests(icd9MeasurementParent.getType(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    criteriaDao.delete(icd9MeasurementChild);
    criteriaDao.delete(icd9MeasurementParent);
  }

  @Test
  public void countSubjectsDemoGender() throws Exception {
    Criteria demoGender = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.GEN.name(), "8507");
    SearchParameter demo = createSearchParameter(demoGender, null);
    SearchRequest searchRequest = createSearchRequests(demoGender.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoEthnicity() throws Exception {
    Criteria demoEthnicity = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.ETH.name(), "9898");
    SearchParameter demo = createSearchParameter(demoEthnicity, null);
    SearchRequest searchRequest = createSearchRequests(demoEthnicity.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoDec() throws Exception {
    Criteria demoGender = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.DEC.name(), null);
    SearchParameter demo = createSearchParameter(demoGender, "Deceased");
    SearchRequest searchRequest = createSearchRequests(demoGender.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

//  @Test
//  public void countSubjectsDemoDecValueException() throws Exception {
//    Criteria demoGender = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.DEC.name(), null);
//    SearchParameter demo = createSearchParameter(demoGender, "");
//    SearchRequest searchRequest = createSearchRequests(demoGender.getType(), Arrays.asList(demo), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.DEC, demo.getValue()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      demo.setValue("blah");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, DemoQueryBuilder.DEC, demo.getValue()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsDemoAge() throws Exception {
    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    Criteria demoAge = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.AGE.name(), null);
    SearchParameter demo = createSearchParameter(demoAge, null);
    demo.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));
    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

//  @Test
//  public void countSubjectsDemoTypeException() throws Exception {
//    Criteria demoAge = createDemoCriteria(null, TreeSubType.AGE.name(), null);
//    SearchParameter demo = createSearchParameter(demoAge, null);
//    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.TYPE, demo.getType()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      demo.setType("");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.TYPE, demo.getType()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      demo.setType("blah");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.TYPE, demo.getType()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsDemoSubtypeException() throws Exception {
//    Criteria demoAge = createDemoCriteria(TreeType.DEMO.name(), null, null);
//    SearchParameter demo = createSearchParameter(demoAge, null);
//    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.SUBTYPE, demo.getSubtype()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      demo.setSubtype("");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.SUBTYPE, demo.getSubtype()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      demo.setSubtype("blah");
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.SUBTYPE, demo.getSubtype()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsDemoConceptIdException() throws Exception {
//    Criteria demoRace = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.RACE.name(), null);
//    SearchParameter demo = createSearchParameter(demoRace, null);
//    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.CONCEPT_ID, demoRace.getConceptId()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsDemoAgeAndDeceased() throws Exception {
//    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
//    DateTime now = new DateTime();
//    Period period = new Period(birthDate, now);
//    Integer age = period.getYears();
//    Criteria demoAge = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.AGE.name(), null);
//    Criteria demoDec = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.DEC.name(), null);
//    SearchParameter demoAgeParameter = createSearchParameter(demoAge, null);
//    SearchParameter demoDecParameter = createSearchParameter(demoDec, null);
//    demoAgeParameter.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));
//    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demoAgeParameter, demoDecParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      assertEquals(DemoQueryBuilder.AGE_DEC_MESSAGE, bre.getMessage());
//    }
//  }


//  @Test
//  public void countSubjectsDemoAgeAtributeExceptions() throws Exception {
//    Attribute attribute = new Attribute();
//    Criteria demoAge = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.AGE.name(), null);
//    SearchParameter demoAgeParameter = createSearchParameter(demoAge, null);
//    demoAgeParameter.attributes(Arrays.asList(attribute));
//    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demoAgeParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.OPERATOR, attribute.getOperator()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      attribute.name("Age").operator(Operator.EQUAL).operands(Arrays.asList("1", "2"));
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.ONE_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, attribute.getName(), AbstractQueryBuilder.operatorText.get(attribute.getOperator())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      attribute.operator(Operator.BETWEEN).operands(Arrays.asList("1"));
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.TWO_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, attribute.getName(), AbstractQueryBuilder.operatorText.get(attribute.getOperator())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//
//    try {
//      attribute.operator(Operator.EQUAL).operands(Arrays.asList("blah"));
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.OPERANDS_NUMERIC_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, attribute.getName()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsDemoAgeBetween() throws Exception {
    Criteria demoAge = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.AGE.name(), null);
    SearchParameter demo = createSearchParameter(demoAge, null);
    demo.attributes(Arrays.asList(
      new Attribute().operator(Operator.BETWEEN).operands(Arrays.asList("15","99"))
    ));
    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsDemoGenderAndAge() throws Exception {
    Criteria demoGender = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.GEN.name(), "8507");
    SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    Criteria demoAge = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.AGE.name(), null);
    SearchParameter demoAgeSearchParam = createSearchParameter(demoAge, null);
    demoAgeSearchParam.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));

    SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demoGenderSearchParam, demoAgeSearchParam), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsICD9AndDemo() throws Exception {
    Criteria icd9MeasurementChild =
      createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), 0, "003", DomainType.MEASUREMENT.name(), "3");
    Criteria demoGender = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.GEN.name(), "8507");
    SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    Criteria demoAge = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.AGE.name(), null);
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
    Criteria demoGender = createDemoCriteria(TreeType.DEMO.name(), TreeSubType.GEN.name(), "8507");
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
  public void countSubjectsICD10ConditionOccurrenceChildEncounter() throws Exception {
    Criteria icd10ConditionChild =
      createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), 0, "A09", DomainType.CONDITION.name(), "6");
    SearchParameter icd10 = createSearchParameter(icd10ConditionChild, "A09");
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(icd10ConditionChild.getType(), Arrays.asList(icd10), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ConditionOccurrenceChild() throws Exception {
    Criteria icd10ConditionChild =
      createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), 0, "A09", DomainType.CONDITION.name(), "6");
    SearchParameter icd10 = createSearchParameter(icd10ConditionChild, "A09");
    SearchRequest searchRequest = createSearchRequests(icd10ConditionChild.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildICD10ConditionOccurrenceChild() throws Exception {
    Criteria icd9ConditionParent =
      criteriaDao.save(createCriteriaParent(TreeType.ICD9.name(), TreeSubType.CM.name(), "001"));
    Criteria icd9ConditionChild =
      criteriaDao.save(createCriteriaChild(TreeType.ICD9.name(), TreeSubType.CM.name(), icd9ConditionParent.getId(), "001", DomainType.CONDITION.name(), "1"));
    Criteria icd10ConditionChild =
      createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), 0, "A09", DomainType.CONDITION.name(), "6");
    SearchParameter icd9P = createSearchParameter(icd9ConditionParent, "001");
    SearchParameter icd10 = createSearchParameter(icd10ConditionChild, "A09");
    SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9P, icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
    criteriaDao.delete(icd9ConditionChild);
    criteriaDao.delete(icd9ConditionParent);
  }

  @Test
  public void countSubjectsICD10ConditionOccurrenceParent() throws Exception {
    Criteria icd10ConditionParent =
      criteriaDao.save(createCriteriaParent(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), "A"));
    Criteria icd10ConditionChild =
      criteriaDao.save(createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), icd10ConditionParent.getId(), "A09", DomainType.CONDITION.name(), "6"));
    SearchParameter icd10 = createSearchParameter(icd10ConditionParent, "A");
    SearchRequest searchRequest = createSearchRequests(icd10ConditionParent.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    criteriaDao.delete(icd10ConditionChild);
    criteriaDao.delete(icd10ConditionParent);
  }

  @Test
  public void countSubjectsICD10ProcedureOccurrenceChild() throws Exception {
    Criteria icd10ProcedureChild =
      createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10PCS.name(), 0, "16070", DomainType.PROCEDURE.name(), "8");
    SearchParameter icd10 = createSearchParameter(icd10ProcedureChild, "16070");
    SearchRequest searchRequest = createSearchRequests(icd10ProcedureChild.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ProcedureOccurrenceParent() throws Exception {
    Criteria icd10ProcedureParent =
      criteriaDao.save(createCriteriaParent(TreeType.ICD10.name(), TreeSubType.ICD10PCS.name(), "16"));
    Criteria icd10ProcedureChild =
      criteriaDao.save(createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10PCS.name(), icd10ProcedureParent.getId(), "16070", DomainType.PROCEDURE.name(), "8"));
    SearchParameter icd10 = createSearchParameter(icd10ProcedureParent, "16");
    SearchRequest searchRequest = createSearchRequests(icd10ProcedureParent.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    criteriaDao.delete(icd10ProcedureChild);
    criteriaDao.delete(icd10ProcedureParent);
  }

  @Test
  public void countSubjectsICD10MeasurementChild() throws Exception {
    Criteria icd10MeasurementChild =
      createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), 0, "R92.2", DomainType.MEASUREMENT.name(), "9");
    SearchParameter icd10 = createSearchParameter(icd10MeasurementChild, "R92.2");
    SearchRequest searchRequest = createSearchRequests(icd10MeasurementChild.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10MeasurementParent() throws Exception {
    Criteria icd10MeasurementParent =
      criteriaDao.save(createCriteriaParent(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), "R92"));
    Criteria icd10MeasurementChild =
      criteriaDao.save(createCriteriaChild(TreeType.ICD10.name(), TreeSubType.ICD10CM.name(), icd10MeasurementParent.getId(), "R92.2", DomainType.MEASUREMENT.name(), "9"));
    SearchParameter icd10 = createSearchParameter(icd10MeasurementParent, "R92");
    SearchRequest searchRequest = createSearchRequests(icd10MeasurementParent.getType(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
    criteriaDao.delete(icd10MeasurementChild);
    criteriaDao.delete(icd10MeasurementParent);
  }

  @Test
  public void countSubjectsCPTProcedureOccurrence() throws Exception {
    Criteria cptProcedure =
      createCriteriaChild(TreeType.CPT.name(), TreeSubType.CPT4.name(), 1L, "0001T", DomainType.PROCEDURE.name(), "4");
    SearchParameter cpt = createSearchParameter(cptProcedure, "0001T");
    SearchRequest searchRequest = createSearchRequests(cptProcedure.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTProcedureOccurrenceEncounter() throws Exception {
    Criteria cptProcedure =
      createCriteriaChild(TreeType.CPT.name(), TreeSubType.CPT4.name(), 1L, "0001T", DomainType.PROCEDURE.name(), "4");
    SearchParameter cpt = createSearchParameter(cptProcedure, "0001T");
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(cptProcedure.getType(), Arrays.asList(cpt), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTObservation() throws Exception {
    Criteria cptObservation =
      createCriteriaChild(TreeType.CPT.name(), TreeSubType.CPT4.name(), 1L, "0001Z", DomainType.OBSERVATION.name(), "5");
    SearchParameter cpt = createSearchParameter(cptObservation, "0001Z");
    SearchRequest searchRequest = createSearchRequests(cptObservation.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTMeasurement() throws Exception {
    Criteria cptMeasurement =
      createCriteriaChild(TreeType.CPT.name(), TreeSubType.CPT4.name(), 1L, "0001Q", DomainType.MEASUREMENT.name(), "10");
    SearchParameter cpt = createSearchParameter(cptMeasurement, "0001Q");
    SearchRequest searchRequest = createSearchRequests(cptMeasurement.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTDrugExposure() throws Exception {
    Criteria cptDrug =
      createCriteriaChild(TreeType.CPT.name(), TreeSubType.CPT4.name(), 1L, "90703", DomainType.DRUG.name(), "11");
    SearchParameter cpt = createSearchParameter(cptDrug, "90703");
    SearchRequest searchRequest = createSearchRequests(cptDrug.getType(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsVisitChild() throws Exception {
    Criteria visitCriteria = new Criteria().type(TreeType.VISIT.name()).group(false).conceptId("10");
    SearchParameter visit = createSearchParameter(visitCriteria, null);
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsVisitParent() throws Exception {
    Criteria visitCriteria = new Criteria().type(TreeType.VISIT.name()).group(true).conceptId("1");
    SearchParameter visit = createSearchParameter(visitCriteria, null);
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsVisitChildOrParent() throws Exception {
    Criteria visitChildCriteria = new Criteria().type(TreeType.VISIT.name()).group(false).conceptId("1");
    Criteria visitParentCriteria = new Criteria().type(TreeType.VISIT.name()).group(true).conceptId("1");
    SearchParameter visitChild = createSearchParameter(visitChildCriteria, null);
    SearchParameter visitParent = createSearchParameter(visitParentCriteria, null);
    SearchRequest searchRequest = createSearchRequests(visitChild.getType(), Arrays.asList(visitChild, visitParent), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

//  @Test
//  public void countSubjectsVisitChildNullConceptId() throws Exception {
//    Criteria visitCriteria = new Criteria().type(TreeType.VISIT.name()).group(false);
//    SearchParameter visit = createSearchParameter(visitCriteria, null);
//    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException e) {
//      // success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.CONCEPT_ID, visitCriteria.getConceptId()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

  @Test
  public void countSubjectsVisitChildModifiers() throws Exception {
    Criteria visitCriteria = new Criteria().type(TreeType.VISIT.name()).group(false).conceptId("10");
    SearchParameter visit = createSearchParameter(visitCriteria, null);
    Modifier modifier = new Modifier()
    .name(ModifierType.NUM_OF_OCCURRENCES)
    .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
    .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

//  @Test
//  public void countSubjectsDrugNoConceptIdOnSearchParameter() throws Exception {
//    Criteria drugCriteria = new Criteria().type(TreeType.DRUG.name()).group(false);
//    SearchParameter drug = createSearchParameter(drugCriteria, null);
//    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.CONCEPT_ID, drug.getConceptId()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsDrugChild() throws Exception {
    Criteria drugCriteria = new Criteria().type(TreeType.DRUG.name()).group(false).conceptId("11");
    SearchParameter drug = createSearchParameter(drugCriteria, null);
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugChildEncounter() throws Exception {
    Criteria drugCriteria = new Criteria().type(TreeType.DRUG.name()).group(false).conceptId("11");
    SearchParameter drug = createSearchParameter(drugCriteria, null);
    Modifier modifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugChildModifiers() throws Exception {
    Criteria drugCriteria = new Criteria().type(TreeType.DRUG.name()).group(false).conceptId("11");
    SearchParameter drug = createSearchParameter(drugCriteria, null);
    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabTextAnyEncounter() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
    Modifier modifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

//  @Test
//  public void countSubjectsLabTextAnyEncounterNoInOperator() throws Exception {
//    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
//    SearchParameter lab = createSearchParameter(labCriteria, null);
//    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
//    Modifier modifier = new Modifier()
//      .name(ModifierType.ENCOUNTERS)
//      .operator(Operator.EQUAL)
//      .operands(Arrays.asList("1"));
//    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_IN_MODIFIER_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.modifierText.get(modifier.getName()), Operator.IN};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsLabTextAnyEncounterConceptIdNotANumber() throws Exception {
//    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
//    SearchParameter lab = createSearchParameter(labCriteria, null);
//    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
//    Modifier modifier = new Modifier()
//      .name(ModifierType.ENCOUNTERS)
//      .operator(Operator.IN)
//      .operands(Arrays.asList("x"));
//    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.OPERANDS_NUMERIC_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.MODIFIER, AbstractQueryBuilder.modifierText.get(modifier.getName())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

  @Test
  public void countSubjectsLabTextAny() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalAny() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalBetween() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(NUMERICAL).operator(Operator.BETWEEN).operands(Arrays.asList("0", "1"))));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalAny() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalIn() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(CATEGORICAL).operator(Operator.IN).operands(Arrays.asList("1"))));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothAny() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothNumericalAndCategorical() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    Attribute numerical = new Attribute().name(BOTH).operator(Operator.EQUAL).operands(Arrays.asList("0.1"));
    Attribute categorical = new Attribute().name(BOTH).operator(Operator.IN).operands(Arrays.asList("1"));
    lab.attributes(Arrays.asList(numerical, categorical));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothNumericalAndCategoricalSpecificName() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    Attribute numerical = new Attribute().name(NUMERICAL).operator(Operator.EQUAL).operands(Arrays.asList("0.1"));
    Attribute categorical = new Attribute().name(CATEGORICAL).operator(Operator.IN).operands(Arrays.asList("1"));
    lab.attributes(Arrays.asList(numerical, categorical));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothCategorical() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    Attribute categorical = new Attribute().name(BOTH).operator(Operator.IN).operands(Arrays.asList("1"));
    lab.attributes(Arrays.asList(categorical));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothNumerical() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    Attribute numerical = new Attribute().name(BOTH).operator(Operator.EQUAL).operands(Arrays.asList("1.0"));
    lab.attributes(Arrays.asList(numerical));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalAnyAgeAtEvent() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(MeasurementQueryBuilder.ANY)));
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.GREATER_THAN_OR_EQUAL_TO).operands(Arrays.asList("25"));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameter() throws Exception {
    Criteria labCriteria1 = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    Criteria labCriteria2 = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("9");
    Criteria labCriteria3 = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("9");
    SearchParameter lab1 = createSearchParameter(labCriteria1, null);
    SearchParameter lab2 = createSearchParameter(labCriteria2, null);
    SearchParameter lab3 = createSearchParameter(labCriteria3, null);
    Attribute labText = new Attribute().name(MeasurementQueryBuilder.ANY);
    Attribute labNumerical = new Attribute().name(MeasurementQueryBuilder.ANY);
    Attribute labCategorical = new Attribute().name(CATEGORICAL).operator(Operator.IN).operands(Arrays.asList("77"));
    lab1.attributes(Arrays.asList(labText));
    lab2.attributes(Arrays.asList(labNumerical));
    lab3.attributes(Arrays.asList(labCategorical));
    SearchRequest searchRequest = createSearchRequests(lab1.getType(), Arrays.asList(lab1, lab2, lab3), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsBloodPressure() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L),
      new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("60","80")).conceptId(903115L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.BP.name(), "BP Name", attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureAny() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(PMQueryBuilder.ANY).operands(new ArrayList<>()).conceptId(903118L),
      new Attribute().name(PMQueryBuilder.ANY).operands(new ArrayList<>()).conceptId(903115L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.BP.name(), "BP Name", attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetailOrHeartRateIrr() throws Exception {
    List<Attribute> bpAttributes = Arrays.asList(
      new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L),
      new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("60","80")).conceptId(903115L)
    );
    SearchParameter bpSearchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.BP.name(), "BP Name", bpAttributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(bpSearchParameter), new ArrayList<>());

    List<Attribute> hrAttributes = Arrays.asList(
      new Attribute().name("Heart Rate").operator(Operator.EQUAL).operands(Arrays.asList("71")).conceptId(903126L)
    );
    SearchParameter hrSearchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.HR_DETAIL.name(), "Heart Rate Detail", hrAttributes);
    SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type(TreeType.PM.name()).searchParameters(Arrays.asList(hrSearchParameter)).modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    SearchParameter heartRateIrr = createPMSearchCriteria(TreeType.PM.name(), TreeSubType.HR.name(), "Heart Rate Irr", "1586218", "4262985");
    SearchGroupItem heartRateIrrSearchGroupItem = new SearchGroupItem().type(TreeType.PM.name()).searchParameters(Arrays.asList(heartRateIrr)).modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(heartRateIrrSearchGroupItem);

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsHeartRateNoIrr() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TreeType.PM.name(), TreeSubType.HR.name(), "Heart Rate Irr", "1586218", "4297303");
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsHeight() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Height").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("168")).conceptId(903133L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.HEIGHT.name(), "Height", attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsWeight() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Weight").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("201")).conceptId(903121L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.WEIGHT.name(), "Weight", attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBMI() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("BMI").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("263")).conceptId(903124L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.BMI.name(), "BMI", attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWaistCircumference() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Waist").operator(Operator.EQUAL).operands(Arrays.asList("31")).conceptId(903135L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.WC.name(), "Waist", attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectHipCircumference() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name("Hip").operator(Operator.EQUAL).operands(Arrays.asList("33")).conceptId(903136L)
    );
    SearchParameter searchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.HC.name(), "Hip", attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectPregnant() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TreeType.PM.name(), TreeSubType.PREG.name(), "Pregnancy", "903120", "45877994");
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWheelChairUser() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TreeType.PM.name(), TreeSubType.WHEEL.name(), "Wheel Chair User", "903111", "4023190");
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void getDemoChartInfo() throws Exception {
    SearchParameter searchParameter = createPMSearchCriteria(TreeType.PM.name(), TreeSubType.WHEEL.name(), "Wheel Chair User", "903111", "4023190");
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());

    DemoChartInfoListResponse response = controller.getDemoChartInfo(cdrVersion.getCdrVersionId(), searchRequest).getBody();
    assertEquals(response.getItems().size(), 1);
    assertEquals(response.getItems().get(0), new DemoChartInfo().gender("M").race("Unknown").ageRange("19-44").count(1L));
  }

//  @Test
//  public void countSubjectWheelChairUserBadValue() throws Exception {
//    SearchParameter searchParameter = createPMSearchCriteria(TreeType.PM.name(), TreeSubType.WHEEL.name(), "Wheel Chair User", "903111", null);
//    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(searchParameter), new ArrayList<>());
//
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestException!");
//    } catch (BadRequestException bre) {
//      //Success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, AbstractQueryBuilder.VALUE, searchParameter.getValue()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, bre.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBadOperand() throws Exception {
//    Attribute attribute = new Attribute().name("Heart Rate Detail").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903126L);
//    assertBadRequestExceptionBadOperand(attribute, TreeType.PM.name(), TreeSubType.HR_DETAIL.name(), "Measurement");
//
//    attribute = new Attribute().name("BMI").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903124L);
//    assertBadRequestExceptionBadOperand(attribute, TreeType.PM.name(), TreeSubType.BMI.name(), "Measurement");
//
//    attribute = new Attribute().name("Height").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903133L);
//    assertBadRequestExceptionBadOperand(attribute, TreeType.PM.name(), TreeSubType.HEIGHT.name(), "Measurement");
//
//    attribute = new Attribute().name("Weight").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903121L);
//    assertBadRequestExceptionBadOperand(attribute, TreeType.PM.name(), TreeSubType.WEIGHT.name(), "Measurement");
//
//    attribute = new Attribute().name("Waist").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903135L);
//    assertBadRequestExceptionBadOperand(attribute, TreeType.PM.name(), TreeSubType.WC.name(), "Measurement");
//
//    attribute = new Attribute().name("Hip").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903136L);
//    assertBadRequestExceptionBadOperand(attribute, TreeType.PM.name(), TreeSubType.HC.name(), "Measurement");
//  }

//  @Test
//  public void countSubjectsNoOperator() throws Exception {
//    Attribute attribute = new Attribute().name("Heart Rate Detail").operands(Arrays.asList("10")).conceptId(903126L);
//    assertBadRequestExceptionNoOperator(attribute, TreeType.PM.name(), TreeSubType.HR_DETAIL.name(), "Measurement");
//
//    attribute = new Attribute().name("BMI").operands(Arrays.asList("10")).conceptId(903124L);
//    assertBadRequestExceptionNoOperator(attribute, TreeType.PM.name(), TreeSubType.BMI.name(), "Measurement");
//
//    attribute = new Attribute().name("Height").operands(Arrays.asList("10")).conceptId(903133L);
//    assertBadRequestExceptionNoOperator(attribute, TreeType.PM.name(), TreeSubType.HEIGHT.name(), "Measurement");
//
//    attribute = new Attribute().name("Weight").operands(Arrays.asList("10")).conceptId(903121L);
//    assertBadRequestExceptionNoOperator(attribute, TreeType.PM.name(), TreeSubType.WEIGHT.name(), "Measurement");
//
//    attribute = new Attribute().name("Waist").operands(Arrays.asList("10")).conceptId(903135L);
//    assertBadRequestExceptionNoOperator(attribute, TreeType.PM.name(), TreeSubType.WC.name(), "Measurement");
//
//    attribute = new Attribute().name("Hip").operands(Arrays.asList("10")).conceptId(903136L);
//    assertBadRequestExceptionNoOperator(attribute, TreeType.PM.name(), TreeSubType.HC.name(), "Measurement");
//  }

//  @Test
//  public void countSubjectsNoConceptId() throws Exception {
//    Attribute attribute = new Attribute().name("Heart Rate Detail").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
//    assertBadRequestExceptionNoConceptId(attribute, TreeType.PM.name(), TreeSubType.HR_DETAIL.name(), "Measurement");
//
//    attribute = new Attribute().name("BMI").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
//    assertBadRequestExceptionNoConceptId(attribute, TreeType.PM.name(), TreeSubType.BMI.name(), "Measurement");
//
//    attribute = new Attribute().name("Height").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
//    assertBadRequestExceptionNoConceptId(attribute, TreeType.PM.name(), TreeSubType.HEIGHT.name(), "Measurement");
//
//    attribute = new Attribute().name("Weight").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
//    assertBadRequestExceptionNoConceptId(attribute, TreeType.PM.name(), TreeSubType.WEIGHT.name(), "Measurement");
//
//    attribute = new Attribute().name("Waist").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
//    assertBadRequestExceptionNoConceptId(attribute, TreeType.PM.name(), TreeSubType.WC.name(), "Measurement");
//
//    attribute = new Attribute().name("Hip").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("10"));
//    assertBadRequestExceptionNoConceptId(attribute, TreeType.PM.name(), TreeSubType.HC.name(), "Measurement");
//  }

//  @Test
//  public void countSubjectsEmptyAttribute() throws Exception {
//    Attribute attribute = new Attribute();
//    assertBadRequestExceptionEmptyAttribute(attribute, TreeType.PM.name(), TreeSubType.HR_DETAIL.name(), "Measurement");
//    assertBadRequestExceptionEmptyAttribute(attribute, TreeType.PM.name(), TreeSubType.BMI.name(), "Measurement");
//    assertBadRequestExceptionEmptyAttribute(attribute, TreeType.PM.name(), TreeSubType.HEIGHT.name(), "Measurement");
//    assertBadRequestExceptionEmptyAttribute(attribute, TreeType.PM.name(), TreeSubType.WEIGHT.name(), "Measurement");
//    assertBadRequestExceptionEmptyAttribute(attribute, TreeType.PM.name(), TreeSubType.WC.name(), "Measurement");
//    assertBadRequestExceptionEmptyAttribute(attribute, TreeType.PM.name(), TreeSubType.HC.name(), "Measurement");
//  }

//  @Test
//  public void countSubjectsNoAttribute() throws Exception {
//    assertBadRequestExceptionNoAttribute(null, TreeType.PM.name(), TreeSubType.HR_DETAIL.name(), "Measurement");
//    assertBadRequestExceptionNoAttribute(null, TreeType.PM.name(), TreeSubType.BMI.name(), "Measurement");
//    assertBadRequestExceptionNoAttribute(null, TreeType.PM.name(), TreeSubType.HEIGHT.name(), "Measurement");
//    assertBadRequestExceptionNoAttribute(null, TreeType.PM.name(), TreeSubType.WEIGHT.name(), "Measurement");
//    assertBadRequestExceptionNoAttribute(null, TreeType.PM.name(), TreeSubType.WC.name(), "Measurement");
//    assertBadRequestExceptionNoAttribute(null, TreeType.PM.name(), TreeSubType.HC.name(), "Measurement");
//  }

//  @Test
//  public void countSubjectsValidateNonAttribute() throws Exception {
//    assertBadRequestExceptionNoAttributes(TreeSubType.HR.name(), null, null);
//    assertBadRequestExceptionNoAttributes(TreeSubType.HR.name(), "12", null);
//    assertBadRequestExceptionNoAttributes(TreeSubType.HR.name(), null, "val");
//    assertBadRequestExceptionNoAttributes(TreeSubType.PREG.name(), null, null);
//    assertBadRequestExceptionNoAttributes(TreeSubType.PREG.name(), "12", null);
//    assertBadRequestExceptionNoAttributes(TreeSubType.PREG.name(), null, "val");
//    assertBadRequestExceptionNoAttributes(TreeSubType.WHEEL.name(), null, null);
//    assertBadRequestExceptionNoAttributes(TreeSubType.WHEEL.name(), "12", null);
//    assertBadRequestExceptionNoAttributes(TreeSubType.WHEEL.name(), null, "val");
//  }

//  @Test
//  public void countSubjectsBloodPressureBadSystolicOperand() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.OPERANDS_NUMERIC_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, systolicAttr.getName()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureBadDiastolicOperand() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("zz")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.OPERANDS_NUMERIC_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, diastolicAttr.getName()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureBadSystolicName() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Other").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      assertEquals(PMQueryBuilder.BP_TWO_ATTRIBUTE_MESSAGE, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureBadDiastolicName() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Other").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      assertEquals(PMQueryBuilder.BP_TWO_ATTRIBUTE_MESSAGE, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureNoSystolicOperator() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.OPERATOR, systolicAttr.getOperator()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureNoDiastolicOperator() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.OPERATOR, diastolicAttr.getOperator()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureNoSystolicConceptId() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90"));
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.CONCEPT_ID, systolicAttr.getConceptId()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureNoDiastolicConceptId() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60"));
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.CONCEPT_ID, diastolicAttr.getConceptId()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureSystolicBetweenOneOperand() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.BETWEEN).operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.TWO_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, systolicAttr.getName(), AbstractQueryBuilder.operatorText.get(systolicAttr.getOperator())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureDiastolicBetweenOneOperand() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.TWO_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, diastolicAttr.getName(), AbstractQueryBuilder.operatorText.get(diastolicAttr.getOperator())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureSysstolicBetweenThreeOperands() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.BETWEEN).operands(Arrays.asList("90","122","200")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.TWO_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, systolicAttr.getName(), AbstractQueryBuilder.operatorText.get(systolicAttr.getOperator())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureDiasstolicBetweenThreeOperands() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903118L);
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.BETWEEN).operands(Arrays.asList("90","122","200")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr, diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.TWO_OPERAND_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, diastolicAttr.getName(), AbstractQueryBuilder.operatorText.get(diastolicAttr.getOperator())};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureNoSystolicAttribute() throws Exception {
//    Attribute diastolicAttr = new Attribute().name("Diastolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903115L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(diastolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      assertThat(e.getMessage()).isEqualTo(PMQueryBuilder.BP_TWO_ATTRIBUTE_MESSAGE);
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureNoDiastolicAttribute() throws Exception {
//    Attribute systolicAttr = new Attribute().name("Systolic").operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("60")).conceptId(903118L);
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(systolicAttr));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      assertEquals(PMQueryBuilder.BP_TWO_ATTRIBUTE_MESSAGE, e.getMessage());
//    }
//  }

//  @Test
//  public void countSubjectsBloodPressureEmptyAttributes() throws Exception {
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(Arrays.asList(new Attribute(), new Attribute()));
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      assertThat(e.getMessage()).isEqualTo(PMQueryBuilder.BP_TWO_ATTRIBUTE_MESSAGE);
//    }
//  }

//  @Test
//  public void countSubjects_EmptyIcludesAndExcludes() throws Exception {
//    try {
//      controller.countParticipants(1L, new SearchRequest());
//      fail("Should have thrown BadRequestException!");
//    } catch (BadRequestException e) {
//      assertEquals("Invalid SearchRequest: includes[] and excludes[] cannot both be empty", e.getMessage());
//    }
//  }

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

//  private void assertBadRequestExceptionBloodPressure(Attribute systolicAttr, Attribute diastolicAttr) {
//    List<Attribute> attributes = new ArrayList<>();
//    if (systolicAttr != null) {
//      attributes.add(systolicAttr);
//    }
//    if (diastolicAttr != null) {
//      attributes.add(diastolicAttr);
//    }
//    Criteria hypotensive = new Criteria().type(TreeType.PM.name()).subtype(TreeSubType.BP.name())
//      .name("Hypotensive (Systolic <= 90 / Diastolic <= 60)").group(false).selectable(true)
//      .count("16").domainId("Measurement");
//    SearchParameter hypotensiveSP = createSearchParameter(hypotensive, null);
//    hypotensiveSP.attributes(attributes);
//    SearchRequest searchRequest = createSearchRequests(hypotensive.getType(), Arrays.asList(hypotensiveSP), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      assertThat(e.getMessage()).isEqualTo("Please provide valid search attributes(name, operator, " +
//        "operands and conceptId) for Systolic and Diastolic.");
//    }
//  }

//  private void assertBadRequestExceptionNoOperator(Attribute attribute, String type, String subtype, String domain) {
//    List<Attribute> attributes = new ArrayList<>();
//    if (attribute != null) {
//      attributes.add(attribute);
//    }
//    Criteria criteria = new Criteria().type(type).subtype(subtype)
//      .name("Name").group(false).selectable(true)
//      .count("16").domainId(domain);
//    SearchParameter searchParameter = createSearchParameter(criteria, null);
//    searchParameter.attributes(attributes);
//    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.OPERATOR, attribute.getOperator()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  private void assertBadRequestExceptionNoConceptId(Attribute attribute, String type, String subtype, String domain) {
//    List<Attribute> attributes = new ArrayList<>();
//    if (attribute != null) {
//      attributes.add(attribute);
//    }
//    Criteria criteria = new Criteria().type(type).subtype(subtype)
//      .name("Name").group(false).selectable(true)
//      .count("16").domainId(domain);
//    SearchParameter searchParameter = createSearchParameter(criteria, null);
//    searchParameter.attributes(attributes);
//    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.CONCEPT_ID, attribute.getConceptId()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  private void assertBadRequestExceptionEmptyAttribute(Attribute attribute, String type, String subtype, String domain) {
//    List<Attribute> attributes = new ArrayList<>();
//    if (attribute != null) {
//      attributes.add(attribute);
//    }
//    Criteria criteria = new Criteria().type(type).subtype(subtype)
//      .name("Name").group(false).selectable(true)
//      .count("16").domainId(domain);
//    SearchParameter searchParameter = createSearchParameter(criteria, null);
//    searchParameter.attributes(attributes);
//    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, AbstractQueryBuilder.NAME, attribute.getName()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  private void assertBadRequestExceptionBadOperand(Attribute attribute, String type, String subtype, String domain) {
//    List<Attribute> attributes = new ArrayList<>();
//    if (attribute != null) {
//      attributes.add(attribute);
//    }
//    Criteria criteria = new Criteria().type(type).subtype(subtype)
//      .name("Name").group(false).selectable(true)
//      .count("16").domainId(domain);
//    SearchParameter searchParameter = createSearchParameter(criteria, null);
//    searchParameter.attributes(attributes);
//    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.OPERANDS_NUMERIC_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.ATTRIBUTE, attribute.getName()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }



//  private void assertBadRequestExceptionAttributes(Attribute attribute, String type, String subtype, String domain) {
//    List<Attribute> attributes = new ArrayList<>();
//    if (attribute != null) {
//      attributes.add(attribute);
//    }
//    Criteria criteria = new Criteria().type(type).subtype(subtype)
//      .name("Name").group(false).selectable(true)
//      .count("16").domainId(domain);
//    SearchParameter searchParameter = createSearchParameter(criteria, null);
//    searchParameter.attributes(attributes);
//    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.OPERATOR, attribute.getOperator()};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

//  private void assertBadRequestExceptionNoAttributes(String subtype, String conceptId, String value) {
//    Criteria criteria = new Criteria().type(TreeType.PM.name()).subtype(subtype)
//      .name("Name").group(false).selectable(true)
//      .count("16").domainId("Measurement").conceptId(conceptId);
//    SearchParameter searchParameter = createSearchParameter(criteria, value);
//    SearchRequest searchRequest = createSearchRequests(criteria.getType(), Arrays.asList(searchParameter), new ArrayList<>());
//    try {
//      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
//      fail("Should have thrown a BadRequestExeption!");
//    } catch (BadRequestException e) {
//      //success
//      String message = AbstractQueryBuilder.NOT_VALID_MESSAGE;
//      Object[] args = new Object[] {AbstractQueryBuilder.PARAMETER, value == null ? AbstractQueryBuilder.VALUE : AbstractQueryBuilder.CONCEPT_ID, value == null ? value : conceptId};
//      String expected = new MessageFormat(message).format(args);
//      assertEquals(expected, e.getMessage());
//    }
//  }

  private void assertEmptyMessageException(SearchRequest searchRequest, String messageParam) {
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      String message = AbstractQueryBuilder.EMPTY_MESSAGE;
      Object[] args = new Object[] {messageParam};
      String expected = new MessageFormat(message).format(args);
      assertEquals(expected, bre.getMessage());
    }
  }

  private Criteria createCriteriaParent(String type, String subtype, String code) {
    return new Criteria().parentId(0).type(type).subtype(subtype)
      .code(code).name("Cholera").group(true).selectable(true)
      .count("28").path("1.2");
  }

  private Criteria createCriteriaChild(String type, String subtype, long parentId, String code, String domain, String conceptId) {
    return new Criteria().parentId(parentId).type(type).subtype(subtype)
      .code(code).name("Cholera").group(false).selectable(true)
      .count("16").domainId(domain).conceptId(conceptId).path("1.2." + parentId);
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
