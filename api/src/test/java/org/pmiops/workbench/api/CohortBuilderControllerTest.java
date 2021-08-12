package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.List;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.cache.MySQLStopWords;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.CBMenuDao;
import org.pmiops.workbench.cdr.dao.CriteriaMenuDao;
import org.pmiops.workbench.cdr.dao.DomainCardDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbCBMenu;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.pmiops.workbench.cdr.model.DbDomainCard;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCard;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersionListResponse;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortBuilderControllerTest extends SpringTest {

  private CohortBuilderController controller;

  @Mock private BigQueryService bigQueryService;
  @Mock private CloudStorageClient cloudStorageClient;
  @Mock private CohortQueryBuilder cohortQueryBuilder;
  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;
  @Autowired private CBDataFilterDao cbDataFilterDao;
  @Autowired private CriteriaMenuDao criteriaMenuDao;
  @Autowired private CBMenuDao cbMenuDao;
  @Autowired private DomainInfoDao domainInfoDao;
  @Autowired private DomainCardDao domainCardDao;
  @Autowired private PersonDao personDao;
  @Autowired private SurveyModuleDao surveyModuleDao;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private CohortBuilderMapper cohortBuilderMapper;
  @Mock private WorkspaceAuthService workspaceAuthService;
  @Mock private Provider<WorkbenchConfig> configProvider;
  @Mock private Provider<MySQLStopWords> mySQLStopWordsProvider;

  @TestConfiguration
  @Import({CohortBuilderMapperImpl.class})
  @MockBean({WorkspaceAuthService.class})
  static class Configuration {}

  private static final String WORKSPACE_ID = "workspaceId";
  private static final String WORKSPACE_NAMESPACE = "workspaceNS";

  @BeforeEach
  public void setUp() {

    CohortBuilderService cohortBuilderService =
        new CohortBuilderServiceImpl(
            bigQueryService,
            cohortQueryBuilder,
            cbCriteriaAttributeDao,
            cbCriteriaDao,
            criteriaMenuDao,
            cbMenuDao,
            cbDataFilterDao,
            domainInfoDao,
            domainCardDao,
            personDao,
            surveyModuleDao,
            cohortBuilderMapper,
            mySQLStopWordsProvider);
    controller =
        new CohortBuilderController(configProvider, cohortBuilderService, workspaceAuthService);

    MySQLStopWords mySQLStopWords = new MySQLStopWords(Arrays.asList("about"));
    doReturn(mySQLStopWords).when(mySQLStopWordsProvider).get();
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(1l);
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    dbWorkspace.setName("Saved workspace");
    dbWorkspace.setFirecloudName(WORKSPACE_ID);
    dbWorkspace.setCdrVersion(cdrVersion);
  }

  //  TODO: Remove this test once feature flag enableStandardSourceDomains is removed
  @Test
  public void findCriteriaMenuOld() {
    DbCriteriaMenu dbCriteriaMenu =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addGroup(true)
                .addName("Condition")
                .addSortOrder(2L)
                .build());
    assertThat(
            controller
                .findCriteriaMenuOld(WORKSPACE_NAMESPACE, WORKSPACE_ID, 0L)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(cohortBuilderMapper.dbModelToClient(dbCriteriaMenu));
  }

  @Test
  public void findCriteriaMenu() {
    DbCBMenu dbCBMenu =
        cbMenuDao.save(
            DbCBMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addGroup(true)
                .addName("Condition")
                .addIsStandard(true)
                .addSortOrder(2L)
                .build());
    assertThat(
            controller
                .findCriteriaMenu(WORKSPACE_NAMESPACE, WORKSPACE_ID, 0L)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(cohortBuilderMapper.dbModelToClient(dbCBMenu));
  }

  // Todo: Remove this test once the standardSource flag is true on all environment
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
                .allConceptCount(10)
                .standardConceptCount(0)
                .participantCount(1000));

    DomainInfo domainInfo =
        controller.findDomainInfos(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody().getItems().get(0);
    assertThat(domainInfo.getName()).isEqualTo(dbDomainInfo.getName());
    assertThat(domainInfo.getDescription()).isEqualTo(dbDomainInfo.getDescription());
    assertThat(domainInfo.getParticipantCount()).isEqualTo(dbDomainInfo.getParticipantCount());
    assertThat(domainInfo.getAllConceptCount()).isEqualTo(10);
    assertThat(domainInfo.getStandardConceptCount()).isEqualTo(0);
  }

  @Test
  public void findDomainCards() {
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
    DbDomainCard dbDomainCard =
        domainCardDao.save(
            new DbDomainCard()
                .id(1L)
                .domain((short) 0)
                .name("Conditions")
                .description("descr")
                .conceptCount(10)
                .participantCount(1000)
                .standard(false)
                .sortOrder(3));

    DomainCard domainCard =
        controller.findDomainCards(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody().getItems().get(0);
    assertThat(domainCard.getName()).isEqualTo(dbDomainCard.getName());
    assertThat(domainCard.getDescription()).isEqualTo(dbDomainCard.getDescription());
    assertThat(domainCard.getParticipantCount()).isEqualTo(dbDomainCard.getParticipantCount());
    assertThat(domainCard.getConceptCount()).isEqualTo(10);
    assertThat(domainCard.getStandard()).isFalse();
    assertThat(domainCard.getSortOrder()).isEqualTo(3);
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

    SurveyModule surveyModule =
        controller.findSurveyModules(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody().getItems().get(0);
    assertThat(surveyModule.getName()).isEqualTo(dbSurveyModule.getName());
    assertThat(surveyModule.getDescription()).isEqualTo(dbSurveyModule.getDescription());
    assertThat(surveyModule.getParticipantCount()).isEqualTo(dbSurveyModule.getParticipantCount());
    assertThat(surveyModule.getQuestionCount()).isEqualTo(1);
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

    assertThat(
            controller
                .findCriteriaBy(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.toString(),
                    CriteriaType.ICD9CM.toString(),
                    false,
                    0L)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(icd9CriteriaParent));

    assertThat(
            controller
                .findCriteriaBy(
                    "1",
                    "1",
                    Domain.CONDITION.toString(),
                    CriteriaType.ICD9CM.toString(),
                    false,
                    icd9CriteriaParent.getId())
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(icd9Criteria));
  }

  @Test
  public void findCriteriaByExceptions() {
    assertThrows(
        BadRequestException.class,
        () -> controller.findCriteriaBy(WORKSPACE_NAMESPACE, WORKSPACE_ID, null, null, false, null),
        "Bad Request: Please provide a valid domain. null is not valid.");

    assertThrows(
        BadRequestException.class,
        () ->
            controller.findCriteriaBy(WORKSPACE_NAMESPACE, WORKSPACE_ID, "blah", null, false, null),
        "Bad Request: Please provide a valid domain. blah is not valid.");
    assertThrows(
        BadRequestException.class,
        () ->
            controller.findCriteriaBy(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                Domain.CONDITION.toString(),
                "blah",
                false,
                null),
        "Bad Request: Please provide a valid type. blah is not valid.");
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

    assertThat(
            controller
                .findCriteriaBy(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.PERSON.toString(),
                    CriteriaType.AGE.toString(),
                    false,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(demoCriteria));
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

    assertThat(
            controller
                .findCriteriaAutoComplete(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.MEASUREMENT.toString(),
                    "LP12",
                    CriteriaType.LOINC.toString(),
                    true,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
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

    assertThat(
            controller
                .findCriteriaAutoComplete(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.MEASUREMENT.toString(),
                    "LP12",
                    CriteriaType.LOINC.toString(),
                    true,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
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

    assertThat(
            controller
                .findCriteriaAutoComplete(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.toString(),
                    "LP12",
                    CriteriaType.SNOMED.toString(),
                    true,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
  }

  @Test
  public void findCriteriaAutoCompleteExceptions() {
    assertThrows(
        BadRequestException.class,
        () ->
            controller.findCriteriaAutoComplete(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, null, "blah", null, null, null),
        "Bad Request: Please provide a valid domain. null is not valid.");

    assertThrows(
        BadRequestException.class,
        () ->
            controller.findCriteriaAutoComplete(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, null, "blah", null, null, null),
        "Bad Request: Please provide a valid domain. blah is not valid.");

    assertThrows(
        BadRequestException.class,
        () ->
            controller.findCriteriaAutoComplete(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                Domain.CONDITION.toString(),
                "blah",
                "blah",
                null,
                null),
        "Bad Request: Please provide a valid type. blah is not valid.");
  }

  @Test
  public void findCriteriaByDomainPhysicalMeasurement() {
    DbCriteria dbCriteria =
        DbCriteria.builder()
            .addCode("12345")
            .addCount(10L)
            .addConceptId("123")
            .addDomainId(Domain.PHYSICAL_MEASUREMENT_CSS.toString())
            .addGroup(Boolean.FALSE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.PPI.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(false)
            .addFullText("[PHYSICAL_MEASUREMENT_CSS_rank1]")
            .build();
    cbCriteriaDao.save(dbCriteria);

    assertThat(cohortBuilderMapper.dbModelToClient(dbCriteria))
        .isEqualTo(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.PHYSICAL_MEASUREMENT_CSS.toString(),
                    false,
                    "12345",
                    null,
                    null)
                .getBody()
                .getItems()
                .get(0));

    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.PHYSICAL_MEASUREMENT_CSS.toString(),
                    true,
                    "12345",
                    null,
                    null)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void findCriteriaByDomainMatchesSourceCode() {
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

    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.name(),
                    false,
                    "001",
                    null,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.name(),
                    true,
                    "001",
                    null,
                    null)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void findCriteriaByDomainLikeSourceCode() {
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
            .findCriteriaByDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), false, "00", null, null)
            .getBody()
            .getItems();

    assertThat(1).isEqualTo(results.size());
    assertThat(results.get(0)).isEqualTo(createResponseCriteria(criteria));

    results =
        controller
            .findCriteriaByDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), true, "00", null, null)
            .getBody()
            .getItems();

    assertThat(0).isEqualTo(results.size());
  }

  @Test
  public void findCriteriaByDomainDrugMatchesStandardCodeBrand() {
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
            .findCriteriaByDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.DRUG.name(), true, "672535", null, null)
            .getBody()
            .getItems();
    assertThat(1).isEqualTo(results.size());
    assertThat(results.get(0)).isEqualTo(createResponseCriteria(criteria1));

    results =
        controller
            .findCriteriaByDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.DRUG.name(), false, "672535", null, null)
            .getBody()
            .getItems();
    assertThat(0).isEqualTo(results.size());
  }

  @Test
  public void findCriteriaByDomainMatchesStandardCode() {
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
            .addStandard(false)
            .addFullText("[CONDITION_rank1]")
            .build();
    cbCriteriaDao.save(criteria);

    //    assertThat(
    //            controller
    //                .findCriteriaByDomain(
    //                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), true, "LP12",
    // null, null)
    //                .getBody()
    //                .getItems()).isEmpty();

    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.name(),
                    false,
                    "LP12",
                    null,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
  }

  @Test
  public void findCriteriaByDomainMatchesSynonyms() {
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

    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.name(),
                    true,
                    "LP12",
                    null,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.name(),
                    false,
                    "LP12",
                    null,
                    null)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void findCriteriaByDomainEmptyTerm() {
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

    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.name(),
                    true,
                    "",
                    null,
                    null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));

    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.CONDITION.name(),
                    false,
                    "",
                    null,
                    null)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void findCriteriaByDomainDrugMatchesSynonyms() {
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

    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.DRUG.name(), true, "LP12", null, null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
    assertThat(
            controller
                .findCriteriaByDomain(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.DRUG.name(),
                    false,
                    "LP12",
                    null,
                    null)
                .getBody()
                .getItems())
        .isEmpty();
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findCriteriaByDomainAndSearchTermPhysicalMeasurement() {
    DbCriteria dbCriteria =
        DbCriteria.builder()
            .addCode("12345")
            .addCount(10L)
            .addConceptId("123")
            .addDomainId(Domain.PHYSICAL_MEASUREMENT_CSS.toString())
            .addGroup(Boolean.FALSE)
            .addSelectable(Boolean.TRUE)
            .addName("chol blah")
            .addParentId(0)
            .addType(CriteriaType.PPI.toString())
            .addAttribute(Boolean.FALSE)
            .addStandard(false)
            .addFullText("[PHYSICAL_MEASUREMENT_CSS_rank1]")
            .build();
    cbCriteriaDao.save(dbCriteria);

    assertThat(cohortBuilderMapper.dbModelToClient(dbCriteria))
        .isEqualTo(
            controller
                .findCriteriaByDomainAndSearchTerm(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_ID,
                    Domain.PHYSICAL_MEASUREMENT_CSS.toString(),
                    "12345",
                    null,
                    null)
                .getBody()
                .getItems()
                .get(0));
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

    assertThat(
            controller
                .findCriteriaByDomainAndSearchTerm(
                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), "001", null, null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
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
            .findCriteriaByDomainAndSearchTerm(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), "00", null, null)
            .getBody()
            .getItems();

    assertThat(1).isEqualTo(results.size());
    assertThat(results.get(0)).isEqualTo(createResponseCriteria(criteria));
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
            .findCriteriaByDomainAndSearchTerm(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.DRUG.name(), "672535", null, null)
            .getBody()
            .getItems();
    assertThat(1).isEqualTo(results.size());
    assertThat(results.get(0)).isEqualTo(createResponseCriteria(criteria1));
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

    assertThat(
            controller
                .findCriteriaByDomainAndSearchTerm(
                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), "LP12", null, null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
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

    assertThat(
            controller
                .findCriteriaByDomainAndSearchTerm(
                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), "LP12", null, null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
  }

  @Test
  public void findCriteriaByDomainAndSearchTermEmptyTerm() {
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

    assertThat(
            controller
                .findCriteriaByDomainAndSearchTerm(
                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.name(), "", null, null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
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

    assertThat(
            controller
                .findCriteriaByDomainAndSearchTerm(
                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.DRUG.name(), "LP12", null, null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
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
    assertThat(
            controller
                .findStandardCriteriaByDomainAndConceptId(
                    WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.toString(), 12345L)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
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

    assertThat(
            controller
                .findDrugBrandOrIngredientByValue(WORKSPACE_NAMESPACE, WORKSPACE_ID, "drugN", null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(drugATCCriteria));

    assertThat(
            controller
                .findDrugBrandOrIngredientByValue(WORKSPACE_NAMESPACE, WORKSPACE_ID, "brandN", null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(drugBrandCriteria));

    assertThat(
            controller
                .findDrugBrandOrIngredientByValue(WORKSPACE_NAMESPACE, WORKSPACE_ID, "LP6789", null)
                .getBody()
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(drugBrandCriteria));
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
            .findCriteriaAttributeByConceptId(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, criteriaAttributeMin.getConceptId())
            .getBody()
            .getItems();
    assertThat(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMin))).isTrue();
    assertThat(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMax))).isTrue();
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
    ParticipantDemographics demos =
        controller.findParticipantDemographics(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody();
    assertThat(new ConceptIdName().conceptId(1L).conceptName("Male"))
        .isEqualTo(demos.getGenderList().get(0));
    assertThat(new ConceptIdName().conceptId(2L).conceptName("African American"))
        .isEqualTo(demos.getRaceList().get(0));
    assertThat(new ConceptIdName().conceptId(3L).conceptName("Not Hispanic or Latino"))
        .isEqualTo(demos.getEthnicityList().get(0));
    assertThat(new ConceptIdName().conceptId(4L).conceptName("Male"))
        .isEqualTo(demos.getSexAtBirthList().get(0));
  }

  @Test
  public void findSurveyVersionByQuestionConceptId() {
    jdbcTemplate.execute(
        "create table cb_survey_version(survey_version_concept_id integer, survey_concept_id integer, display_name varchar(50), display_order integer)");
    jdbcTemplate.execute(
        "create table cb_survey_attribute(id integer, question_concept_id integer, answer_concept_id integer, survey_version_concept_id integer, item_count integer)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (100, 1333342, 'May 2020', 1)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (101, 1333342, 'June 2020', 2)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (102, 1333342, 'July 2020', 3)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (1, 715713, 0, 100, 291)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (2, 715713, 0, 101, 148)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (3, 715713, 0, 102, 150)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (4, 715713, 903096, 100, 154)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (5, 715713, 903096, 101, 82)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (6, 715713, 903096, 102, 31)");
    SurveyVersionListResponse response =
        controller
            .findSurveyVersionByQuestionConceptId(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, 1333342L, 715713L)
            .getBody();
    assertThat(response.getItems().get(0).getSurveyVersionConceptId()).isEqualTo(new Long("100"));
    assertThat(response.getItems().get(0).getDisplayName()).isEqualTo("May 2020");
    assertThat(response.getItems().get(0).getItemCount()).isEqualTo(new Long("291"));
    assertThat(response.getItems().get(1).getSurveyVersionConceptId()).isEqualTo(new Long("101"));
    assertThat(response.getItems().get(1).getDisplayName()).isEqualTo("June 2020");
    assertThat(response.getItems().get(1).getItemCount()).isEqualTo(new Long("148"));
    assertThat(response.getItems().get(2).getSurveyVersionConceptId()).isEqualTo(new Long("102"));
    assertThat(response.getItems().get(2).getDisplayName()).isEqualTo("July 2020");
    assertThat(response.getItems().get(2).getItemCount()).isEqualTo(new Long("150"));
    jdbcTemplate.execute("drop table cb_survey_version");
    jdbcTemplate.execute("drop table cb_survey_attribute");
  }

  private Criteria createResponseCriteria(DbCriteria dbCriteria) {
    return new Criteria()
        .code(dbCriteria.getCode())
        .conceptId(dbCriteria.getConceptId() == null ? null : new Long(dbCriteria.getConceptId()))
        .count(dbCriteria.getCount())
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
