package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.apphosting.api.DeadlineExceededException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cdr.cache.MySQLStopWords;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.CriteriaMenuDao;
import org.pmiops.workbench.cdr.dao.DomainCardDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.pmiops.workbench.cdr.model.DbDomainCard;
import org.pmiops.workbench.cdr.model.DbPerson;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartService;
import org.pmiops.workbench.cohortbuilder.chart.ChartServiceImpl;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCard;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersion;
import org.pmiops.workbench.model.SurveyVersionListResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;
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
public class CohortBuilderControllerTest {

  private CohortBuilderController controller;

  @Mock private BigQueryService bigQueryService;
  @Mock private CohortQueryBuilder cohortQueryBuilder;
  @Mock private ChartQueryBuilder chartQueryBuilder;

  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;
  @Autowired private CBDataFilterDao cbDataFilterDao;
  @Autowired private CriteriaMenuDao criteriaMenuDao;
  @Autowired private DomainCardDao domainCardDao;
  @Autowired private PersonDao personDao;
  @Autowired private SurveyModuleDao surveyModuleDao;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private CohortBuilderMapper cohortBuilderMapper;

  @Autowired private CohortReviewMapper cohortReviewMapper;

  @Mock private WorkspaceAuthService workspaceAuthService;
  @Mock private Provider<MySQLStopWords> mySQLStopWordsProvider;
  @MockBean private Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final String WORKSPACE_ID = "workspaceId";
  private static final String WORKSPACE_NAMESPACE = "workspaceNS";

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    CohortBuilderMapperImpl.class,
    CohortReviewMapperImpl.class
  })
  @MockBean({WorkspaceAuthService.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {

    CohortBuilderService cohortBuilderService =
        new CohortBuilderServiceImpl(
            bigQueryService,
            cohortQueryBuilder,
            cbCriteriaAttributeDao,
            cbCriteriaDao,
            criteriaMenuDao,
            cbDataFilterDao,
            domainCardDao,
            personDao,
            surveyModuleDao,
            cohortBuilderMapper,
            mySQLStopWordsProvider);
    ChartService chartService =
        new ChartServiceImpl(
            bigQueryService, chartQueryBuilder, cohortBuilderMapper, cohortReviewMapper);

    controller =
        new CohortBuilderController(
            cohortBuilderService, chartService, workspaceAuthService, workbenchConfigProvider);

    MySQLStopWords mySQLStopWords = new MySQLStopWords(Collections.singletonList("about"));
    doReturn(mySQLStopWords).when(mySQLStopWordsProvider).get();

    WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.featureFlags.enableDrugWildcardSearch = true;
    doReturn(workbenchConfig).when(workbenchConfigProvider).get();

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(1L);
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    dbWorkspace.setName("Saved workspace");
    dbWorkspace.setFirecloudName(WORKSPACE_ID);
    dbWorkspace.setCdrVersion(cdrVersion);
  }

  @Test
  public void countParticipantsRaiseDeadlineExceededException() {
    stubBigQueryCallThrowDeadlineExceededException();
    assertThrows(
        DeadlineExceededException.class,
        () -> controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, new SearchRequest()));
  }

  @Test
  public void findCriteriaMenu() {
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
            Objects.requireNonNull(
                    controller.findCriteriaMenu(WORKSPACE_NAMESPACE, WORKSPACE_ID, 0L).getBody())
                .getItems()
                .get(0))
        .isEqualTo(cohortBuilderMapper.dbModelToClient(dbCriteriaMenu));
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
        Objects.requireNonNull(
                controller.findDomainCards(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody())
            .getItems()
            .get(0);
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
        Objects.requireNonNull(
                controller.findSurveyModules(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody())
            .getItems()
            .get(0);
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaBy(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.toString(),
                            CriteriaType.ICD9CM.toString(),
                            false,
                            0L)
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(icd9CriteriaParent));

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findCriteriaBy(
                            "1",
                            "1",
                            Domain.CONDITION.toString(),
                            CriteriaType.ICD9CM.toString(),
                            false,
                            icd9CriteriaParent.getId())
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaBy(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.PERSON.toString(),
                            CriteriaType.AGE.toString(),
                            false,
                            null)
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaAutoComplete(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.MEASUREMENT.toString(),
                            "LP12",
                            CriteriaType.LOINC.toString(),
                            true)
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaAutoComplete(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.MEASUREMENT.toString(),
                            "LP12",
                            CriteriaType.LOINC.toString(),
                            true)
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaAutoComplete(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.toString(),
                            "LP12",
                            CriteriaType.SNOMED.toString(),
                            true)
                        .getBody())
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
                WORKSPACE_NAMESPACE, WORKSPACE_ID, null, "blah", null, null),
        "Bad Request: Please provide a valid domain. null is not valid.");

    assertThrows(
        BadRequestException.class,
        () ->
            controller.findCriteriaAutoComplete(
                WORKSPACE_NAMESPACE, WORKSPACE_ID, null, "blah", null, null),
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
                null),
        "Bad Request: Please provide a valid type. blah is not valid.");
  }

  @Test
  public void findSurveyAutoCompleteTheBasics() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(Domain.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype("QUESTION")
            .addConceptId("1001")
            .addCount(0L)
            .addHierarchy(true)
            .addName("The Basics")
            .addStandard(true)
            .addFullText("LP12*[survey_rank1]")
            .build();
    criteria = cbCriteriaDao.save(criteria);
    // need to set the id in the path
    criteria.setPath(String.valueOf(criteria.getId()));
    criteria = cbCriteriaDao.save(criteria);

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findSurveyAutoComplete(
                            WORKSPACE_NAMESPACE, WORKSPACE_ID, "The Basics", "LP12")
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
  }

  @Test
  public void findSurveyAutoCompleteAllSurveys() {
    DbCriteria criteria =
        DbCriteria.builder()
            .addDomainId(Domain.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype("QUESTION")
            .addConceptId("1001")
            .addCount(0L)
            .addHierarchy(true)
            .addName("The Basics")
            .addStandard(true)
            .addFullText("LP12*[survey_rank1]")
            .build();
    criteria = cbCriteriaDao.save(criteria);
    // need to set the id in the path
    criteria.setPath(String.valueOf(criteria.getId()));
    criteria = cbCriteriaDao.save(criteria);

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findSurveyAutoComplete(WORKSPACE_NAMESPACE, WORKSPACE_ID, "All", "LP12")
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
  }

  @Test
  public void findSurveyAutoCompleteExceptions() {
    assertThrows(
        BadRequestException.class,
        () -> controller.findSurveyAutoComplete(WORKSPACE_NAMESPACE, WORKSPACE_ID, null, "blah"),
        "Bad Request: Please provide a valid surveyName. null is not valid.");

    assertThrows(
        BadRequestException.class,
        () -> controller.findSurveyAutoComplete(WORKSPACE_NAMESPACE, WORKSPACE_ID, "All", null),
        "Bad Request: Please provide a valid term. blah is not valid.");
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.PHYSICAL_MEASUREMENT_CSS.toString(),
                            false,
                            "12345",
                            null,
                            false,
                            null)
                        .getBody())
                .getItems()
                .get(0));

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.PHYSICAL_MEASUREMENT_CSS.toString(),
                            true,
                            "12345",
                            null,
                            false,
                            null)
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.name(),
                            false,
                            "001",
                            null,
                            false,
                            null)
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
    assertThat(
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.name(),
                            true,
                            "001",
                            null,
                            false,
                            null)
                        .getBody())
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
        Objects.requireNonNull(
                controller
                    .findCriteriaByDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_ID,
                        Domain.CONDITION.name(),
                        false,
                        "00",
                        null,
                        false,
                        null)
                    .getBody())
            .getItems();
    assertThat(1).isEqualTo(results.size());
    assertThat(results.get(0)).isEqualTo(createResponseCriteria(criteria));

    results =
        Objects.requireNonNull(
                controller
                    .findCriteriaByDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_ID,
                        Domain.CONDITION.name(),
                        true,
                        "00",
                        null,
                        false,
                        null)
                    .getBody())
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
        Objects.requireNonNull(
                controller
                    .findCriteriaByDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_ID,
                        Domain.DRUG.name(),
                        true,
                        "672535",
                        null,
                        false,
                        null)
                    .getBody())
            .getItems();
    assertThat(1).isEqualTo(results.size());
    assertThat(results.get(0)).isEqualTo(createResponseCriteria(criteria1));

    results =
        Objects.requireNonNull(
                controller
                    .findCriteriaByDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_ID,
                        Domain.DRUG.name(),
                        false,
                        "672535",
                        null,
                        false,
                        null)
                    .getBody())
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

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.name(),
                            false,
                            "LP12",
                            null,
                            false,
                            null)
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.name(),
                            true,
                            "LP12",
                            null,
                            false,
                            null)
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
    assertThat(
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.name(),
                            false,
                            "LP12",
                            null,
                            false,
                            null)
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.name(),
                            true,
                            "",
                            null,
                            null,
                            null)
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.CONDITION.name(),
                            false,
                            "",
                            null,
                            null,
                            null)
                        .getBody())
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
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.DRUG.name(),
                            true,
                            "LP12",
                            null,
                            false,
                            null)
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(criteria));
    assertThat(
            Objects.requireNonNull(
                    controller
                        .findCriteriaByDomain(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_ID,
                            Domain.DRUG.name(),
                            false,
                            "LP12",
                            null,
                            false,
                            null)
                        .getBody())
                .getItems())
        .isEmpty();
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
            .addFullText(
                "[condition_rank1][procedure_rank1][observation_rank1][measurement_rank1][drug_rank1]")
            .build();
    cbCriteriaDao.save(criteria);
    assertThat(
            Objects.requireNonNull(
                    controller
                        .findStandardCriteriaByDomainAndConceptId(
                            WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.toString(), 12345L)
                        .getBody())
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
            .addStandard(true)
            .addGroup(true)
            .addSelectable(true)
            .addHierarchy(true)
            .addCount(12L)
            .addFullText("drugN*[DRUG_rank1]")
            .build();
    cbCriteriaDao.save(drugATCCriteria);
    DbCriteria drugBrandCriteria =
        DbCriteria.builder()
            .addDomainId(Domain.DRUG.toString())
            .addType(CriteriaType.RXNORM.toString())
            .addParentId(0L)
            .addCode("LP6789")
            .addName("RXName")
            .addConceptId("1235")
            .addStandard(true)
            .addGroup(true)
            .addSelectable(true)
            .addHierarchy(true)
            .addCount(33L)
            .addFullText("rxN*[DRUG_rank1]")
            .build();
    cbCriteriaDao.save(drugBrandCriteria);

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findDrugBrandOrIngredientByValue(
                            WORKSPACE_NAMESPACE, WORKSPACE_ID, "drugN", null)
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(drugATCCriteria));

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findDrugBrandOrIngredientByValue(
                            WORKSPACE_NAMESPACE, WORKSPACE_ID, "rxN", null)
                        .getBody())
                .getItems()
                .get(0))
        .isEqualTo(createResponseCriteria(drugBrandCriteria));

    assertThat(
            Objects.requireNonNull(
                    controller
                        .findDrugBrandOrIngredientByValue(
                            WORKSPACE_NAMESPACE, WORKSPACE_ID, "LP6789", null)
                        .getBody())
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
        Objects.requireNonNull(
                controller
                    .findCriteriaAttributeByConceptId(
                        WORKSPACE_NAMESPACE, WORKSPACE_ID, criteriaAttributeMin.getConceptId())
                    .getBody())
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
    assert demos != null;
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
            .findSurveyVersionByQuestionConceptId(WORKSPACE_NAMESPACE, WORKSPACE_ID, 715713L)
            .getBody();
    assert response != null;
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

  @Disabled(
      "Cannot be tested without SQL/Java modifications for H2. "
          + "(a) Native query and not JPQL and (b) MATCH ... against... not H2 function")
  public void findSurveyCount() {}

  @Disabled(
      "Cannot be tested without underlying CBCriteriaDao's SQL/Java modifications for H2. "
          + "(a) Native query and not JPQL and (b) MATCH ... against... not H2 function")
  public void findEhrDomainCounts() {}

  @Test
  public void findSurveyVersionByQuestionConceptIdAndAnswerConceptId() {
    jdbcTemplate.execute(
        "create table cb_survey_version(survey_version_concept_id integer, survey_concept_id integer, display_name varchar(50), display_order integer)");
    jdbcTemplate.execute(
        "create table cb_survey_attribute(id integer, question_concept_id integer, answer_concept_id integer, survey_version_concept_id integer, item_count integer)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (999, 111, 'May 2020', 1)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (1, 222, 333, 999, 200)");

    List<SurveyVersion> response =
        Objects.requireNonNull(
                controller
                    .findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
                        WORKSPACE_NAMESPACE, WORKSPACE_ID, 222L, 333L)
                    .getBody())
            .getItems();
    assertThat(response.get(0).getSurveyVersionConceptId()).isEqualTo(999);
    assertThat(response.get(0).getDisplayName()).isEqualTo("May 2020");
    assertThat(response.get(0).getItemCount()).isEqualTo(200);

    jdbcTemplate.execute("drop table cb_survey_version");
    jdbcTemplate.execute("drop table cb_survey_attribute");
  }

  @Disabled(
      "Cannot be tested without SQL/Java modifications for H2. "
          + "(a) Native query and not JPQL and (b) MATCH ... against... not H2 function")
  public void findDrugIngredientByConceptId() {}

  @Test
  public void findAgeTypeCounts() {
    int age = 25;
    LocalDate todayDate = LocalDate.now().minusYears(age);
    // birthday-month-date today
    LocalDate dob = todayDate.minusDays(0);
    DbPerson person =
        DbPerson.builder()
            .addPersonId(1000L)
            .addDob(Date.valueOf(dob))
            .addAgeAtCdr(20)
            .addAgeAtConsent(18)
            .addIsDeceased(false)
            .build();
    // assert dob month and date is today's month/date
    assertThat(Period.between(dob, todayDate).getDays()).isEqualTo(0);
    personDao.save(person);

    // birthday-month-date yesterday
    dob = todayDate.minusDays(1);
    person =
        DbPerson.builder()
            .addPersonId(1001L)
            .addDob(Date.valueOf(dob))
            .addAgeAtCdr(20)
            .addAgeAtConsent(18)
            .addIsDeceased(false)
            .build();
    // assert dob month and date is yesterday's month/date
    assertThat(Period.between(dob, todayDate).getDays()).isEqualTo(1);
    personDao.save(person);

    // birthday-month-date tomorrow
    dob = todayDate.plusDays(1);
    person =
        DbPerson.builder()
            .addPersonId(1002L)
            .addDob(Date.valueOf(dob))
            .addAgeAtCdr(20)
            .addAgeAtConsent(18)
            .addIsDeceased(false)
            .build();
    // assert dob month and date is tomorrows's month/date
    assertThat(Period.between(dob, todayDate).getDays()).isEqualTo(-1);
    // save DbPerson entities
    personDao.save(person);

    List<AgeTypeCount> expected = new ArrayList<>();
    // preserve order of output - order by age, count
    expected.add(new AgeTypeCount().ageType("AGE").age(age - 1).count(1L)); // birthday tomorrow
    expected.add(
        new AgeTypeCount().ageType("AGE").age(age).count(2L)); // birthday yesterday and today
    expected.add(new AgeTypeCount().ageType("AGE_AT_CDR").age(20).count(3L));
    expected.add(new AgeTypeCount().ageType("AGE_AT_CONSENT").age(18).count(3L));

    List<AgeTypeCount> response =
        Objects.requireNonNull(
                controller.findAgeTypeCounts(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody())
            .getItems();
    assertThat(response).isEqualTo(expected);
  }

  @Test
  public void validateDomainCaseInsensitive() {
    List<String> values =
        Stream.of(Domain.values()).map(Domain::toString).collect(Collectors.toList());
    for (String value : values) {
      assertDoesNotThrow(
          () -> controller.validateDomain(value),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    // Do not expect exception for lowercase
    values.replaceAll(String::toLowerCase);
    for (String value : values) {
      assertDoesNotThrow(
          () -> controller.validateDomain(value.toLowerCase()),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> controller.validateDomain("BOGUS"),
            "Expected BadRequestException is not thrown.");
    assertThat(exception).hasMessageThat().contains("Please provide a valid domain");
  }

  @Test
  public void validDomainAndSurveyName() {
    // While validateDomain is case-insensitive,
    // this is case sensitive for SURVEY when checking surveyName
    assertDoesNotThrow(
        () -> controller.validateDomain("SURVEY", "some survey name"),
        "BadRequestException is not expected to be thrown for [some survey name]");
    // survey name cannot be null for SURVEY
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> controller.validateDomain("SURVEY", null),
            "Expected BadRequestException is not thrown.");
    assertThat(exception).hasMessageThat().contains("Please provide a valid surveyName");
  }

  @Test
  public void validateType() {
    List<String> values =
        Stream.of(CriteriaType.values()).map(CriteriaType::toString).collect(Collectors.toList());
    for (String value : values) {
      assertDoesNotThrow(
          () -> controller.validateType(value),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    // Do not expect exception for lowercase
    values.replaceAll(String::toLowerCase);
    for (String value : values) {
      assertDoesNotThrow(
          () -> controller.validateType(value.toLowerCase()),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> controller.validateType("BOGUS"),
            "Expected BadRequestException is not thrown.");
    assertThat(exception).hasMessageThat().contains("Please provide a valid type");
  }

  @Test
  public void validateTerm() {
    assertDoesNotThrow(
        () -> controller.validateTerm("non-empty search string"),
        "BadRequestException is not expected to be thrown for non-empty search string");
    // null string
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> controller.validateTerm(null),
            "Expected BadRequestException is not thrown.");
    assertThat(exception).hasMessageThat().contains("Please provide a valid search term");
    // empty string length = 0
    exception =
        assertThrows(
            BadRequestException.class,
            () -> controller.validateTerm(""),
            "Expected BadRequestException is not thrown.");
    assertThat(exception).hasMessageThat().contains("Please provide a valid search term");
    // empty string length > 0
    exception =
        assertThrows(
            BadRequestException.class,
            () -> controller.validateTerm("   "),
            "Expected BadRequestException is not thrown.");
    assertThat(exception).hasMessageThat().contains("Please provide a valid search term");
  }

  @Test
  public void validateAgeTypeCaseSensitive() {
    List<String> values =
        Stream.of(AgeType.values()).map(AgeType::toString).collect(Collectors.toList());
    for (String value : values) {
      assertDoesNotThrow(
          () -> controller.validateAgeType(value),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    // expect exception for lowercase
    values.replaceAll(String::toLowerCase);
    for (String value : values) {
      Throwable exception =
          assertThrows(
              BadRequestException.class,
              () -> controller.validateAgeType(value),
              "Expected BadRequestException is not thrown.");
      assertThat(exception).hasMessageThat().contains("Please provide a valid age type parameter");
    }
  }

  @Test
  public void validateGenderOrSexTypeCaseSensitive() {
    List<String> values =
        Stream.of(GenderOrSexType.values())
            .map(GenderOrSexType::toString)
            .collect(Collectors.toList());
    for (String value : values) {
      assertDoesNotThrow(
          () -> controller.validateGenderOrSexType(value),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    // expect exception for lowercase
    values.replaceAll(String::toLowerCase);
    for (String value : values) {
      Throwable exception =
          assertThrows(
              BadRequestException.class,
              () -> controller.validateGenderOrSexType(value),
              "Expected BadRequestException is not thrown.");
      assertThat(exception)
          .hasMessageThat()
          .contains("Please provide a valid gender or sex at birth parameter");
    }
  }

  @Test
  public void findVersionedSurveys() {
    DbCriteria versionedSurvey =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.SURVEY.toString())
                .addGroup(true)
                .addConceptId("1333342")
                .addStandard(false)
                .addSelectable(true)
                .addName("COVID-19 Participant Experience (COPE) Survey")
                .build());
    jdbcTemplate.execute(
        "create table cb_survey_version(survey_version_concept_id integer, survey_concept_id integer, display_name varchar(50), display_order integer)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (100, 1333342, 'May 2020', 1)");

    List<Criteria> response =
        Objects.requireNonNull(
                controller.findVersionedSurveys(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody())
            .getItems();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.get(0).getId()).isEqualTo(versionedSurvey.getId());

    jdbcTemplate.execute("drop table cb_survey_version");
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

  private void stubBigQueryCallThrowDeadlineExceededException() {
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(null))
        .thenThrow(new DeadlineExceededException());
  }
}
