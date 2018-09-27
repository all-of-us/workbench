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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.*;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({QueryBuilderFactory.class, BigQueryService.class, CohortBuilderController.class,
  ParticipantCounter.class, DomainLookupService.class, CohortQueryBuilder.class,
  TestJpaConfig.class, CdrVersionService.class})
@MockBean({FireCloudService.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerBQTest extends BigQueryBaseTest {

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
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //demo no search parameters
    searchRequest = createSearchRequests(TreeType.DEMO.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //demo no attributes
    Criteria demoAge =
      createCriteriaChild(TreeType.DEMO.name(), TreeSubType.AGE.name(), 0, null, null, null);
    SearchParameter demo = createSearchParameter(demoAge, null);
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, ATTRIBUTES);

    //demo no attribute operands
    Attribute attr = new Attribute().operator(Operator.BETWEEN);
    demo.attributes(Arrays.asList(attr));
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, OPERANDS);

    //drug no search parameters
    searchRequest = createSearchRequests(TreeType.DRUG.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //measurement no search parameters
    searchRequest = createSearchRequests(TreeType.MEAS.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //measurement no attributes
    Criteria meas =
      createCriteriaChild(TreeType.MEAS.name(), TreeSubType.LAB.name(), 0, null, null, "1");
    SearchParameter measParam = createSearchParameter(meas, null);
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, ATTRIBUTES);

    //measurement no attribute operands
    measParam.attributes(Arrays.asList(new Attribute().operator(Operator.IN).name("name")));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, OPERANDS);

    //pm no search parameters
    searchRequest = createSearchRequests(TreeType.PM.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //pm no attributes
    Criteria pm =
      createCriteriaChild(TreeType.PM.name(), TreeSubType.HEIGHT.name(), 0, null, null, null);
    SearchParameter pmParam = createSearchParameter(pm, null);
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, ATTRIBUTES);

    //pm no attribute operands
    pmParam.attributes(Arrays.asList(new Attribute().operator(Operator.IN).name("name")));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, OPERANDS);

    //visit no search parameters
    searchRequest = createSearchRequests(TreeType.VISIT.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);
  }

  @Test
  public void countSubjectsNotValidMessageException() throws Exception {
    //icd9 no type
    Criteria icd9ConditionChild =
      createCriteriaChild(null, null, 0, "001", null, null);
    SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
    SearchRequest searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, icd9.getType());

    //icd9 bad type
    icd9.type(TreeType.VISIT.name());
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, icd9.getType());

    //icd9 no subtype
    icd9.type(TreeType.ICD9.name());
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, SUBTYPE, icd9.getSubtype());

    //icd9 bad subtype
    icd9.subtype("badsubtype");
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, SUBTYPE, icd9.getSubtype());

    //icd9 no domain
    icd9.subtype(TreeSubType.CM.name());
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, DOMAIN, icd9.getDomain());

    //icd9 bad domain
    icd9.domain("baddomain");
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, DOMAIN, icd9.getDomain());

    //icd9 child no concept id
    icd9.domain(DomainType.CONDITION.name());
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CONCEPT_ID, icd9.getConceptId());

    //icd9 parent no code
    icd9.group(true).value(null);
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CODE, icd9.getValue());

    //icd9 parent empty code
    icd9.group(true).value("");
    searchRequest = createSearchRequests(TreeType.ICD9.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CODE, icd9.getValue());

    //demo no type
    Criteria demo =
      createCriteriaChild(null, null, 0, null, null, null);
    SearchParameter demoParam = createSearchParameter(demo, null);
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, demoParam.getType());

    //demo bad type
    demoParam.type(TreeType.VISIT.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, demoParam.getType());

    //demo no subtype
    demoParam.type(TreeType.DEMO.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, SUBTYPE, demoParam.getSubtype());

    //demo bad subtype
    demoParam.subtype(TreeSubType.HEIGHT.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, SUBTYPE, demoParam.getSubtype());

    //demo no concept id for gender
    demoParam.subtype(TreeSubType.GEN.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CONCEPT_ID, demoParam.getConceptId());

    //demo no concept id for gender
    Attribute age = new Attribute().name("Age");
    demoParam.subtype(TreeSubType.AGE.name()).attributes(Arrays.asList(age));
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      ATTRIBUTE, OPERATOR, age.getOperator());

    //drug no type
    Criteria drug =
      createCriteriaChild(null, null, 0, null, null, null);
    SearchParameter drugParam = createSearchParameter(drug, null);
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, drugParam.getType());

    //drug bad type
    drugParam.type("blah");
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, drugParam.getType());

    //drug no concept id
    drugParam.type(TreeType.DRUG.name());
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CONCEPT_ID, drugParam.getConceptId());

    //meas no type
    Criteria meas =
      createCriteriaChild(null, null, 0, null, null, null);
    Attribute measAttr = new Attribute();
    SearchParameter measParam = createSearchParameter(meas, null).attributes(Arrays.asList(measAttr));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, measParam.getType());

    //meas bad type
    measParam.type("blah");
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, measParam.getType());

    //meas no concept id
    measParam.type(TreeType.MEAS.name());
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CONCEPT_ID, measParam.getConceptId());

    //meas no attr name
    measParam.conceptId(1L);
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      ATTRIBUTE, NAME, measAttr.getName());

    //meas no operator
    measParam.attributes(Arrays.asList(measAttr.name("name")));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      ATTRIBUTE, OPERATOR, measAttr.getOperator());

    //pm no type
    Criteria pm =
      createCriteriaChild(null, null, 0, null, null, null);
    SearchParameter pmParam = createSearchParameter(pm, null);
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, pmParam.getType());

    //pm bad type
    pmParam.type("blah");
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, pmParam.getType());

    //pm no subtype
    pmParam.type(TreeType.PM.name());
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, SUBTYPE, pmParam.getSubtype());

    //pm bad subtype
    pmParam.subtype("blah");
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, SUBTYPE, pmParam.getSubtype());

    //pm no domain
    pmParam.subtype(TreeSubType.PREG.name());
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, DOMAIN, pmParam.getDomain());

    //pm bad domain
    pmParam.domain("blah");
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, DOMAIN, pmParam.getDomain());

    //pm no value
    pmParam.domain(DomainType.MEASUREMENT.toString());
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, VALUE, pmParam.getValue());

    //pm value not a number
    pmParam.value("z");
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, VALUE, pmParam.getValue());

    //pm no concept id
    pmParam.value("10");
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CONCEPT_ID, pmParam.getConceptId());

    //pm no attribute name
    Attribute pmAttr = new Attribute();
    pmParam.subtype(TreeSubType.HEIGHT.name()).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      ATTRIBUTE, NAME, pmAttr.getName());

    //pm no operator
    pmAttr.name("HEIGHT");
    pmParam.attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      ATTRIBUTE, OPERATOR, pmAttr.getOperator());

    //pm no attr concept id
    pmAttr.name("HEIGHT").operator(Operator.EQUAL).operands(Arrays.asList("1"));
    pmParam.attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      ATTRIBUTE, CONCEPT_ID, pmAttr.getConceptId());

    //visit no type
    Criteria visit =
      createCriteriaChild(null, null, 0, null, null, null);
    SearchParameter visitParam = createSearchParameter(visit, null);
    searchRequest = createSearchRequests(TreeType.VISIT.name(), Arrays.asList(visitParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, visitParam.getType());

    //visit bad type
    visitParam.type("blah");
    searchRequest = createSearchRequests(TreeType.VISIT.name(), Arrays.asList(visitParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, TYPE, visitParam.getType());

    //visit no concept id
    visitParam.type(TreeType.VISIT.name());
    searchRequest = createSearchRequests(TreeType.VISIT.name(), Arrays.asList(visitParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE,
      PARAMETER, CONCEPT_ID, visitParam.getConceptId());
  }

  @Test
  public void countSubjectsOneOperandMessageException() throws Exception {
    //demo operands not one
    Attribute demoAttr = new Attribute().name("Age").operator(Operator.EQUAL).operands(Arrays.asList("1", "2"));
    Criteria demo =
      createCriteriaChild(TreeType.DEMO.name(), TreeSubType.AGE.name(), 0, null, null, null);
    SearchParameter demoParam = createSearchParameter(demo, null).attributes(Arrays.asList(demoAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, ONE_OPERAND_MESSAGE, ATTRIBUTE, demoAttr.getName(),
      operatorText.get(demoAttr.getOperator()));

    //pm operands not one
    Attribute pmAttr = new Attribute().name("Height").operator(Operator.EQUAL).operands(Arrays.asList("1", "2")).conceptId(1L);
    Criteria pm =
      createCriteriaChild(TreeType.PM.name(), TreeSubType.HEIGHT.name(), 0, null, null, null);
    SearchParameter pmParam = createSearchParameter(pm, null).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, ONE_OPERAND_MESSAGE, ATTRIBUTE, pmAttr.getName(),
      operatorText.get(pmAttr.getOperator()));

    //modifier operands not one
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.EQUAL).operands(Arrays.asList("1", "2"));
    Criteria drug =
      createCriteriaChild(TreeType.DRUG.name(), TreeSubType.ATC.name(), 0, null, null, "1");
    SearchParameter drugParam = createSearchParameter(drug, null);
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, ONE_OPERAND_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()),
      operatorText.get(pmAttr.getOperator()));
  }

  @Test
  public void countSubjectsTwoOperandMessageException() throws Exception {
    //demo operands not two
    Attribute demoAttr = new Attribute().name("Age").operator(Operator.BETWEEN).operands(Arrays.asList("1"));
    Criteria demo =
      createCriteriaChild(TreeType.DEMO.name(), TreeSubType.AGE.name(), 0, null, null, null);
    SearchParameter demoParam = createSearchParameter(demo, null).attributes(Arrays.asList(demoAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, ATTRIBUTE,
      demoAttr.getName(), operatorText.get(demoAttr.getOperator()));

    //pm operands not two
    Attribute pmAttr = new Attribute().name("Height").operator(Operator.BETWEEN).operands(Arrays.asList("1")).conceptId(1L);
    Criteria pm =
      createCriteriaChild(TreeType.PM.name(), TreeSubType.HEIGHT.name(), 0, null, null, null);
    SearchParameter pmParam = createSearchParameter(pm, null).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, ATTRIBUTE,
      pmAttr.getName(), operatorText.get(pmAttr.getOperator()));

    //meas operands not two
    Attribute measAttr = new Attribute().name("Meas").operator(Operator.BETWEEN).operands(Arrays.asList("1"));
    Criteria meas =
      createCriteriaChild(TreeType.MEAS.name(), TreeSubType.LAB.name(), 0, null, null, "1");
    SearchParameter measParam = createSearchParameter(meas, null).attributes(Arrays.asList(measAttr));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, ATTRIBUTE,
      measAttr.getName(), operatorText.get(measAttr.getOperator()));

    //modifier operands not two
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.BETWEEN).operands(Arrays.asList("1"));
    Criteria drug =
      createCriteriaChild(TreeType.DRUG.name(), TreeSubType.ATC.name(), 0, null, null, "1");
    SearchParameter drugParam = createSearchParameter(drug, null);
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()), operatorText.get(modifier.getOperator()));
  }

  @Test
  public void countSubjectsOperandsNumericMessageException() throws Exception {
    //demo operands not a number
    Attribute demoAttr = new Attribute().name("Age").operator(Operator.EQUAL).operands(Arrays.asList("z"));
    Criteria demo =
      createCriteriaChild(TreeType.DEMO.name(), TreeSubType.AGE.name(), 0, null, null, null);
    SearchParameter demoParam = createSearchParameter(demo, null).attributes(Arrays.asList(demoAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE,
      demoAttr.getName());

    //pm operands not a number
    Attribute pmAttr = new Attribute().name("Height").operator(Operator.EQUAL).operands(Arrays.asList("z")).conceptId(1L);
    Criteria pm =
      createCriteriaChild(TreeType.PM.name(), TreeSubType.HEIGHT.name(), 0, null, null, null);
    SearchParameter pmParam = createSearchParameter(pm, null).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE,
      pmAttr.getName());

    //meas operands not a number
    Attribute measAttr = new Attribute().name("Meas").operator(Operator.EQUAL).operands(Arrays.asList("z"));
    Criteria meas =
      createCriteriaChild(TreeType.MEAS.name(), TreeSubType.LAB.name(), 0, null, null, "1");
    SearchParameter measParam = createSearchParameter(meas, null).attributes(Arrays.asList(measAttr));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE,
      measAttr.getName());

    //encounters operands not a number
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("z"));
    Criteria drug =
      createCriteriaChild(TreeType.DRUG.name(), TreeSubType.ATC.name(), 0, null, null, "1");
    SearchParameter drugParam = createSearchParameter(drug, null);
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()));

    //age at event operands not a number
    modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.EQUAL).operands(Arrays.asList("z"));
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()));
  }

  @Test
  public void countSubjectsOneModifierMessageException() throws Exception {
    Modifier modifier1 = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    Modifier modifier2 = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    Criteria drug =
      createCriteriaChild(TreeType.DRUG.name(), TreeSubType.ATC.name(), 0, null, null, "1");
    SearchParameter drugParam = createSearchParameter(drug, null);
    SearchRequest searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier1, modifier2));
    assertMessageException(searchRequest, ONE_MODIFIER_MESSAGE, modifierText.get(modifier1.getName()));
  }

  @Test
  public void countSubjectsDateModifierMessageException() throws Exception {
    Modifier modifier = new Modifier().name(ModifierType.EVENT_DATE).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    Criteria drug =
      createCriteriaChild(TreeType.DRUG.name(), TreeSubType.ATC.name(), 0, null, null, "1");
    SearchParameter drugParam = createSearchParameter(drug, null);
    SearchRequest searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, DATE_MODIFIER_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()));
  }

  @Test
  public void countSubjectsNotInModifierMessageException() throws Exception {
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    Criteria drug =
      createCriteriaChild(TreeType.DRUG.name(), TreeSubType.ATC.name(), 0, null, null, "1");
    SearchParameter drugParam = createSearchParameter(drug, null);
    SearchRequest searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, NOT_IN_MODIFIER_MESSAGE, modifierText.get(modifier.getName()),
      operatorText.get(Operator.IN));
  }

  @Test
  public void countSubjectsAgeAndDecMessageException() throws Exception {
    Attribute demoAttr = new Attribute().name("Age").operator(Operator.EQUAL).operands(Arrays.asList("1"));
    Criteria demo =
      createCriteriaChild(TreeType.DEMO.name(), TreeSubType.AGE.name(), 0, null, null, null);
    Criteria dec =
      createCriteriaChild(TreeType.DEMO.name(), TreeSubType.DEC.name(), 0, null, null, null);
    SearchParameter demoParam = createSearchParameter(demo, null).attributes(Arrays.asList(demoAttr));
    SearchParameter decParam = createSearchParameter(dec, null);
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam, decParam), new ArrayList<>());
    assertMessageException(searchRequest, AGE_DEC_MESSAGE);
  }

  @Test
  public void countSubjectsCategoricalMessageException() throws Exception {
    Attribute measAttr = new Attribute().name(CATEGORICAL).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    Criteria meas =
      createCriteriaChild(TreeType.MEAS.name(), TreeSubType.LAB.name(), 0, null, null, "1");
    SearchParameter measParam = createSearchParameter(meas, null).attributes(Arrays.asList(measAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, CATEGORICAL_MESSAGE);
  }

  @Test
  public void countSubjectsBPMessageException() throws Exception {
    Attribute sysAttr = new Attribute().name("Systolic").operator(Operator.EQUAL).operands(Arrays.asList("90"));
    Attribute nonDiaAttr = new Attribute().name("NotDiastolic").operator(Operator.EQUAL).operands(Arrays.asList("90"));

    //only 1 attribute
    Criteria pm =
      createCriteriaChild(TreeType.PM.name(), TreeSubType.BP.name(), 0, null, null, null);
    SearchParameter pmParam = createSearchParameter(pm, null).attributes(Arrays.asList(sysAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, BP_TWO_ATTRIBUTE_MESSAGE);

    //2 but not systolic and diastolic
    pmParam.attributes(Arrays.asList(sysAttr, nonDiaAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, BP_TWO_ATTRIBUTE_MESSAGE);
  }

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
    lab.attributes(Arrays.asList(new Attribute().name(ANY)));
    Modifier modifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabTextAny() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(ANY)));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalAny() throws Exception {
    Criteria labCriteria = new Criteria().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId("3");
    SearchParameter lab = createSearchParameter(labCriteria, null);
    lab.attributes(Arrays.asList(new Attribute().name(ANY)));
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
    lab.attributes(Arrays.asList(new Attribute().name(ANY)));
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
    lab.attributes(Arrays.asList(new Attribute().name(ANY)));
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
    lab.attributes(Arrays.asList(new Attribute().name(ANY)));
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
    Attribute labText = new Attribute().name(ANY);
    Attribute labNumerical = new Attribute().name(ANY);
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
      new Attribute().name(ANY).operands(new ArrayList<>()).conceptId(903118L),
      new Attribute().name(ANY).operands(new ArrayList<>()).conceptId(903115L)
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
    SearchParameter hrSearchParameter = createPMSearchCriteriaWithAttributes(TreeType.PM.name(), TreeSubType.HR_DETAIL.toString(), "Heart Rate Detail", hrAttributes);
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

  private void assertMessageException(SearchRequest searchRequest, String message, Object... messageParams) {
    try {
      controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      String expected = new MessageFormat(message).format(messageParams);
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
      .count("16").domainId(DomainType.MEASUREMENT.name());
    SearchParameter searchParameter = createSearchParameter(criteria, null);
    searchParameter.attributes(attributes);
    return searchParameter;
  }

  private SearchParameter createPMSearchCriteria(String type, String subtype, String criteriaName, String conceptId, String code) {
    Criteria criteria = new Criteria().type(type).subtype(subtype)
      .name(criteriaName).group(false).selectable(true)
      .count("16").domainId(DomainType.MEASUREMENT.name()).conceptId(conceptId);
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
