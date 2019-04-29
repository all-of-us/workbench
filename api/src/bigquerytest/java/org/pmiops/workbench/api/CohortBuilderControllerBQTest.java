package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
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
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.TemporalQueryBuilder;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.inject.Provider;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.*;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({QueryBuilderFactory.class, BigQueryService.class, CloudStorageServiceImpl.class,
  ParticipantCounter.class, CohortQueryBuilder.class,
  TestJpaConfig.class, CdrVersionService.class, TemporalQueryBuilder.class})
@MockBean({FireCloudService.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerBQTest extends BigQueryBaseTest {

  private CohortBuilderController controller;

  private CdrVersion cdrVersion;

  @Autowired
  private BigQueryService bigQueryService;

  @Autowired
  private CloudStorageService cloudStorageService;

  @Autowired
  private ParticipantCounter participantCounter;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private CriteriaDao criteriaDao;

  @Autowired
  private CBCriteriaDao cbCriteriaDao;

  @Autowired
  private CdrVersionService cdrVersionService;

  @Autowired
  private CriteriaAttributeDao criteriaAttributeDao;

  @Autowired
  private CBCriteriaAttributeDao cbCriteriaAttributeDao;

  @Autowired
  private TestWorkbenchConfig testWorkbenchConfig;

  @Mock
  private Provider<WorkbenchConfig> configProvider;

  @Mock
  private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;


  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
      "person",
      "death",
      "criteria",
      "criteria_ancestor",
      "search_person",
      "search_all_domains");
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

    ElasticSearchService elasticSearchService =
        new ElasticSearchService(criteriaDao, cloudStorageService, configProvider);

    controller = new CohortBuilderController(bigQueryService, participantCounter,
      criteriaDao, cbCriteriaDao, criteriaAttributeDao, cbCriteriaAttributeDao,
      cdrVersionDao, genderRaceEthnicityConceptProvider, cdrVersionService,
      elasticSearchService, configProvider);

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
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //procedure no search parameters
    searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //demo no search parameters
    searchRequest = createSearchRequests(TreeType.DEMO.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //demo no attributes
    SearchParameter demo = new SearchParameter().type(TreeType.DEMO.name()).subtype(TreeSubType.AGE.name()).group(false);
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

    //measurement no attribute operands
    SearchParameter measParam = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(1L);
    measParam.attributes(Arrays.asList(new Attribute().operator(Operator.IN).name(AttrName.NUM)));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, OPERANDS);

    //pm no search parameters
    searchRequest = createSearchRequests(TreeType.PM.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //pm no attribute operands
    SearchParameter pmParam = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HEIGHT.name()).group(false).conceptId(1L);
    pmParam.attributes(Arrays.asList(new Attribute().operator(Operator.IN).name(AttrName.NUM)));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, OPERANDS);

    //visit no search parameters
    searchRequest = createSearchRequests(TreeType.VISIT.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);

    //ppi no search parameters
    searchRequest = createSearchRequests(TreeType.PPI.name(), new ArrayList<>(), new ArrayList<>());
    assertMessageException(searchRequest, EMPTY_MESSAGE, PARAMETERS);
  }

  @Test
  public void countSubjectsNotValidMessageException() throws Exception {
    //icd9 no type
    SearchParameter icd9 = new SearchParameter().value("001.1");
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, icd9.getType());

    //icd9 bad type
    icd9.type(TreeType.VISIT.name());
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, icd9.getType());

    //icd9 no subtype
    icd9.type(TreeType.ICD9.name());
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, icd9.getSubtype());

    //icd9 bad subtype
    icd9.subtype("badsubtype");
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, icd9.getSubtype());

    //icd9 child no concept id
    icd9.subtype(TreeSubType.CM.name()).group(false);
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, icd9.getConceptId());

    //icd9 parent no code
    icd9.group(true).value(null);
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CODE, icd9.getValue());

    //icd9 parent empty code
    icd9.group(true).value("");
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CODE, icd9.getValue());

    //snomed no type
    SearchParameter snomed = new SearchParameter();
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(snomed), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, snomed.getType());

    //snomed bad type
    snomed.type(TreeType.VISIT.name());
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(snomed), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, snomed.getType());

    //snomed child no concept id
    snomed.type(TreeType.SNOMED.name()).subtype(TreeSubType.CM.name()).group(false);
    searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(snomed), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, snomed.getConceptId());

    //demo no type
    SearchParameter demoParam = new SearchParameter();
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, demoParam.getType());

    //demo bad type
    demoParam.type(TreeType.VISIT.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, demoParam.getType());

    //demo no subtype
    demoParam.type(TreeType.DEMO.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, demoParam.getSubtype());

    //demo bad subtype
    demoParam.subtype(TreeSubType.HEIGHT.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, demoParam.getSubtype());

    //demo no concept id for gender
    demoParam.subtype(TreeSubType.GEN.name());
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, demoParam.getConceptId());

    //demo no concept id for gender
    Attribute age = new Attribute().name(AttrName.AGE);
    demoParam.subtype(TreeSubType.AGE.name()).attributes(Arrays.asList(age));
    searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, age.getOperator());

    //drug no type
    SearchParameter drugParam = new SearchParameter();
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, drugParam.getType());

    //drug bad type
    drugParam.type("blah");
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, drugParam.getType());

    //drug no concept id
    drugParam.type(TreeType.DRUG.name());
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, drugParam.getConceptId());

    //meas no type
    Attribute measAttr = new Attribute();
    SearchParameter measParam = new SearchParameter().attributes(Arrays.asList(measAttr));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, measParam.getType());

    //meas bad type
    measParam.type("blah");
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, measParam.getType());

    //meas no concept id
    measParam.type(TreeType.MEAS.name());
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, measParam.getConceptId());

    //meas no attr name
    measParam.conceptId(1L);
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, ATTRIBUTE, NAME, measAttr.getName());

    //meas no operator
    measParam.attributes(Arrays.asList(measAttr.name(AttrName.NUM)));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, measAttr.getOperator());

    //pm no type
    SearchParameter pmParam = new SearchParameter();
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, pmParam.getType());

    //pm bad type
    pmParam.type("blah");
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, pmParam.getType());

    //pm no subtype
    pmParam.type(TreeType.PM.name());
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, pmParam.getSubtype());

    //pm bad subtype
    pmParam.subtype("blah");
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, SUBTYPE, pmParam.getSubtype());

    //pm no attribute name
    Attribute pmAttr = new Attribute();
    pmParam.subtype(TreeSubType.HEIGHT.name()).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, pmParam.getConceptId());

    //pm no operator
    pmParam.conceptId(1L);
    pmAttr.name(AttrName.NUM);
    pmParam.attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, pmAttr.getOperator());

    //visit no type
    SearchParameter visitParam = new SearchParameter();
    searchRequest = createSearchRequests(TreeType.VISIT.name(), Arrays.asList(visitParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, visitParam.getType());

    //visit bad type
    visitParam.type("blah");
    searchRequest = createSearchRequests(TreeType.VISIT.name(), Arrays.asList(visitParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, visitParam.getType());

    //visit no concept id
    visitParam.type(TreeType.VISIT.name());
    searchRequest = createSearchRequests(TreeType.VISIT.name(), Arrays.asList(visitParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, visitParam.getConceptId());

    //ppi no type
    SearchParameter ppiParam = new SearchParameter();
    searchRequest = createSearchRequests(TreeType.PPI.name(), Arrays.asList(ppiParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, ppiParam.getType());

    //ppi bad type
    ppiParam.type("blah");
    searchRequest = createSearchRequests(TreeType.PPI.name(), Arrays.asList(ppiParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, TYPE, ppiParam.getType());

    //ppi no concept id
    Attribute attr = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    ppiParam.attributes(Arrays.asList(attr));
    ppiParam.type(TreeType.PPI.name()).group(false);
    searchRequest = createSearchRequests(TreeType.PPI.name(), Arrays.asList(ppiParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, ppiParam.getConceptId());

    //ppi no attribute name
    attr = new Attribute().name(null).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    ppiParam.attributes(Arrays.asList(attr));
    ppiParam.setConceptId(1L);
    searchRequest = createSearchRequests(TreeType.PPI.name(), Arrays.asList(ppiParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, ATTRIBUTE, NAME, attr.getName());

    //ppi no operator
    attr = new Attribute().name(AttrName.NUM).operands(Arrays.asList("1"));
    ppiParam.attributes(Arrays.asList(attr));
    searchRequest = createSearchRequests(TreeType.PPI.name(), Arrays.asList(ppiParam), new ArrayList<>());
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, ATTRIBUTE, OPERATOR, null);
  }

  @Test
  public void temporalMessageExceptions() throws Exception {
    SearchParameter param1 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(1L);
    SearchParameter param2 = new SearchParameter()
      .type(TreeType.ICD10.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(9L);

    SearchGroupItem searchGroupItem1 = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(param1);
    SearchGroupItem searchGroupItem2 = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(param2);

    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(searchGroupItem1, searchGroupItem2))
      .temporal(true);
    SearchRequest searchRequest = new SearchRequest()
      .includes(Arrays.asList(temporalGroup));
    //temporal mention null
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, SEARCH_GROUP, MENTION, param1.getValue());

    //temporal mention invalid
    temporalGroup.setMention(null);
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, SEARCH_GROUP, MENTION, temporalGroup.getMention());

    //temporal time null
    temporalGroup.setMention(TemporalMention.ANY_MENTION);
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, SEARCH_GROUP, TIME, temporalGroup.getTime());

    //temporal time invalid
    temporalGroup.setTime(null);
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, SEARCH_GROUP, TIME, temporalGroup.getTime());

    //temporal timeValue null
    temporalGroup.setTime(TemporalTime.X_DAYS_AFTER);
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, SEARCH_GROUP, TIME_VALUE, temporalGroup.getTimeValue());

    //temporal group is null
    temporalGroup.setTime(TemporalTime.DURING_SAME_ENCOUNTER_AS);
    assertMessageException(searchRequest, NOT_VALID_MESSAGE, SEARCH_GROUP_ITEM, TEMPORAL_GROUP, searchGroupItem1.getTemporalGroup());

    //temporal group not valid
    searchGroupItem1.setTemporalGroup(2);
    searchGroupItem2.setTemporalGroup(3);
    assertMessageException(searchRequest, TEMPORAL_GROUP_MESSAGE);
  }

  @Test
  public void countSubjectsOneOperandMessageException() throws Exception {
    //demo operands not one
    Attribute demoAttr = new Attribute().name(AttrName.AGE).operator(Operator.EQUAL).operands(Arrays.asList("1", "2"));
    SearchParameter demoParam = new SearchParameter().type(TreeType.DEMO.name()).subtype(TreeSubType.AGE.name()).attributes(Arrays.asList(demoAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, ONE_OPERAND_MESSAGE, ATTRIBUTE, demoAttr.getName(),
      operatorText.get(demoAttr.getOperator()));

    //pm operands not one
    Attribute pmAttr = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("1", "2"));
    SearchParameter pmParam = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HEIGHT.name()).conceptId(1L).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, ONE_OPERAND_MESSAGE, ATTRIBUTE, pmAttr.getName(),
      operatorText.get(pmAttr.getOperator()));

    //modifier operands not one
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.EQUAL).operands(Arrays.asList("1", "2"));
    SearchParameter drugParam = new SearchParameter().type(TreeType.DRUG.name()).subtype(TreeSubType.ATC.name()).group(false).conceptId(1L);
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, ONE_OPERAND_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()),
      operatorText.get(pmAttr.getOperator()));
  }

  @Test
  public void countSubjectsTwoOperandMessageException() throws Exception {
    //demo operands not two
    Attribute demoAttr = new Attribute().name(AttrName.AGE).operator(Operator.BETWEEN).operands(Arrays.asList("1"));
    SearchParameter demoParam = new SearchParameter().type(TreeType.DEMO.name()).subtype(TreeSubType.AGE.name()).attributes(Arrays.asList(demoAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, ATTRIBUTE,
      demoAttr.getName(), operatorText.get(demoAttr.getOperator()));

    //pm operands not two
    Attribute pmAttr = new Attribute().name(AttrName.NUM).operator(Operator.BETWEEN).operands(Arrays.asList("1")).conceptId(1L);
    SearchParameter pmParam = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HEIGHT.name()).conceptId(1L).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, ATTRIBUTE,
      pmAttr.getName(), operatorText.get(pmAttr.getOperator()));

    //meas operands not two
    Attribute measAttr = new Attribute().name(AttrName.NUM).operator(Operator.BETWEEN).operands(Arrays.asList("1"));
    SearchParameter measParam = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).conceptId(1L).attributes(Arrays.asList(measAttr));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, ATTRIBUTE,
      measAttr.getName(), operatorText.get(measAttr.getOperator()));

    //modifier operands not two
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.BETWEEN).operands(Arrays.asList("1"));
    SearchParameter drugParam = new SearchParameter().type(TreeType.DRUG.name()).group(false).subtype(TreeSubType.ATC.name()).conceptId(1L);
    searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, TWO_OPERAND_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()), operatorText.get(modifier.getOperator()));
  }

  @Test
  public void countSubjectsOperandsNumericMessageException() throws Exception {
    //demo operands not a number
    Attribute demoAttr = new Attribute().name(AttrName.AGE).operator(Operator.EQUAL).operands(Arrays.asList("z"));
    SearchParameter demoParam = new SearchParameter().type(TreeType.DEMO.name()).subtype(TreeSubType.AGE.name()).group(false).attributes(Arrays.asList(demoAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam), new ArrayList<>());
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE,
      demoAttr.getName());

    //pm operands not a number
    Attribute pmAttr = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("z")).conceptId(1L);
    SearchParameter pmParam = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HEIGHT.name()).group(false).conceptId(1L).attributes(Arrays.asList(pmAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE,
      pmAttr.getName());

    //meas operands not a number
    Attribute measAttr = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("z"));
    SearchParameter measParam = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(1L).attributes(Arrays.asList(measAttr));
    searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, OPERANDS_NUMERIC_MESSAGE, ATTRIBUTE,
      measAttr.getName());

    //encounters operands not a number
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("z"));
    SearchParameter drugParam = new SearchParameter().type(TreeType.DRUG.name()).subtype(TreeSubType.ATC.name()).group(false).conceptId(1L);
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
    SearchParameter drugParam = new SearchParameter().type(TreeType.DRUG.name()).subtype(TreeSubType.ATC.name()).group(false).conceptId(1L);
    SearchRequest searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier1, modifier2));
    assertMessageException(searchRequest, ONE_MODIFIER_MESSAGE, modifierText.get(modifier1.getName()));
  }

  @Test
  public void countSubjectsDateModifierMessageException() throws Exception {
    Modifier modifier = new Modifier().name(ModifierType.EVENT_DATE).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    SearchParameter drugParam = new SearchParameter().type(TreeType.DRUG.name()).subtype(TreeSubType.ATC.name()).group(false).conceptId(1L);
    SearchRequest searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, DATE_MODIFIER_MESSAGE, MODIFIER,
      modifierText.get(modifier.getName()));
  }

  @Test
  public void countSubjectsNotInModifierMessageException() throws Exception {
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    SearchParameter drugParam = new SearchParameter().type(TreeType.DRUG.name()).subtype(TreeSubType.ATC.name()).group(false).conceptId(1L);
    SearchRequest searchRequest = createSearchRequests(TreeType.DRUG.name(), Arrays.asList(drugParam), Arrays.asList(modifier));
    assertMessageException(searchRequest, NOT_IN_MODIFIER_MESSAGE, modifierText.get(modifier.getName()),
      operatorText.get(Operator.IN));
  }

  @Test
  public void countSubjectsAgeAndDecMessageException() throws Exception {
    Attribute demoAttr = new Attribute().name(AttrName.AGE).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    SearchParameter demoParam = new SearchParameter().type(TreeType.DEMO.name()).subtype(TreeSubType.AGE.name()).group(false).attributes(Arrays.asList(demoAttr));
    SearchParameter decParam = new SearchParameter().type(TreeType.DEMO.name()).subtype(TreeSubType.DEC.name()).group(false);
    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoParam, decParam), new ArrayList<>());
    assertMessageException(searchRequest, AGE_DEC_MESSAGE);
  }

  @Test
  public void countSubjectsCategoricalMessageException() throws Exception {
    Attribute measAttr = new Attribute().name(AttrName.CAT).operator(Operator.EQUAL).operands(Arrays.asList("1"));
    SearchParameter measParam = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(1L).attributes(Arrays.asList(measAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.MEAS.name(), Arrays.asList(measParam), new ArrayList<>());
    assertMessageException(searchRequest, CATEGORICAL_MESSAGE);
  }

  @Test
  public void countSubjectsBPMessageException() throws Exception {
    Attribute sysAttr = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("90")).conceptId(903118L);
    Attribute nonDiaAttr = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("90")).conceptId(1L);

    //only 1 attribute
    SearchParameter pmParam = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.BP.name()).group(false).attributes(Arrays.asList(sysAttr));
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, BP_TWO_ATTRIBUTE_MESSAGE);

    //2 but not systolic and diastolic
    pmParam.attributes(Arrays.asList(sysAttr, nonDiaAttr));
    searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pmParam), new ArrayList<>());
    assertMessageException(searchRequest, BP_TWO_ATTRIBUTE_MESSAGE);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChild() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfICD9WithModifiersOrSnomed5DaysAfterICD10WithModifiers() throws Exception {
    Modifier ageModifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));
    Modifier visitsModifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));

    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(1L);
    SearchParameter icd10 = new SearchParameter()
      .type(TreeType.ICD10.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(9L);
    SearchParameter snomed = new SearchParameter()
      .type(TreeType.SNOMED.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(4L);

    SearchGroupItem icd9SGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(icd9)
      .temporalGroup(0)
      .addModifiersItem(ageModifier);
    SearchGroupItem icd10SGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(icd10)
      .temporalGroup(1)
      .addModifiersItem(visitsModifier);
    SearchGroupItem snomedSGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(snomed)
      .temporalGroup(0);

    //First Mention Of (ICD9 w/modifiers or Snomed) 5 Days After ICD10 w/modifiers
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(icd9SGI, snomedSGI, icd10SGI))
      .temporal(true)
      .mention(TemporalMention.FIRST_MENTION)
      .time(TemporalTime.X_DAYS_AFTER)
      .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    //matches icd9SGI in group 0 and icd10SGI in group 1
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfICD9WithOccurrencesOrSnomed5DaysAfterICD10() throws Exception {
    Modifier occurrencesModifier = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("2"));

    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(1L);
    SearchParameter icd10 = new SearchParameter()
      .type(TreeType.ICD10.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(9L);
    SearchParameter snomed = new SearchParameter()
      .type(TreeType.SNOMED.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(4L);

    SearchGroupItem icd9SGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(icd9)
      .temporalGroup(0)
      .addModifiersItem(occurrencesModifier);
    SearchGroupItem icd10SGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(icd10)
      .temporalGroup(1);
    SearchGroupItem snomedSGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(snomed)
      .temporalGroup(0);

    //First Mention Of (ICD9 w/modifiers or Snomed) 5 Days After ICD10 w/modifiers
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(icd9SGI, snomedSGI, icd10SGI))
      .temporal(true)
      .mention(TemporalMention.FIRST_MENTION)
      .time(TemporalTime.X_DAYS_AFTER)
      .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    //matches icd9SGI in group 0 and icd10SGI in group 1
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfDrug5DaysBeforeICD10WithModifiers() throws Exception {
    Modifier visitsModifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));

    SearchParameter drug = new SearchParameter()
      .type(TreeType.DRUG.name())
      .subtype(TreeSubType.ATC.name())
      .group(true)
      .conceptId(21600932L);
    SearchParameter icd10 = new SearchParameter()
      .type(TreeType.ICD10.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(9L);

    SearchGroupItem drugSGI = new SearchGroupItem()
      .type(TreeType.DRUG.name())
      .addSearchParametersItem(drug)
      .temporalGroup(0);
    SearchGroupItem icd10SGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(icd10)
      .temporalGroup(1)
      .addModifiersItem(visitsModifier);

    //First Mention Of Drug 5 Days Before ICD10 w/modifiers
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(drugSGI, icd10SGI))
      .temporal(true)
      .mention(TemporalMention.FIRST_MENTION)
      .time(TemporalTime.X_DAYS_BEFORE)
      .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void anyMentionOfICD9Parent5DaysAfterICD10Child() throws Exception {
    Modifier visitsModifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));

    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(true)
      .value("001");
    SearchParameter icd10 = new SearchParameter()
      .type(TreeType.ICD10.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(9L);

    SearchGroupItem icd9SGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(icd9)
      .temporalGroup(0)
      .addModifiersItem(visitsModifier);
    SearchGroupItem icd10SGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(icd10)
      .temporalGroup(1);

    //Any Mention Of ICD9 5 Days After ICD10
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(icd9SGI, icd10SGI))
      .temporal(true)
      .mention(TemporalMention.ANY_MENTION)
      .time(TemporalTime.X_DAYS_AFTER)
      .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void anyMentionOfCPTWithIn5DaysOfVisit() throws Exception {
    SearchParameter cpt = new SearchParameter()
      .type(TreeType.CPT.name())
      .subtype(TreeSubType.CPT4.name())
      .group(false)
      .conceptId(10L);
    SearchParameter visit = new SearchParameter()
      .type(TreeType.VISIT.name())
      .group(false)
      .conceptId(1L);

    SearchGroupItem cptSGI = new SearchGroupItem()
      .type(TreeType.CONDITION.name())
      .addSearchParametersItem(cpt)
      .temporalGroup(0);
    SearchGroupItem visitSGI = new SearchGroupItem()
      .type(TreeType.VISIT.name())
      .addSearchParametersItem(visit)
      .temporalGroup(1);

    //Any Mention Of ICD10 Parent within 5 Days of visit
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(visitSGI, cptSGI))
      .temporal(true)
      .mention(TemporalMention.ANY_MENTION)
      .time(TemporalTime.WITHIN_X_DAYS_OF)
      .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void firstMentionOfDrugDuringSameEncounterAsMeasurement() throws Exception {
    SearchParameter drug = new SearchParameter()
      .type(TreeType.DRUG.name())
      .subtype(TreeSubType.ATC.name())
      .group(false)
      .conceptId(11L);
    SearchParameter measurement = new SearchParameter()
      .type(TreeType.MEAS.name())
      .subtype(TreeSubType.LAB.name())
      .group(false)
      .conceptId(3L);

    SearchGroupItem drugSGI = new SearchGroupItem()
      .type(TreeType.DRUG.name())
      .addSearchParametersItem(drug)
      .temporalGroup(0);
    SearchGroupItem measurementSGI = new SearchGroupItem()
      .type(TreeType.MEAS.name())
      .addSearchParametersItem(measurement)
      .temporalGroup(1);

    //First Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(drugSGI, measurementSGI))
      .temporal(true)
      .mention(TemporalMention.FIRST_MENTION)
      .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurement() throws Exception {
    SearchParameter drug = new SearchParameter()
      .type(TreeType.DRUG.name())
      .subtype(TreeSubType.ATC.name())
      .group(false)
      .conceptId(11L);
    SearchParameter measurement = new SearchParameter()
      .type(TreeType.MEAS.name())
      .subtype(TreeSubType.LAB.name())
      .group(false)
      .conceptId(3L);

    SearchGroupItem drugSGI = new SearchGroupItem()
      .type(TreeType.DRUG.name())
      .addSearchParametersItem(drug)
      .temporalGroup(0);
    SearchGroupItem measurementSGI = new SearchGroupItem()
      .type(TreeType.MEAS.name())
      .addSearchParametersItem(measurement)
      .temporalGroup(1);

    //Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(drugSGI, measurementSGI))
      .temporal(true)
      .mention(TemporalMention.LAST_MENTION)
      .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurementOrVisit() throws Exception {
    SearchParameter drug = new SearchParameter()
      .type(TreeType.DRUG.name())
      .subtype(TreeSubType.ATC.name())
      .group(false)
      .conceptId(11L);
    SearchParameter measurement = new SearchParameter()
      .type(TreeType.MEAS.name())
      .subtype(TreeSubType.LAB.name())
      .group(false)
      .conceptId(3L);
    SearchParameter visit = new SearchParameter()
      .type(TreeType.VISIT.name())
      .group(false)
      .conceptId(1L);

    SearchGroupItem drugSGI = new SearchGroupItem()
      .type(TreeType.DRUG.name())
      .addSearchParametersItem(drug)
      .temporalGroup(0);
    SearchGroupItem measurementSGI = new SearchGroupItem()
      .type(TreeType.MEAS.name())
      .addSearchParametersItem(measurement)
      .temporalGroup(1);
    SearchGroupItem visitSGI = new SearchGroupItem()
      .type(TreeType.VISIT.name())
      .addSearchParametersItem(visit)
      .temporalGroup(1);

    //Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(drugSGI, measurementSGI, visitSGI))
      .temporal(true)
      .mention(TemporalMention.LAST_MENTION)
      .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void lastMentionOfMeasurementOrVisit5DaysAfterDrug() throws Exception {
    SearchParameter measurement = new SearchParameter()
      .type(TreeType.MEAS.name())
      .subtype(TreeSubType.LAB.name())
      .group(false)
      .conceptId(3L);
    SearchParameter visit = new SearchParameter()
      .type(TreeType.VISIT.name())
      .group(false)
      .conceptId(1L);
    SearchParameter drug = new SearchParameter()
      .type(TreeType.DRUG.name())
      .subtype(TreeSubType.ATC.name())
      .group(true)
      .conceptId(21600932L);

    SearchGroupItem measurementSGI = new SearchGroupItem()
      .type(TreeType.MEAS.name())
      .addSearchParametersItem(measurement)
      .temporalGroup(0);
    SearchGroupItem visitSGI = new SearchGroupItem()
      .type(TreeType.VISIT.name())
      .addSearchParametersItem(visit)
      .temporalGroup(0);
    SearchGroupItem drugSGI = new SearchGroupItem()
      .type(TreeType.DRUG.name())
      .addSearchParametersItem(drug)
      .temporalGroup(1);

    //Last Mention Of Measurement or Visit 5 days after Drug
    SearchGroup temporalGroup = new SearchGroup()
      .items(Arrays.asList(drugSGI, measurementSGI, visitSGI))
      .temporal(true)
      .mention(TemporalMention.LAST_MENTION)
      .time(TemporalTime.X_DAYS_AFTER)
      .timeValue(5L);

    SearchRequest searchRequest = new SearchRequest().includes(Arrays.asList(temporalGroup));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildrenAgeAtEvent() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);
    SearchParameter icd9Proc = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.PROC.name())
      .group(false)
      .value("001.1")
      .conceptId(2L);

    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(),
      Arrays.asList(icd9, icd9Proc), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildrenEncounter() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);
    SearchParameter icd9Proc = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.PROC.name())
      .group(false)
      .value("001.1")
      .conceptId(2L);

    Modifier modifier = new Modifier()
      .name(ModifierType.ENCOUNTERS)
      .operator(Operator.IN)
      .operands(Arrays.asList("1"));

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(),
      Arrays.asList(icd9, icd9Proc), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventBetween() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);

    Modifier modifier = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.BETWEEN)
      .operands(Arrays.asList("37", "39"));

    SearchRequest searchRequest =
      createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), Arrays.asList(modifier));

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);

    Modifier modifier1 = new Modifier()
      .name(ModifierType.AGE_AT_EVENT)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("25"));
    Modifier modifier2 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.EQUAL)
      .operands(Arrays.asList("2"));

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(),
      Arrays.asList(icd9), Arrays.asList(modifier1, modifier2));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrencesAndEventDate() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);
    SearchParameter icd9Proc = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.PROC.name())
      .group(false)
      .value("001.1")
      .conceptId(2L);

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

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(),
      Arrays.asList(icd9,icd9Proc), Arrays.asList(modifier1, modifier2, modifier3));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildEventDate() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);

    Modifier modifier = new Modifier()
      .name(ModifierType.EVENT_DATE)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("2009-12-03"));

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(),
      Arrays.asList(icd9), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildOccurrences() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);

    Modifier modifier2 = new Modifier()
      .name(ModifierType.NUM_OF_OCCURRENCES)
      .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
      .operands(Arrays.asList("1"));

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(),
      Arrays.asList(icd9), Arrays.asList(modifier2));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ParentAndChild() throws Exception {
    SearchParameter icd9Child = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001.1")
      .conceptId(1L);
    SearchParameter icd9Parent = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(true)
      .value("001");

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9Child, icd9Parent), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceParent() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("001")
      .conceptId(1L);

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ProcedureOccurrenceChild() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.PROC.name())
      .group(false)
      .value("002.1")
      .conceptId(2L);

    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ProcedureOccurrenceParent() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.PROC.name())
      .group(true)
      .value("002");

    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9MeasurementChild() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .value("003.1")
      .conceptId(3L);

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9MeasurementParent() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(true)
      .value("003");

    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoGender() throws Exception {
    SearchParameter demo = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.GEN.name())
      .group(false)
      .conceptId(8507L);

    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoEthnicity() throws Exception {
    SearchParameter demo = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.ETH.name())
      .group(false)
      .conceptId(9898L);

    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoDec() throws Exception {
    SearchParameter demo = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.DEC.name())
      .group(false)
      .value("Deceased");

    SearchRequest searchRequest = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDemoAge() throws Exception {
    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    SearchParameter demo = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.AGE.name())
      .group(false);
    demo.attributes(Arrays.asList(new Attribute().name(AttrName.AGE).operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));
    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsDemoAgeBetween() throws Exception {
    SearchParameter demo = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.AGE.name())
      .group(false);
    demo.attributes(Arrays.asList(
      new Attribute().operator(Operator.BETWEEN).operands(Arrays.asList("15","99"))
    ));
    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demo), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsDemoGenderAndAge() throws Exception {
    SearchParameter demoGenderSearchParam = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.GEN.name())
      .group(false)
      .conceptId(8507L);

    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();
    SearchParameter demoAgeSearchParam = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.AGE.name())
      .group(false);
    demoAgeSearchParam.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));

    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoGenderSearchParam, demoAgeSearchParam), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsICD9AndDemo() throws Exception {
    SearchParameter icd9 = new SearchParameter()
      .type(TreeType.ICD9.name())
      .subtype(TreeSubType.CM.name())
      .group(false)
      .conceptId(3L)
      .value("003.1");

    SearchParameter demoGenderSearchParam = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.GEN.name())
      .group(false)
      .conceptId(8507L);

    SearchParameter demoAgeSearchParam = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.AGE.name())
      .group(false);

    DateTime birthDate = new DateTime(1980, 8, 01, 0, 0, 0, 0);
    DateTime now = new DateTime();
    Period period = new Period(birthDate, now);
    Integer age = period.getYears();

    demoAgeSearchParam.attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList(age.toString()))));

    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoGenderSearchParam, demoAgeSearchParam), new ArrayList<>());

    SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type(TreeType.CONDITION.name()).searchParameters(Arrays.asList(icd9)).modifiers(new ArrayList<>());

    searchRequests.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
  }

  @Test
  public void countSubjectsDemoExcluded() throws Exception {
    SearchParameter demoGenderSearchParam = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.GEN.name())
      .group(false)
      .conceptId(8507L);

    SearchParameter demoGenderSearchParamExclude = new SearchParameter()
      .type(TreeType.DEMO.name())
      .subtype(TreeSubType.GEN.name())
      .group(false)
      .conceptId(8507L);

    SearchGroupItem excludeSearchGroupItem = new SearchGroupItem().type(TreeType.DEMO.name())
      .searchParameters(Arrays.asList(demoGenderSearchParamExclude));
    SearchGroup excludeSearchGroup = new SearchGroup().addItemsItem(excludeSearchGroupItem);

    SearchRequest searchRequests = createSearchRequests(TreeType.DEMO.name(), Arrays.asList(demoGenderSearchParam), new ArrayList<>());
    searchRequests.getExcludes().add(excludeSearchGroup);

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 0);
  }

  @Test
  public void countSubjectsICD10ConditionOccurrenceChildEncounter() throws Exception {
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(false).conceptId(6L);
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd10), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ConditionOccurrenceChild() throws Exception {
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(false).conceptId(6L);
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildICD10ConditionOccurrenceChild() throws Exception {
    SearchParameter icd9P = new SearchParameter().type(TreeType.ICD9.name()).subtype(TreeSubType.CM.name()).group(true).value("001");
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(false).conceptId(6L);
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd9P, icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsICD10ConditionOccurrenceParent() throws Exception {
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(true).value("A");
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ProcedureOccurrenceChild() throws Exception {
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(false).conceptId(8L);
    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10ProcedureOccurrenceParent() throws Exception {
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.PCS.name()).group(true).value("16");
    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsICD10MeasurementChild() throws Exception {
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(false).conceptId(9L);
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsICD10MeasurementParent() throws Exception {
    SearchParameter icd10 = new SearchParameter().type(TreeType.ICD10.name()).subtype(TreeSubType.CM.name()).group(true).value("R92");
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(icd10), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsCPTProcedureOccurrence() throws Exception {
    SearchParameter cpt = new SearchParameter().type(TreeType.CPT.name()).subtype(TreeSubType.CPT4.name()).group(false).conceptId(4L);
    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTProcedureOccurrenceEncounter() throws Exception {
    SearchParameter cpt = new SearchParameter().type(TreeType.CPT.name()).subtype(TreeSubType.CPT4.name()).group(false).conceptId(4L);
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(cpt), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTObservation() throws Exception {
    SearchParameter cpt = new SearchParameter().type(TreeType.CPT.name()).subtype(TreeSubType.CPT4.name()).group(false).conceptId(5L);
    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTMeasurement() throws Exception {
    SearchParameter cpt = new SearchParameter().type(TreeType.CPT.name()).subtype(TreeSubType.CPT4.name()).group(false).conceptId(10L);
    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsConditionsSnomed() throws Exception {
    SearchParameter snomed = new SearchParameter().type(TreeType.SNOMED.name()).subtype(TreeSubType.CM.name()).group(false).conceptId(6L);
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(snomed), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsProceduresSnomed() throws Exception {
    SearchParameter snomed = new SearchParameter().type(TreeType.SNOMED.name()).subtype(TreeSubType.PCS.name()).group(false).conceptId(4L);
    SearchRequest searchRequest = createSearchRequests(TreeType.CONDITION.name(), Arrays.asList(snomed), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsCPTDrugExposure() throws Exception {
    SearchParameter cpt = new SearchParameter().type(TreeType.CPT.name()).subtype(TreeSubType.CPT4.name()).group(false).conceptId(11L);
    SearchRequest searchRequest = createSearchRequests(TreeType.PROCEDURE.name(), Arrays.asList(cpt), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsVisitChild() throws Exception {
    SearchParameter visit = new SearchParameter().type(TreeType.VISIT.name()).group(false).conceptId(10L);
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsVisitChildModifiers() throws Exception {
    SearchParameter visit = new SearchParameter().type(TreeType.VISIT.name()).group(false).conceptId(10L);
    Modifier modifier = new Modifier().name(ModifierType.NUM_OF_OCCURRENCES).operator(Operator.GREATER_THAN_OR_EQUAL_TO).operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(visit.getType(), Arrays.asList(visit), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsDrugChild() throws Exception {
    SearchParameter drug = new SearchParameter().type(TreeType.DRUG.name()).group(false).conceptId(11L);
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugParent() throws Exception {
    SearchParameter drug = new SearchParameter().type(TreeType.DRUG.name()).group(true).conceptId(21600932L);
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugParentAndChild() throws Exception {
    SearchParameter drugChild = new SearchParameter().type(TreeType.DRUG.name()).group(false).conceptId(11L);
    SearchParameter drugParent = new SearchParameter().type(TreeType.DRUG.name()).group(true).conceptId(21600932L);
    SearchRequest searchRequest = createSearchRequests(drugParent.getType(), Arrays.asList(drugParent,drugChild), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugChildEncounter() throws Exception {
    SearchParameter drug = new SearchParameter().type(TreeType.DRUG.name()).group(false).conceptId(11L);
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsDrugChildModifiers() throws Exception {
    SearchParameter drug = new SearchParameter().type(TreeType.DRUG.name()).group(false).conceptId(11L);
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.GREATER_THAN_OR_EQUAL_TO).operands(Arrays.asList("25"));
    SearchRequest searchRequest = createSearchRequests(drug.getType(), Arrays.asList(drug), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabTextAnyEncounter() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    Modifier modifier = new Modifier().name(ModifierType.ENCOUNTERS).operator(Operator.IN).operands(Arrays.asList("1"));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabTextAny() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalAny() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalBetween() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    lab.attributes(Arrays.asList(new Attribute().name(AttrName.NUM).operator(Operator.BETWEEN).operands(Arrays.asList("0", "1"))));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalAny() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabCategoricalIn() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    lab.attributes(Arrays.asList(new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("1"))));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothAny() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothNumericalAndCategorical() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    Attribute numerical = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("0.1"));
    Attribute categorical = new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("1", "2"));
    lab.attributes(Arrays.asList(numerical, categorical));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabBothNumericalAndCategoricalSpecificName() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    Attribute numerical = new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("0.1"));
    Attribute categorical = new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("1"));
    lab.attributes(Arrays.asList(numerical, categorical));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabNumericalAnyAgeAtEvent() throws Exception {
    SearchParameter lab = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    Modifier modifier = new Modifier().name(ModifierType.AGE_AT_EVENT).operator(Operator.GREATER_THAN_OR_EQUAL_TO).operands(Arrays.asList("25"));
    SearchRequest searchRequest = createSearchRequests(lab.getType(), Arrays.asList(lab), Arrays.asList(modifier));
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameter() throws Exception {
    SearchParameter lab1 = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(3L);
    SearchParameter lab2 = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(9L);
    SearchParameter lab3 = new SearchParameter().type(TreeType.MEAS.name()).subtype(TreeSubType.LAB.name()).group(false).conceptId(9L);
    Attribute labCategorical = new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("77"));
    lab3.attributes(Arrays.asList(labCategorical));
    SearchRequest searchRequest = createSearchRequests(lab1.getType(), Arrays.asList(lab1, lab2, lab3), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsBloodPressure() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L),
      new Attribute().name(AttrName.NUM).operator(Operator.BETWEEN).operands(Arrays.asList("60","80")).conceptId(903115L)
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.BP.name()).group(false).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(pm.getType(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureAny() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903118L),
      new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903115L)
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.BP.name()).group(false).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetailOrHeartRateIrr() throws Exception {
    List<Attribute> bpAttributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("90")).conceptId(903118L),
      new Attribute().name(AttrName.NUM).operator(Operator.BETWEEN).operands(Arrays.asList("60","80")).conceptId(903115L)
    );
    SearchParameter bpPm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.BP.name()).group(false).attributes(bpAttributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(bpPm), new ArrayList<>());

    List<Attribute> hrAttributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("71"))
    );
    SearchParameter hrPm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HR_DETAIL.toString()).group(false).conceptId(903126L).attributes(hrAttributes);
    SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type(TreeType.PM.name()).searchParameters(Arrays.asList(hrPm)).modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    List<Attribute> irrAttributes = Arrays.asList(
      new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("4262985"))
    );
    SearchParameter hrIrrPm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HR.name()).group(false).conceptId(1586218L).attributes(irrAttributes);
    SearchGroupItem heartRateIrrSearchGroupItem = new SearchGroupItem().type(TreeType.PM.name()).searchParameters(Arrays.asList(hrIrrPm)).modifiers(new ArrayList<>());

    searchRequest.getIncludes().get(0).addItemsItem(heartRateIrrSearchGroupItem);

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 3);
  }

  @Test
  public void countSubjectsHeartRateAny() throws Exception {
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HR_DETAIL.toString()).group(false).conceptId(1586218L);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsHeartRate() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.GREATER_THAN_OR_EQUAL_TO).operands(Arrays.asList("45"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HR_DETAIL.toString()).group(false).conceptId(1586218L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsHeight() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("168"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HEIGHT.toString()).group(false).conceptId(903133L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsWeight() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("201"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.WEIGHT.toString()).group(false).conceptId(903121L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectsBMI() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.LESS_THAN_OR_EQUAL_TO).operands(Arrays.asList("263"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.BMI.toString()).group(false).conceptId(903124L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWaistCircumference() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("31"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.WC.toString()).group(false).conceptId(903135L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectHipCircumference() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("33"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.HC.toString()).group(false).conceptId(903136L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectPregnant() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("45877994"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.PREG.toString()).group(false).conceptId(903120L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void countSubjectWheelChairUser() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("4023190"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.WHEEL.toString()).group(false).conceptId(903111L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 2);
  }

  @Test
  public void countSubjectsPPI() throws Exception {
    //Survey
    SearchParameter ppiSurvey = new SearchParameter().type(TreeType.PPI.name()).subtype(TreeSubType.BASICS.toString()).group(true);
    SearchRequest searchRequest = createSearchRequests(ppiSurvey.getType(), Arrays.asList(ppiSurvey), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    //Question
    SearchParameter ppiQuestion = new SearchParameter().type(TreeType.PPI.name()).subtype(TreeSubType.BASICS.toString()).group(true).conceptId(1585899L);
    searchRequest = createSearchRequests(ppiQuestion.getType(), Arrays.asList(ppiQuestion), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    //value as concept id
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("7"))
    );
    SearchParameter ppiValueAsConceptId = new SearchParameter().type(TreeType.PPI.name()).subtype(TreeSubType.BASICS.toString()).group(false).conceptId(5L).attributes(attributes);
    searchRequest = createSearchRequests(ppiValueAsConceptId.getType(), Arrays.asList(ppiValueAsConceptId), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);

    //value as number
    attributes = Arrays.asList(
      new Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList("7"))
    );
    SearchParameter ppiValueAsNumer = new SearchParameter().type(TreeType.PPI.name()).subtype(TreeSubType.BASICS.toString()).group(false).conceptId(5L).attributes(attributes);
    searchRequest = createSearchRequests(ppiValueAsNumer.getType(), Arrays.asList(ppiValueAsNumer), new ArrayList<>());
    assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest), 1);
  }

  @Test
  public void getDemoChartInfo() throws Exception {
    List<Attribute> attributes = Arrays.asList(
      new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList("4023190"))
    );
    SearchParameter pm = new SearchParameter().type(TreeType.PM.name()).subtype(TreeSubType.WHEEL.toString()).group(false).conceptId(903111L).attributes(attributes);
    SearchRequest searchRequest = createSearchRequests(TreeType.PM.name(), Arrays.asList(pm), new ArrayList<>());

    DemoChartInfoListResponse response = controller.getDemoChartInfo(cdrVersion.getCdrVersionId(), searchRequest).getBody();
    assertEquals(2, response.getItems().size());
    assertEquals(new DemoChartInfo().gender("MALE").race("Asian").ageRange("45-64").count(1L), response.getItems().get(0));
    assertEquals(new DemoChartInfo().gender("MALE").race("Caucasian").ageRange("19-44").count(1L), response.getItems().get(1));
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

  private SearchRequest createSearchRequests(String type,
                                             List<SearchParameter> parameters,
                                             List<Modifier> modifiers) {
    final SearchGroupItem searchGroupItem = new SearchGroupItem()
      .type(type)
      .searchParameters(parameters)
      .modifiers(modifiers);

    final SearchGroup searchGroup = new SearchGroup()
      .addItemsItem(searchGroupItem);

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
