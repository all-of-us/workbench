package org.pmiops.workbench.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.elasticsearch.ElasticSearchService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersionListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortBuilderControllerTest {

  private CohortBuilderController controller;
  private CohortBuilderService cohortBuilderService;
  @Mock private BigQueryService bigQueryService;
  @Mock private CloudStorageService cloudStorageService;
  @Mock private CohortQueryBuilder cohortQueryBuilder;
  @Mock private CdrVersionService cdrVersionService;
  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;
  @Autowired private CBDataFilterDao cbDataFilterDao;
  @Autowired private DomainInfoDao domainInfoDao;
  @Autowired private PersonDao personDao;
  @Autowired private SurveyModuleDao surveyModuleDao;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private CohortBuilderMapper cohortBuilderMapper;
  @Mock private Provider<WorkbenchConfig> configProvider;

  @TestConfiguration
  @Import({CohortBuilderMapperImpl.class})
  static class Configuration {}

  @Before
  public void setUp() {
    ElasticSearchService elasticSearchService =
        new ElasticSearchService(cbCriteriaDao, cloudStorageService, configProvider);

    cohortBuilderService =
        new CohortBuilderServiceImpl(
            bigQueryService,
            cohortQueryBuilder,
            cbCriteriaAttributeDao,
            cbCriteriaDao,
            cbDataFilterDao,
            domainInfoDao,
            personDao,
            surveyModuleDao,
            cohortBuilderMapper);
    controller =
        new CohortBuilderController(
            cdrVersionService, elasticSearchService, configProvider, cohortBuilderService);
  }

  @Test
  public void findDomainInfos() {
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addCount(0L)
            .addHierarchy(true)
            .addStandard(false)
            .addParentId(0)
            .addFullText("term*[CONDITION_rank1]")
            .build());
    DbDomainInfo dbDomainInfo =
        domainInfoDao.save(
            new DbDomainInfo()
                .conceptId(1L)
                .domain((short) 0)
                .domainId("CONDITION")
                .name("Conditions")
                .description("descr")
                .allConceptCount(0)
                .standardConceptCount(0)
                .participantCount(1000));

    DomainInfo domainInfo = controller.findDomainInfos(1L).getBody().getItems().get(0);
    assertEquals(domainInfo.getName(), dbDomainInfo.getName());
    assertEquals(domainInfo.getDescription(), dbDomainInfo.getDescription());
    assertEquals(domainInfo.getParticipantCount().longValue(), dbDomainInfo.getParticipantCount());
    assertEquals(domainInfo.getAllConceptCount().longValue(), 0);
    assertEquals(domainInfo.getStandardConceptCount().longValue(), 0);
  }

  @Test
  public void findSurveyModules() {
    DbCriteria surveyCriteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.QUESTION.toString())
                .addCount(0L)
                .addHierarchy(true)
                .addStandard(false)
                .addParentId(0)
                .addConceptId("1")
                .addName("The Basics")
                .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype(CriteriaSubType.ANSWER.toString())
            .addCount(0L)
            .addHierarchy(true)
            .addStandard(false)
            .addParentId(0)
            .addConceptId("1")
            .addFullText("term*[SURVEY_rank1]")
            .addPath(String.valueOf(surveyCriteria.getId()))
            .build());
    DbSurveyModule dbSurveyModule =
        surveyModuleDao.save(
            new DbSurveyModule()
                .conceptId(1L)
                .name("The Basics")
                .description("descr")
                .questionCount(1)
                .participantCount(1000));

    SurveyModule surveyModule = controller.findSurveyModules(1L).getBody().getItems().get(0);
    assertEquals(surveyModule.getName(), dbSurveyModule.getName());
    assertEquals(surveyModule.getDescription(), dbSurveyModule.getDescription());
    assertEquals(
        surveyModule.getParticipantCount().longValue(), dbSurveyModule.getParticipantCount());
    assertEquals(surveyModule.getQuestionCount().longValue(), 1);
  }

  @Test
  public void findCriteriaBy() {
    DbCriteria icd9CriteriaParent =
        DbCriteria.builder()
            .addDomainId(Domain.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addCount(0L)
            .addHierarchy(true)
            .addStandard(false)
            .addParentId(0L)
            .build();
    cbCriteriaDao.save(icd9CriteriaParent);
    DbCriteria icd9Criteria =
        DbCriteria.builder()
            .addDomainId(Domain.CONDITION.toString())
            .addType(CriteriaType.ICD9CM.toString())
            .addCount(0L)
            .addHierarchy(true)
            .addStandard(false)
            .addParentId(icd9CriteriaParent.getId())
            .build();
    cbCriteriaDao.save(icd9Criteria);

    assertEquals(
        createResponseCriteria(icd9CriteriaParent),
        controller
            .findCriteriaBy(
                1L, Domain.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L)
            .getBody()
            .getItems()
            .get(0));
    assertEquals(
        createResponseCriteria(icd9Criteria),
        controller
            .findCriteriaBy(
                1L,
                Domain.CONDITION.toString(),
                CriteriaType.ICD9CM.toString(),
                false,
                icd9CriteriaParent.getId())
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByExceptions() {
    try {
      controller.findCriteriaBy(1L, null, null, false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. null is not valid.", bre.getMessage());
    }

    try {
      controller.findCriteriaBy(1L, "blah", null, false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. blah is not valid.", bre.getMessage());
    }

    try {
      controller.findCriteriaBy(1L, Domain.CONDITION.toString(), "blah", false, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void findCriteriaByDemo() {
    DbCriteria demoCriteria =
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.AGE.toString())
            .addCount(0L)
            .addParentId(0L)
            .build();
    cbCriteriaDao.save(demoCriteria);

    assertEquals(
        createResponseCriteria(demoCriteria),
        controller
            .findCriteriaBy(1L, Domain.PERSON.toString(), CriteriaType.AGE.toString(), false, null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaAutoCompleteMatchesSynonyms() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(Domain.MEASUREMENT.toString())
            .addType(CriteriaType.LOINC.toString())
            .addCount(0L)
            .addHierarchy(true)
            .addStandard(true)
            .addFullText("LP12*[MEASUREMENT_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaAutoComplete(
                1L,
                Domain.MEASUREMENT.toString(),
                "LP12",
                CriteriaType.LOINC.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaAutoCompleteMatchesCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(Domain.MEASUREMENT.toString())
            .addType(CriteriaType.LOINC.toString())
            .addCount(0L)
            .addHierarchy(true)
            .addStandard(true)
            .addCode("LP123")
            .addFullText("+[MEASUREMENT_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaAutoComplete(
                1L,
                Domain.MEASUREMENT.toString(),
                "LP12",
                CriteriaType.LOINC.toString(),
                true,
                null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaAutoCompleteSnomed() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(Domain.CONDITION.toString())
            .addType(CriteriaType.SNOMED.toString())
            .addCount(0L)
            .addHierarchy(true)
            .addStandard(true)
            .addFullText("LP12*[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaAutoComplete(
                1L, Domain.CONDITION.toString(), "LP12", CriteriaType.SNOMED.toString(), true, null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaAutoCompleteExceptions() {
    try {
      controller.findCriteriaAutoComplete(1L, null, "blah", null, null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. null is not valid.", bre.getMessage());
    }

    try {
      controller.findCriteriaAutoComplete(1L, "blah", "blah", "blah", null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid domain. blah is not valid.", bre.getMessage());
    }

    try {
      controller.findCriteriaAutoComplete(
          1L, Domain.CONDITION.toString(), "blah", "blah", null, null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertEquals(
          "Bad Request: Please provide a valid type. blah is not valid.", bre.getMessage());
    }
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesSourceCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("001")
            .addCount(10L)
            .addConceptId("123")
            .addDomainId(Domain.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.ICD9CM.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(false)
            .addFullText("[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, Domain.CONDITION.name(), "001", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermLikeSourceCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("00")
            .addCount(10L)
            .addConceptId("123")
            .addDomainId(Domain.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.ICD9CM.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(false)
            .addFullText("+[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    List<Criteria> results =
        controller
            .findCriteriaByDomainAndSearchTerm(1L, Domain.CONDITION.name(), "00", null)
            .getBody()
            .getItems();

    assertEquals(1, results.size());
    assertEquals(createResponseCriteria(criteria), results.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermDrugMatchesStandardCodeBrand() {
    DbCriteria criteria1 =
        DbCriteria.builder()
            .addCode("672535")
            .addCount(-1L)
            .addConceptId("19001487")
            .addDomainId(Domain.DRUG.toString())
            .addGroup(Boolean.FALSE)
            .addSelectable(Boolean.TRUE)
            .addName("4-Way")
            .addParentId(0)
            .addType(CriteriaType.BRAND.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addFullText("[DRUG_rank1]")
            .build();
    cbCriteriaDao.save(criteria1);

    List<Criteria> results =
        controller
            .findCriteriaByDomainAndSearchTerm(1L, Domain.DRUG.name(), "672535", null)
            .getBody()
            .getItems();
    assertEquals(1, results.size());
    assertEquals(createResponseCriteria(criteria1), results.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesStandardCode() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("LP12")
            .addCount(10L)
            .addConceptId("123")
            .addDomainId(Domain.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.LOINC.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addFullText("[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, Domain.CONDITION.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermMatchesSynonyms() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("001")
            .addCount(10L)
            .addConceptId("123")
            .addDomainId(Domain.CONDITION.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.LOINC.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addFullText("LP12*[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, Domain.CONDITION.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermDrugMatchesSynonyms() {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    DbCriteria criteria =
        DbCriteria.builder()
            .addCode("001")
            .addCount(10L)
            .addConceptId("123")
            .addDomainId(Domain.DRUG.toString())
            .addGroup(Boolean.TRUE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.ATC.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(true)
            .addFullText("LP12*[DRUG_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findCriteriaByDomainAndSearchTerm(1L, Domain.DRUG.name(), "LP12", null)
            .getBody()
            .getItems()
            .get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findStandardCriteriaByDomainAndConceptId() {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(Domain.CONDITION.toString())
            .addType(CriteriaType.ICD10CM.toString())
            .addStandard(true)
            .addCount(1L)
            .addConceptId("1")
            .addFullText("[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);
    assertEquals(
        createResponseCriteria(criteria),
        controller
            .findStandardCriteriaByDomainAndConceptId(1L, Domain.CONDITION.toString(), 12345L)
            .getBody()
            .getItems()
            .get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findDrugBrandOrIngredientByName() {
    DbCriteria drugATCCriteria =
        DbCriteria.builder()
            .addDomainId(Domain.DRUG.toString())
            .addType(CriteriaType.ATC.toString())
            .addParentId(0L)
            .addCode("LP12345")
            .addName("drugName")
            .addConceptId("12345")
            .addGroup(true)
            .addSelectable(true)
            .addCount(12L)
            .build();
    cbCriteriaDao.save(drugATCCriteria);
    DbCriteria drugBrandCriteria =
        DbCriteria.builder()
            .addDomainId(Domain.DRUG.toString())
            .addType(CriteriaType.BRAND.toString())
            .addParentId(0L)
            .addCode("LP6789")
            .addName("brandName")
            .addConceptId("1235")
            .addGroup(true)
            .addSelectable(true)
            .addCount(33L)
            .build();
    cbCriteriaDao.save(drugBrandCriteria);

    assertEquals(
        createResponseCriteria(drugATCCriteria),
        controller.findDrugBrandOrIngredientByValue(1L, "drugN", null).getBody().getItems().get(0));

    assertEquals(
        createResponseCriteria(drugBrandCriteria),
        controller
            .findDrugBrandOrIngredientByValue(1L, "brandN", null)
            .getBody()
            .getItems()
            .get(0));

    assertEquals(
        createResponseCriteria(drugBrandCriteria),
        controller
            .findDrugBrandOrIngredientByValue(1L, "LP6789", null)
            .getBody()
            .getItems()
            .get(0));
  }

  @Test
  public void findCriteriaAttributeByConceptId() {
    DbCriteriaAttribute criteriaAttributeMin =
        cbCriteriaAttributeDao.save(
            DbCriteriaAttribute.builder()
                .addConceptId(1L)
                .addConceptName("MIN")
                .addEstCount("10")
                .addType("NUM")
                .addValueAsConceptId(0L)
                .build());
    DbCriteriaAttribute criteriaAttributeMax =
        cbCriteriaAttributeDao.save(
            DbCriteriaAttribute.builder()
                .addConceptId(1L)
                .addConceptName("MAX")
                .addEstCount("100")
                .addType("NUM")
                .addValueAsConceptId(0L)
                .build());

    List<CriteriaAttribute> attrs =
        controller
            .findCriteriaAttributeByConceptId(1L, criteriaAttributeMin.getConceptId())
            .getBody()
            .getItems();
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMin)));
    assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMax)));
  }

  @Test
  public void findParticipantDemographics() {
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.GENDER.toString())
            .addName("Male")
            .addStandard(true)
            .addConceptId("1")
            .addParentId(1)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addName("African American")
            .addStandard(true)
            .addConceptId("2")
            .addParentId(1)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.ETHNICITY.toString())
            .addName("Not Hispanic or Latino")
            .addStandard(true)
            .addConceptId("3")
            .addParentId(1)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.SEX.toString())
            .addName("Male")
            .addStandard(true)
            .addConceptId("4")
            .addParentId(1)
            .build());
    ParticipantDemographics demos = controller.findParticipantDemographics(1L).getBody();
    assertEquals(
        new ConceptIdName().conceptId(1L).conceptName("Male"), demos.getGenderList().get(0));
    assertEquals(
        new ConceptIdName().conceptId(2L).conceptName("African American"),
        demos.getRaceList().get(0));
    assertEquals(
        new ConceptIdName().conceptId(3L).conceptName("Not Hispanic or Latino"),
        demos.getEthnicityList().get(0));
    assertEquals(
        new ConceptIdName().conceptId(4L).conceptName("Male"), demos.getSexAtBirthList().get(0));
  }

  @Test
  public void findSurveyVersionByQuestionConceptId() {
    jdbcTemplate.execute(
        "create table cb_survey_version(survey_id integer, concept_id integer, version varchar(50), display_order integer)");
    jdbcTemplate.execute(
        "create table cb_survey_attribute(id integer, question_concept_id integer, answer_concept_id integer, survey_id integer, item_count integer)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_id, concept_id, version, display_order) values (100, 1333342, 'May 2020', 1)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_id, concept_id, version, display_order) values (101, 1333342, 'June 2020', 2)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_id, concept_id, version, display_order) values (102, 1333342, 'July 2020', 3)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_id, item_count) values (1, 715713, 0, 100, 291)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_id, item_count) values (2, 715713, 0, 101, 148)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_id, item_count) values (3, 715713, 0, 102, 150)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_id, item_count) values (4, 715713, 903096, 100, 154)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_id, item_count) values (5, 715713, 903096, 101, 82)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_id, item_count) values (6, 715713, 903096, 102, 31)");
    SurveyVersionListResponse response =
        controller.findSurveyVersionByQuestionConceptId(1L, 1333342L, 715713L).getBody();
    assertEquals(response.getItems().get(0).getSurveyId(), new Long("100"));
    assertEquals(response.getItems().get(0).getVersion(), "May 2020");
    assertEquals(response.getItems().get(0).getItemCount(), new Long("291"));
    assertEquals(response.getItems().get(1).getSurveyId(), new Long("101"));
    assertEquals(response.getItems().get(1).getVersion(), "June 2020");
    assertEquals(response.getItems().get(1).getItemCount(), new Long("148"));
    assertEquals(response.getItems().get(2).getSurveyId(), new Long("102"));
    assertEquals(response.getItems().get(2).getVersion(), "July 2020");
    assertEquals(response.getItems().get(2).getItemCount(), new Long("150"));
    jdbcTemplate.execute("drop table cb_survey_version");
    jdbcTemplate.execute("drop table cb_survey_attribute");
  }

  @Test
  public void isApproximate() {
    SearchParameter inSearchParameter = new SearchParameter();
    SearchParameter exSearchParameter = new SearchParameter();
    SearchGroupItem inSearchGroupItem =
        new SearchGroupItem().addSearchParametersItem(inSearchParameter);
    SearchGroupItem exSearchGroupItem =
        new SearchGroupItem().addSearchParametersItem(exSearchParameter);
    SearchGroup inSearchGroup = new SearchGroup().addItemsItem(inSearchGroupItem);
    SearchGroup exSearchGroup = new SearchGroup().addItemsItem(exSearchGroupItem);
    SearchRequest searchRequest =
        new SearchRequest().addIncludesItem(inSearchGroup).addExcludesItem(exSearchGroup);
    // Temporal includes
    inSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    // BP includes
    inSearchGroup.temporal(false);
    inSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased includes
    inSearchParameter.type(CriteriaType.DECEASED.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP includes
    inSearchGroup.temporal(true);
    inSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // No temporal/BP/Decease
    inSearchGroup.temporal(false);
    inSearchParameter.type(CriteriaType.ETHNICITY.toString()).subtype(null);
    assertFalse(controller.isApproximate(searchRequest));
    // Temporal excludes
    exSearchGroup.temporal(true);
    assertTrue(controller.isApproximate(searchRequest));
    // BP excludes
    exSearchGroup.temporal(false);
    exSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Deceased excludes
    exSearchParameter.type(CriteriaType.DECEASED.toString());
    assertTrue(controller.isApproximate(searchRequest));
    // Temporal and BP excludes
    exSearchGroup.temporal(true);
    exSearchParameter.subtype(CriteriaSubType.BP.toString());
    assertTrue(controller.isApproximate(searchRequest));
  }

  private Criteria createResponseCriteria(DbCriteria dbCriteria) {
    return new Criteria()
        .code(dbCriteria.getCode())
        .conceptId(dbCriteria.getConceptId() == null ? null : new Long(dbCriteria.getConceptId()))
        .count(new Long(dbCriteria.getCount()))
        .parentCount(dbCriteria.getParentCount())
        .childCount(dbCriteria.getChildCount())
        .domainId(dbCriteria.getDomainId())
        .group(dbCriteria.getGroup())
        .hasAttributes(dbCriteria.getAttribute())
        .id(dbCriteria.getId())
        .name(dbCriteria.getName())
        .parentId(dbCriteria.getParentId())
        .selectable(dbCriteria.getSelectable())
        .subtype(dbCriteria.getSubtype())
        .type(dbCriteria.getType())
        .path(dbCriteria.getPath())
        .hasAncestorData(dbCriteria.getAncestorData())
        .hasHierarchy(dbCriteria.getHierarchy())
        .isStandard(dbCriteria.getStandard())
        .value(dbCriteria.getValue());
  }

  private CriteriaAttribute createResponseCriteriaAttribute(
      DbCriteriaAttribute dbCriteriaAttribute) {
    return new CriteriaAttribute()
        .id(dbCriteriaAttribute.getId())
        .valueAsConceptId(dbCriteriaAttribute.getValueAsConceptId())
        .conceptName(dbCriteriaAttribute.getConceptName())
        .type(dbCriteriaAttribute.getType())
        .estCount(dbCriteriaAttribute.getEstCount());
  }
}
