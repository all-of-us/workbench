package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.ImmutableList;
import jakarta.inject.Provider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.cdr.model.DbPerson;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.cohortbuilder.VariantQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartService;
import org.pmiops.workbench.cohortbuilder.chart.ChartServiceImpl;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaRequest;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.DemoChartInfoListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EthnicityInfo;
import org.pmiops.workbench.model.EthnicityInfoListResponse;
import org.pmiops.workbench.model.GenderSexRaceOrEthType;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.ParticipantCountFilter;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.pmiops.workbench.model.Variant;
import org.pmiops.workbench.model.VariantFilter;
import org.pmiops.workbench.model.VariantFilterInfoResponse;
import org.pmiops.workbench.model.VariantFilterRequest;
import org.pmiops.workbench.model.VariantFilterResponse;
import org.pmiops.workbench.model.VariantListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Import({
  TestJpaConfig.class,
  CohortBuilderControllerBQTest.Configuration.class,
  StorageConfig.class
})
public class CohortBuilderControllerBQTest extends BigQueryBaseTest {

  @TestConfiguration
  @Import({
    BigQueryTestService.class,
    CohortQueryBuilder.class,
    ChartServiceImpl.class,
    ChartQueryBuilder.class,
    CommonMappers.class,
    CohortBuilderServiceImpl.class,
    SearchGroupItemQueryBuilder.class,
    CdrVersionService.class,
    CohortBuilderMapperImpl.class,
    CohortReviewMapperImpl.class,
    VariantQueryBuilder.class
  })
  @MockBean({
    FireCloudService.class,
    AccessTierService.class,
    CdrVersionService.class,
    WorkspaceAuthService.class
  })
  static class Configuration {
    @Bean
    public DbUser user() {
      DbUser user = new DbUser();
      user.setUsername("bob@gmail.com");
      return user;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return WorkbenchConfig.createEmptyConfig();
    }
  }

  private CohortBuilderController controller;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private CohortBuilderService cohortBuilderService;

  @Autowired private ChartService chartService;

  @Autowired private WorkspaceAuthService workspaceAuthService;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private FireCloudService firecloudService;

  @Autowired private PersonDao personDao;

  @Autowired private CBDataFilterDao cbDataFilterDao;

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private Provider<WorkbenchConfig> workbenchConfigProvider;

  private DbCriteria drugNode1;
  private DbCriteria drugNode2;
  private DbCriteria criteriaParent;
  private DbCriteria criteriaChild;
  private DbCriteria icd9;
  private DbCriteria icd10;
  private DbCriteria snomedSource;
  private DbCriteria snomedStandard;
  private DbCriteria cpt4;
  private DbCriteria temporalParent1;
  private DbCriteria temporalChild1;
  private DbCriteria procedureParent1;
  private DbCriteria procedureChild1;
  private DbCriteria surveyNode;
  private DbCriteria questionNode;
  private DbCriteria answerNode;
  private DbPerson dbPerson1;
  private DbPerson dbPerson2;
  private DbPerson dbPerson3;

  private static final String WORKSPACE_ID = "workspaceId";
  private static final String WORKSPACE_NAMESPACE = "workspaceNS";

  @Override
  public List<String> getTableNames() {
    return ImmutableList.of(
        "person",
        "death",
        "cb_search_person",
        "cb_search_all_events",
        "cb_review_all_events",
        "cb_criteria",
        "cb_criteria_ancestor",
        "cb_variant_attribute",
        "cb_variant_attribute_contig_position",
        "cb_variant_attribute_genes",
        "cb_variant_attribute_rs_number",
        "cb_variant_to_person");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @BeforeEach
  public void setUp() {

    when(firecloudService.isUserMemberOfGroupWithCache(anyString(), anyString())).thenReturn(true);

    controller =
        new CohortBuilderController(
            cohortBuilderService, chartService, workbenchConfigProvider, workspaceAuthService);

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(1L);
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    cdrVersion.setAccessTier(accessTierDao.save(createRegisteredTier()));
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    cdrVersion = cdrVersionDao.save(cdrVersion);

    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    dbWorkspace.setName("Saved workspace");
    dbWorkspace.setFirecloudName(WORKSPACE_ID);
    dbWorkspace.setCdrVersion(cdrVersion);

    workspaceDao.save(dbWorkspace);

    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_ID, WorkspaceAccessLevel.READER))
        .thenReturn(dbWorkspace);
    drugNode1 = saveCriteriaWithPath("0", drugCriteriaParent());
    drugNode2 = saveCriteriaWithPath(drugNode1.getPath(), drugCriteriaChild(drugNode1.getId()));

    criteriaParent = saveCriteriaWithPath("0", icd9CriteriaParent());
    criteriaChild =
        saveCriteriaWithPath(criteriaParent.getPath(), icd9CriteriaChild(criteriaParent.getId()));

    icd9 =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD9CM.toString())
                .addStandard(false)
                .addConceptId("44823922")
                .addFullText("+[CONDITION_rank1]")
                .build());
    icd10 =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD10CM.toString())
                .addStandard(false)
                .build());
    snomedStandard =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(true)
                .addConceptId("44823923")
                .addFullText("+[CONDITION_rank1]")
                .build());
    snomedSource =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(false)
                .build());
    cpt4 =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.PROCEDURE.toString())
                .addType(CriteriaType.CPT4.toString())
                .addStandard(false)
                .build());

    temporalParent1 =
        saveCriteriaWithPath(
            "0",
            DbCriteria.builder()
                .addAncestorData(false)
                .addCode("001")
                .addConceptId("0")
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD10CM.toString())
                .addGroup(true)
                .addSelectable(true)
                .addStandard(false)
                .addSynonyms("+[CONDITION_rank1]")
                .build());
    temporalChild1 =
        saveCriteriaWithPath(
            temporalParent1.getPath(),
            temporalChild1 =
                DbCriteria.builder()
                    .addParentId(temporalParent1.getId())
                    .addAncestorData(false)
                    .addCode("001.1")
                    .addConceptId("1")
                    .addDomainId(Domain.CONDITION.toString())
                    .addType(CriteriaType.ICD10CM.toString())
                    .addGroup(false)
                    .addSelectable(true)
                    .addStandard(false)
                    .addSynonyms("+[CONDITION_rank1]")
                    .build());

    procedureParent1 =
        saveCriteriaWithPath(
            "0",
            DbCriteria.builder()
                .addParentId(99999)
                .addDomainId(Domain.PROCEDURE.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(true)
                .addCode("386637004")
                .addName("Obstetric procedure")
                .addCount(36673L)
                .addGroup(true)
                .addSelectable(true)
                .addAncestorData(false)
                .addConceptId("4302541")
                .addSynonyms("+[PROCEDURE_rank1]")
                .build());
    procedureChild1 =
        saveCriteriaWithPath(
            procedureParent1.getPath(),
            DbCriteria.builder()
                .addParentId(procedureParent1.getId())
                .addDomainId(Domain.PROCEDURE.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addStandard(true)
                .addCode("386639001")
                .addName("Termination of pregnancy")
                .addCount(50L)
                .addGroup(false)
                .addSelectable(true)
                .addAncestorData(false)
                .addConceptId("4")
                .addSynonyms("+[PROCEDURE_rank1]")
                .build());

    surveyNode =
        saveCriteriaWithPath(
            "0",
            DbCriteria.builder()
                .addParentId(0)
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.SURVEY.toString())
                .addGroup(true)
                .addSelectable(true)
                .addStandard(false)
                .addConceptId("22")
                .build());
    questionNode =
        saveCriteriaWithPath(
            surveyNode.getPath(),
            DbCriteria.builder()
                .addParentId(surveyNode.getId())
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.QUESTION.toString())
                .addGroup(true)
                .addSelectable(true)
                .addStandard(false)
                .addName("In what country were you born?")
                .addConceptId("1585899")
                .addSynonyms("[SURVEY_rank1]")
                .build());
    answerNode =
        saveCriteriaWithPath(
            questionNode.getPath(),
            DbCriteria.builder()
                .addParentId(questionNode.getId())
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.ANSWER.toString())
                .addGroup(false)
                .addSelectable(true)
                .addStandard(false)
                .addName("USA")
                .addConceptId("5")
                .addSynonyms("[SURVEY_rank1]")
                .build());

    dbPerson1 = personDao.save(DbPerson.builder().addAgeAtConsent(55).addAgeAtCdr(56).build());
    dbPerson2 = personDao.save(DbPerson.builder().addAgeAtConsent(22).addAgeAtCdr(22).build());
    dbPerson3 = personDao.save(DbPerson.builder().addAgeAtConsent(34).addAgeAtCdr(35).build());
    cbDataFilterDao.save(
        DbDataFilter.builder()
            .addDataFilterId(1L)
            .addDisplayName("displayName1")
            .addName("name1")
            .build());
    cbDataFilterDao.save(
        DbDataFilter.builder()
            .addDataFilterId(2L)
            .addDisplayName("displayName2")
            .addName("name2")
            .build());
  }

  @AfterEach
  public void tearDown() {
    delete(
        drugNode1,
        drugNode2,
        criteriaParent,
        criteriaChild,
        icd9,
        icd10,
        snomedSource,
        snomedStandard,
        cpt4,
        temporalParent1,
        temporalChild1,
        procedureParent1,
        procedureChild1,
        surveyNode,
        questionNode,
        answerNode);
    personDao.deleteById(dbPerson1.getPersonId());
    personDao.deleteById(dbPerson2.getPersonId());
    personDao.deleteById(dbPerson3.getPersonId());
  }

  private static SearchParameter icd9() {
    return new SearchParameter()
        .domain(Domain.CONDITION.toString())
        .type(CriteriaType.ICD9CM.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(1L);
  }

  private static SearchParameter icd10() {
    return new SearchParameter()
        .domain(Domain.CONDITION.toString())
        .type(CriteriaType.ICD10CM.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(9L);
  }

  private static SearchParameter snomed() {
    return new SearchParameter()
        .domain(Domain.CONDITION.toString())
        .type(CriteriaType.SNOMED.toString())
        .standard(false)
        .ancestorData(false)
        .group(false)
        .conceptId(4L);
  }

  private static SearchParameter drug() {
    return new SearchParameter()
        .domain(Domain.DRUG.toString())
        .type(CriteriaType.ATC.toString())
        .group(false)
        .ancestorData(true)
        .standard(true)
        .conceptId(11L);
  }

  private static SearchParameter measurement() {
    return new SearchParameter()
        .domain(Domain.MEASUREMENT.toString())
        .type(CriteriaType.LOINC.toString())
        .subtype(CriteriaSubType.LAB.toString())
        .group(false)
        .ancestorData(false)
        .standard(true)
        .conceptId(3L);
  }

  private static SearchParameter visit() {
    return new SearchParameter()
        .domain(Domain.VISIT.toString())
        .type(CriteriaType.VISIT.toString())
        .group(false)
        .ancestorData(false)
        .standard(true)
        .conceptId(1L);
  }

  private static SearchParameter procedure() {
    return new SearchParameter()
        .domain(Domain.PROCEDURE.toString())
        .type(CriteriaType.CPT4.toString())
        .group(false)
        .ancestorData(false)
        .standard(false)
        .conceptId(10L);
  }

  private static SearchParameter bloodPressure() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.BP.toString())
        .ancestorData(false)
        .standard(false)
        .group(false);
  }

  private static SearchParameter hrDetail() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HR_DETAIL.toString())
        .ancestorData(false)
        .standard(false)
        .group(false)
        .conceptId(903126L);
  }

  private static SearchParameter hr() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HR.toString())
        .ancestorData(false)
        .standard(false)
        .group(false)
        .conceptId(1586218L);
  }

  private static SearchParameter height() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HEIGHT.toString())
        .group(false)
        .conceptId(903133L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter weight() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WEIGHT.toString())
        .group(false)
        .conceptId(903121L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter bmi() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.BMI.toString())
        .group(false)
        .conceptId(903124L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter waistCircumference() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WC.toString())
        .group(false)
        .conceptId(903135L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter hipCircumference() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.HC.toString())
        .group(false)
        .conceptId(903136L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter pregnancy() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.PREG.toString())
        .group(false)
        .conceptId(903120L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter wheelchair() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.WHEEL.toString())
        .group(false)
        .conceptId(903111L)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter variant() {
    return new SearchParameter()
        .domain(Domain.SNP_INDEL_VARIANT.toString())
        .ancestorData(false)
        .group(false)
        .variantId("1-101504524-G-A");
  }

  private static SearchParameter variantFilter() {
    VariantFilter variantFilter = new VariantFilter().searchTerm("gene");
    return new SearchParameter()
        .domain(Domain.SNP_INDEL_VARIANT.toString())
        .ancestorData(false)
        .group(false)
        .variantFilter(variantFilter);
  }

  /**
   * This SearchParameter specifically represents the case that uses the
   * has_physical_measurement_data flag.
   */
  private static SearchParameter physicalMeasurementDataNoConceptId() {
    return new SearchParameter()
        .domain(Domain.PHYSICAL_MEASUREMENT.toString())
        .type(CriteriaType.PPI.toString())
        .group(false)
        .ancestorData(false)
        .standard(false);
  }

  private static SearchParameter age() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.AGE.toString())
        .group(false)
        .ancestorData(false)
        .standard(true);
  }

  private static SearchParameter male() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.GENDER.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(8507L);
  }

  private static SearchParameter race() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.RACE.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(1L);
  }

  private static SearchParameter ethnicity() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.ETHNICITY.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(9898L);
  }

  private static SearchParameter selfReportedPopulation() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.SELF_REPORTED_POPULATION.toString())
        .group(false)
        .standard(true)
        .ancestorData(false)
        .conceptId(23455L);
  }

  private static SearchParameter deceased() {
    return new SearchParameter()
        .domain(Domain.PERSON.toString())
        .type(CriteriaType.DECEASED.toString())
        .group(false)
        .standard(true)
        .ancestorData(false);
  }

  private static SearchParameter fitbit(Domain domain) {
    return new SearchParameter()
        .domain(domain.toString())
        .group(false)
        .standard(true)
        .ancestorData(false);
  }

  private static SearchParameter wholeGenomeVariant() {
    return new SearchParameter()
        .domain(Domain.WHOLE_GENOME_VARIANT.toString())
        .group(false)
        .standard(false)
        .ancestorData(false);
  }

  private static SearchParameter longReadWholeGenomeVariant() {
    return new SearchParameter()
        .domain(Domain.LR_WHOLE_GENOME_VARIANT.toString())
        .group(false)
        .standard(false)
        .ancestorData(false);
  }

  private static SearchParameter arrayData() {
    return new SearchParameter()
        .domain(Domain.ARRAY_DATA.toString())
        .group(false)
        .standard(false)
        .ancestorData(false);
  }

  private static SearchParameter structuralVariantData() {
    return new SearchParameter()
        .domain(Domain.STRUCTURAL_VARIANT_DATA.toString())
        .group(false)
        .standard(false)
        .ancestorData(false);
  }

  private static SearchParameter survey() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.SURVEY.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(22L);
  }

  private static SearchParameter surveyAnswer() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.ANSWER.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(5L);
  }

  private static SearchParameter copeSurveyQuestion() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101"))));
  }

  private static SearchParameter copeSurveyQuestionAnyValue() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(ImmutableList.of(new Attribute().name(AttrName.ANY)));
  }

  private static SearchParameter copeSurveyQuestionVersionAndAnyValue() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute().name(AttrName.ANY),
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101"))));
  }

  private static SearchParameter copeSurveyAnswer() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101")),
                new Attribute()
                    .name(AttrName.CAT)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("10"))));
  }

  private static SearchParameter copeSurveyCatAndNum() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.QUESTION.toString())
        .ancestorData(false)
        .standard(false)
        .group(true)
        .conceptId(44L)
        .attributes(
            ImmutableList.of(
                new Attribute()
                    .name(AttrName.SURVEY_VERSION_CONCEPT_ID)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("100", "101")),
                new Attribute()
                    .name(AttrName.CAT)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("10")),
                new Attribute()
                    .name(AttrName.NUM)
                    .operator(Operator.EQUAL)
                    .operands(ImmutableList.of("10"))));
  }

  private static SearchParameter pfhhSurveyAnswer() {
    return new SearchParameter()
        .domain(Domain.SURVEY.toString())
        .type(CriteriaType.PPI.toString())
        .subtype(CriteriaSubType.ANSWER.toString())
        .ancestorData(false)
        .standard(false)
        .group(false)
        .conceptId(43528652L)
        .attributes(
            ImmutableList.of(
                new Attribute()
                    .name(AttrName.PERSONAL_FAMILY_HEALTH_HISTORY)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("1740639")),
                new Attribute()
                    .name(AttrName.CAT)
                    .operator(Operator.IN)
                    .operands(ImmutableList.of("43528385"))));
  }

  private static Modifier ageModifier() {
    return new Modifier()
        .name(ModifierType.AGE_AT_EVENT)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(ImmutableList.of("25"));
  }

  private static Modifier visitModifier() {
    return new Modifier()
        .name(ModifierType.ENCOUNTERS)
        .operator(Operator.IN)
        .operands(ImmutableList.of("1"));
  }

  private static Modifier occurrencesModifier() {
    return new Modifier()
        .name(ModifierType.NUM_OF_OCCURRENCES)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(ImmutableList.of("1"));
  }

  private static Modifier eventDateModifier() {
    return new Modifier()
        .name(ModifierType.EVENT_DATE)
        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
        .operands(ImmutableList.of("2009-12-03"));
  }

  private static Modifier catiModifier() {
    return new Modifier()
        .name(ModifierType.CATI)
        .operator(Operator.EQUAL)
        .operands(ImmutableList.of("42530794"));
  }

  private static List<Attribute> wheelchairAttributes() {
    return ImmutableList.of(
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(ImmutableList.of("4023190")));
  }

  private static List<Attribute> bpAttributes() {
    return ImmutableList.of(
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.LESS_THAN_OR_EQUAL_TO)
            .operands(ImmutableList.of("90"))
            .conceptId(903118L),
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.BETWEEN)
            .operands(ImmutableList.of("60", "80"))
            .conceptId(903115L));
  }

  private static DbCriteria drugCriteriaParent() {
    return DbCriteria.builder()
        .addParentId(99999)
        .addDomainId(Domain.DRUG.toString())
        .addType(CriteriaType.ATC.toString())
        .addConceptId("21600932")
        .addGroup(true)
        .addStandard(true)
        .addSelectable(true)
        .build();
  }

  private static DbCriteria drugCriteriaChild(long parentId) {
    return DbCriteria.builder()
        .addParentId(parentId)
        .addDomainId(Domain.DRUG.toString())
        .addType(CriteriaType.RXNORM.toString())
        .addConceptId("1520218")
        .addGroup(false)
        .addSelectable(true)
        .build();
  }

  private static DbCriteria icd9CriteriaParent() {
    return DbCriteria.builder()
        .addParentId(99999)
        .addDomainId(Domain.CONDITION.toString())
        .addType(CriteriaType.ICD9CM.toString())
        .addStandard(false)
        .addCode("001")
        .addName("Cholera")
        .addCount(19L)
        .addGroup(true)
        .addSelectable(true)
        .addAncestorData(false)
        .addConceptId("2")
        .addSynonyms("[CONDITION_rank1]")
        .build();
  }

  private static DbCriteria icd9CriteriaChild(long parentId) {
    return DbCriteria.builder()
        .addParentId(parentId)
        .addDomainId(Domain.CONDITION.toString())
        .addType(CriteriaType.ICD9CM.toString())
        .addStandard(false)
        .addCode("001.1")
        .addName("Cholera")
        .addCount(19L)
        .addGroup(false)
        .addSelectable(true)
        .addAncestorData(false)
        .addConceptId("1")
        .build();
  }

  @Test
  public void getCohortChartDataLab() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                Domain.LAB.name(),
                createCohortDefinition(
                    Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>()))
            .getBody();
    List<CohortChartData> items = Objects.requireNonNull(response).getItems();
    assertThat(items.size()).isEqualTo(3);
    assertThat(items.get(0))
        .isEqualTo(new CohortChartData().name("name10").conceptId(10L).count(1L));
    assertThat(items.get(1)).isEqualTo(new CohortChartData().name("name3").conceptId(3L).count(1L));
    assertThat(items.get(2)).isEqualTo(new CohortChartData().name("name9").conceptId(9L).count(1L));
  }

  @Test
  public void getCohortChartDataDrug() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                Domain.DRUG.name(),
                createCohortDefinition(
                    Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>()))
            .getBody();
    List<CohortChartData> items = Objects.requireNonNull(response).getItems();
    assertThat(items.size()).isEqualTo(1);
    assertThat(items.get(0))
        .isEqualTo(new CohortChartData().name("name11").conceptId(1L).count(1L));
  }

  @Test
  public void getCohortChartDataCondition() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                Domain.CONDITION.name(),
                createCohortDefinition(
                    Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>()))
            .getBody();
    List<CohortChartData> items = Objects.requireNonNull(response).getItems();
    assertThat(items.size()).isEqualTo(2);
    assertThat(items.get(0)).isEqualTo(new CohortChartData().name("name1").conceptId(1L).count(1L));
    assertThat(items.get(1)).isEqualTo(new CohortChartData().name("name7").conceptId(7L).count(1L));
  }

  @Test
  public void getCohortChartDataProcedure() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                Domain.PROCEDURE.name(),
                createCohortDefinition(
                    Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>()))
            .getBody();

    List<CohortChartData> items = Objects.requireNonNull(response).getItems();
    assertThat(items.size()).isEqualTo(3);
    assertThat(items.get(0)).isEqualTo(new CohortChartData().name("name2").conceptId(2L).count(1L));
    assertThat(items.get(1)).isEqualTo(new CohortChartData().name("name4").conceptId(4L).count(1L));
    assertThat(items.get(2)).isEqualTo(new CohortChartData().name("name8").conceptId(8L).count(1L));
  }

  @Test
  public void findDataFilters() {
    List<DataFilter> filters =
        Objects.requireNonNull(
                controller.findDataFilters(WORKSPACE_NAMESPACE, WORKSPACE_ID).getBody())
            .getItems();
    assertThat(
            filters.contains(
                new DataFilter().dataFilterId(1L).displayName("displayName1").name("name1")))
        .isTrue();
    assertThat(
            filters.contains(
                new DataFilter().dataFilterId(2L).displayName("displayName2").name("name2")))
        .isTrue();
  }

  @Test
  public void countParticipantsSearchGroupNoItems() {
    List<SearchGroup> groups = new ArrayList<>();
    groups.add(new SearchGroup().id("sg1"));
    CohortDefinition cohortDefinition = new CohortDefinition().includes(groups);
    assertThrows(
        BadRequestException.class,
        () -> controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition));
  }

  @Test
  public void countParticipantsSearchGroupNoSearchParameters() {
    final SearchGroupItem searchGroupItem =
        new SearchGroupItem()
            .id("sgi1")
            .type(Domain.PERSON.toString())
            .searchParameters(new ArrayList<>())
            .modifiers(new ArrayList<>());

    final SearchGroup searchGroup = new SearchGroup().addItemsItem(searchGroupItem);

    List<SearchGroup> groups = new ArrayList<>();
    groups.add(searchGroup);
    CohortDefinition cohortDefinition = new CohortDefinition().includes(groups);
    assertThrows(
        BadRequestException.class,
        () -> controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition));
  }

  @Test
  public void countParticipantsVariantData() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SNP_INDEL_VARIANT.toString(), ImmutableList.of(variant()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsVariantDataUsingVariantFilter() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SNP_INDEL_VARIANT.toString(),
            ImmutableList.of(variantFilter()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsVariantDataUsingVidAndVariantFilter() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SNP_INDEL_VARIANT.toString(),
            ImmutableList.of(variant(), variantFilter()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsVariantDataUsingVidAndVariantFilters() {
    VariantFilter variantFilter = new VariantFilter().searchTerm("gene");
    variantFilter
        .addClinicalSignificanceListItem("pathogenic")
        .addConsequenceListItem("intron_variant")
        .countMax(0L)
        .countMax(5L);
    SearchParameter sp =
        new SearchParameter()
            .domain(Domain.SNP_INDEL_VARIANT.toString())
            .ancestorData(false)
            .group(false)
            .variantFilter(variantFilter);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SNP_INDEL_VARIANT.toString(),
            ImmutableList.of(variant(), sp),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsVariantDataUsingTwoVariantFilters() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SNP_INDEL_VARIANT.toString(),
            ImmutableList.of(variantFilter(), variantFilter()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsVariantDataUsingVariantFilterWithExcludeList() {
    VariantFilter variantFilter = new VariantFilter().searchTerm("gene5");
    variantFilter.addExclusionListItem("22-100550658-T-EF");
    SearchParameter sp =
        new SearchParameter()
            .domain(Domain.SNP_INDEL_VARIANT.toString())
            .ancestorData(false)
            .group(false)
            .variantFilter(variantFilter);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SNP_INDEL_VARIANT.toString(), ImmutableList.of(sp), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChild() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasEHRData() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    cohortDefinition.addDataFiltersItem("HAS_EHR_DATA");
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasPMData() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    cohortDefinition.addDataFiltersItem("HAS_PHYSICAL_MEASUREMENT_DATA");
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasPPISurveyData() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    cohortDefinition.addDataFiltersItem("HAS_PPI_SURVEY_DATA");
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildHasEHRAndPPISurveyData() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    cohortDefinition.addDataFiltersItem("HAS_EHR_DATA").addDataFiltersItem("HAS_PPI_SURVEY_DATA");
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void firstMentionOfICD9WithModifiersOrSnomed5DaysAfterICD10WithModifiers() {
    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd9())
            .temporalGroup(0)
            .addModifiersItem(ageModifier());
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1)
            .addModifiersItem(visitModifier());
    SearchGroupItem snomedSGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(snomed())
            .temporalGroup(0);

    // First Mention Of (ICD9 w/modifiers or Snomed) 5 Days After ICD10 w/modifiers
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(icd9SGI, snomedSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    // matches icd9SGI in group 0 and icd10SGI in group 1
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsForSurveyWithAgeModifiers() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(),
            ImmutableList.of(survey().conceptId(1585899L)),
            ImmutableList.of(ageModifier()));

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsForPFHHSurveyWithCatiModifiers() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(),
            ImmutableList.of(pfhhSurveyAnswer()),
            ImmutableList.of(catiModifier()));

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void firstMentionOfDrug5DaysBeforeICD10WithModifiers() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug().conceptId(21600932L))
            .temporalGroup(0);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1)
            .addModifiersItem(visitModifier());

    // First Mention Of Drug 5 Days Before ICD10 w/modifiers
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_BEFORE)
            .timeValue(5L);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void anyMentionOfCPT5DaysAfterICD10Child() {
    SearchGroupItem cptSGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(
                icd9().type(CriteriaType.CPT4.toString()).group(false).conceptId(1L))
            .temporalGroup(0)
            .addModifiersItem(visitModifier());
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd10())
            .temporalGroup(1);

    // Any Mention Of CPT 5 Days After ICD10
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(cptSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.ANY_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void anyMentionOfCPTWithIn5DaysOfVisit() {
    SearchGroupItem cptSGI =
        new SearchGroupItem()
            .type(Domain.PROCEDURE.toString())
            .addSearchParametersItem(procedure())
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(Domain.VISIT.toString())
            .addSearchParametersItem(visit())
            .temporalGroup(1);

    // Any Mention Of ICD10 Parent within 5 Days of visit
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(visitSGI, cptSGI))
            .temporal(true)
            .mention(TemporalMention.ANY_MENTION)
            .time(TemporalTime.WITHIN_X_DAYS_OF)
            .timeValue(5L);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void firstMentionOfDrugDuringSameEncounterAsMeasurement() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(1);

    // First Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurement() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(1);

    // Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void lastMentionOfDrugDuringSameEncounterAsMeasurementOrVisit() {
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug())
            .temporalGroup(0);
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(1);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(Domain.VISIT.toString())
            .addSearchParametersItem(visit())
            .temporalGroup(1);

    // Last Mention Of Drug during same encounter as measurement
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI, visitSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.DURING_SAME_ENCOUNTER_AS);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void lastMentionOfMeasurementOrVisit5DaysAfterDrug() {
    SearchGroupItem measurementSGI =
        new SearchGroupItem()
            .type(Domain.MEASUREMENT.toString())
            .addSearchParametersItem(measurement())
            .temporalGroup(0);
    SearchGroupItem visitSGI =
        new SearchGroupItem()
            .type(Domain.VISIT.toString())
            .addSearchParametersItem(visit())
            .temporalGroup(0);
    SearchGroupItem drugSGI =
        new SearchGroupItem()
            .type(Domain.DRUG.toString())
            .addSearchParametersItem(drug().group(true).conceptId(21600932L))
            .temporalGroup(1);

    // Last Mention Of Measurement or Visit 5 days after Drug
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(ImmutableList.of(drugSGI, measurementSGI, visitSGI))
            .temporal(true)
            .mention(TemporalMention.LAST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);

    CohortDefinition cohortDefinition =
        new CohortDefinition().includes(ImmutableList.of(temporalGroup));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEvent() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), ImmutableList.of(ageModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEncounter() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(visitModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEventBetween() {
    Modifier modifier =
        ageModifier().operator(Operator.BETWEEN).operands(ImmutableList.of("37", "39"));

    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), ImmutableList.of(modifier));

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(ageModifier(), occurrencesModifier().operands(ImmutableList.of("2"))));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildAgeAtEventAndOccurrencesAndEventDate() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9(), icd9().conceptId(2L)),
            ImmutableList.of(ageModifier(), occurrencesModifier(), eventDateModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionChildEventDate() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(eventDateModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionNumOfOccurrences() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9()),
            ImmutableList.of(occurrencesModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9Child() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(), ImmutableList.of(icd9()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsICD9ConditionParent() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9().group(true).conceptId(44823922L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDemoGender() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PERSON.toString(), ImmutableList.of(male()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsDemoRace() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PERSON.toString(), ImmutableList.of(race()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsDemoEthnicity() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PERSON.toString(), ImmutableList.of(ethnicity()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsDemoSelfReportedPopulation() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PERSON.toString(),
            ImmutableList.of(selfReportedPopulation()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsDemoDec() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PERSON.toString(), ImmutableList.of(deceased()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsFitbit() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.FITBIT.toString(), ImmutableList.of(fitbit(Domain.FITBIT)), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsFitbitActivity() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.FITBIT_ACTIVITY.toString(),
            ImmutableList.of(fitbit(Domain.FITBIT_ACTIVITY)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsFitbitHeartRateLevel() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.FITBIT_HEART_RATE_LEVEL.toString(),
            ImmutableList.of(fitbit(Domain.FITBIT_HEART_RATE_LEVEL)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsFitbitHeartRateSummary() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.FITBIT_HEART_RATE_SUMMARY.toString(),
            ImmutableList.of(fitbit(Domain.FITBIT_HEART_RATE_SUMMARY)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsFitbitSleepDailySummary() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.FITBIT_SLEEP_DAILY_SUMMARY.toString(),
            ImmutableList.of(fitbit(Domain.FITBIT_SLEEP_DAILY_SUMMARY)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsFitbitIntradaySteps() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.FITBIT_INTRADAY_STEPS.toString(),
            ImmutableList.of(fitbit(Domain.FITBIT_INTRADAY_STEPS)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsFitbitSleepLevel() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.FITBIT_SLEEP_LEVEL.toString(),
            ImmutableList.of(fitbit(Domain.FITBIT_SLEEP_LEVEL)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsWearConsent() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.WEAR_CONSENT.toString(),
            ImmutableList.of(fitbit(Domain.WEAR_CONSENT)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsWholeGenomeVariant() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.WHOLE_GENOME_VARIANT.toString(),
            ImmutableList.of(wholeGenomeVariant()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsLongReadWholeGenomeVariant() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.LR_WHOLE_GENOME_VARIANT.toString(),
            ImmutableList.of(longReadWholeGenomeVariant()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsArrayData() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.ARRAY_DATA.toString(), ImmutableList.of(arrayData()), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countParticipantsStructuralVariantData() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.STRUCTURAL_VARIANT_DATA.toString(),
            ImmutableList.of(structuralVariantData()),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsDemoAge() {
    int lo = getTestPeriod().getYears() - 1;
    int hi = getTestPeriod().getYears() + 1;
    SearchParameter demo = age();
    demo.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of(String.valueOf(lo), String.valueOf(hi)))));
    CohortDefinition cohortDefinition =
        createCohortDefinition(Domain.PERSON.toString(), ImmutableList.of(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsDemoAgeAtConsent() {
    SearchParameter demo = age();
    demo.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE_AT_CONSENT)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of("29", "30"))));
    CohortDefinition cohortDefinition =
        createCohortDefinition(Domain.PERSON.toString(), ImmutableList.of(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 3);
  }

  @Test
  public void countSubjectsDemoAgeAtCdr() {
    SearchParameter demo = age();
    demo.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE_AT_CDR)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of("29", "30"))));
    CohortDefinition cohortDefinition =
        createCohortDefinition(Domain.PERSON.toString(), ImmutableList.of(demo), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsICD9AndDemo() {
    SearchParameter demoAgeSearchParam = age();
    int lo = getTestPeriod().getYears() - 1;
    int hi = getTestPeriod().getYears() + 1;

    demoAgeSearchParam.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.AGE)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of(String.valueOf(lo), String.valueOf(hi)))));

    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PERSON.toString(), ImmutableList.of(male()), new ArrayList<>());

    SearchGroupItem anotherSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .searchParameters(ImmutableList.of(icd9().conceptId(3L)))
            .modifiers(new ArrayList<>());

    SearchGroupItem anotherNewSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PERSON.toString())
            .searchParameters(ImmutableList.of(demoAgeSearchParam))
            .modifiers(new ArrayList<>());

    cohortDefinition.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);
    cohortDefinition.getIncludes().get(0).addItemsItem(anotherNewSearchGroupItem);

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsDemoExcluded() {
    SearchGroupItem excludeSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PERSON.toString())
            .searchParameters(ImmutableList.of(male()));
    SearchGroup excludeSearchGroup = new SearchGroup().addItemsItem(excludeSearchGroupItem);

    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PERSON.toString(), ImmutableList.of(male()), new ArrayList<>());
    cohortDefinition.getExcludes().add(excludeSearchGroup);

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 0);
  }

  @Test
  public void countSubjectsICD9ParentAndICD10ChildCondition() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(icd9().group(true).conceptId(2L), icd10().conceptId(6L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 2);
  }

  @Test
  public void countSubjectsCPTProcedure() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PROCEDURE.toString(),
            ImmutableList.of(procedure().conceptId(4L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsSnomedChildCondition() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.CONDITION.toString(),
            ImmutableList.of(snomed().standard(true).conceptId(6L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsSnomedParentProcedure() {
    SearchParameter snomed = snomed().group(true).standard(true).conceptId(4302541L);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PROCEDURE.toString(), ImmutableList.of(snomed), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsVisit() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.VISIT.toString(), ImmutableList.of(visit().conceptId(10L)), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsVisitModifiers() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.VISIT.toString(),
            ImmutableList.of(visit().conceptId(10L)),
            ImmutableList.of(occurrencesModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsDrugChild() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(Domain.DRUG.toString(), ImmutableList.of(drug()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugParent() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.DRUG.toString(),
            ImmutableList.of(drug().group(true).conceptId(21600932L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugParentAndChild() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.DRUG.toString(),
            ImmutableList.of(drug().group(true).conceptId(21600932L), drug()),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugChildEncounter() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.DRUG.toString(), ImmutableList.of(drug()), ImmutableList.of(visitModifier()));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsDrugChildAgeAtEvent() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.DRUG.toString(), ImmutableList.of(drug()), ImmutableList.of(ageModifier()));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsLabEncounter() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.MEASUREMENT.toString(),
            ImmutableList.of(measurement()),
            ImmutableList.of(visitModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsLabNumericalBetween() {
    SearchParameter lab = measurement();
    lab.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.BETWEEN)
                .operands(ImmutableList.of("0", "1"))));
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.MEASUREMENT.toString(), ImmutableList.of(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsLabCategoricalIn() {
    SearchParameter lab = measurement();
    lab.attributes(
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("1"))));
    CohortDefinition cohortDefinition =
        createCohortDefinition(lab.getDomain(), ImmutableList.of(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsLabBothNumericalAndCategorical() {
    SearchParameter lab = measurement();
    Attribute numerical =
        new Attribute()
            .name(AttrName.NUM)
            .operator(Operator.EQUAL)
            .operands(ImmutableList.of("0.1"));
    Attribute categorical =
        new Attribute()
            .name(AttrName.CAT)
            .operator(Operator.IN)
            .operands(ImmutableList.of("1", "2"));
    lab.attributes(ImmutableList.of(numerical, categorical));
    CohortDefinition cohortDefinition =
        createCohortDefinition(lab.getDomain(), ImmutableList.of(lab), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsLabCategoricalAgeAtEvent() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.MEASUREMENT.toString(),
            ImmutableList.of(measurement()),
            ImmutableList.of(ageModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameter() {
    SearchParameter lab1 = measurement();
    SearchParameter lab2 = measurement().conceptId(9L);
    SearchParameter lab3 = measurement().conceptId(11L);
    Attribute labCategorical =
        new Attribute().name(AttrName.CAT).operator(Operator.IN).operands(ImmutableList.of("77"));
    lab3.attributes(ImmutableList.of(labCategorical));
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            lab1.getDomain(), ImmutableList.of(lab1, lab2, lab3), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsLabMoreThanOneSearchParameterSourceAndStandard() {
    SearchParameter icd9 = icd9();
    SearchParameter snomed = snomed().standard(true);
    CohortDefinition cohortDefinition =
        createCohortDefinition(icd9.getDomain(), ImmutableList.of(icd9, snomed), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsBloodPressure() {
    SearchParameter pm = bloodPressure().attributes(bpAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(pm.getDomain(), ImmutableList.of(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsBloodPressureAny() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903118L),
            new Attribute().name(AttrName.ANY).operands(new ArrayList<>()).conceptId(903115L));
    SearchParameter pm = bloodPressure().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetail() {
    SearchParameter bpPm = bloodPressure().attributes(bpAttributes());

    List<Attribute> hrAttributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("71")));
    SearchParameter hrPm = hrDetail().attributes(hrAttributes);

    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(),
            ImmutableList.of(bpPm, hrPm),
            new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsBloodPressureOrHeartRateDetailOrHeartRateIrr() {
    SearchParameter bpPm = bloodPressure().attributes(bpAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(bpPm), new ArrayList<>());

    List<Attribute> hrAttributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("71")));
    SearchParameter hrPm = hrDetail().attributes(hrAttributes);
    SearchGroupItem anotherSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PHYSICAL_MEASUREMENT.toString())
            .searchParameters(ImmutableList.of(hrPm))
            .modifiers(new ArrayList<>());

    cohortDefinition.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

    List<Attribute> irrAttributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("4262985")));
    SearchParameter hrIrrPm = hr().attributes(irrAttributes);
    SearchGroupItem heartRateIrrSearchGroupItem =
        new SearchGroupItem()
            .type(Domain.PHYSICAL_MEASUREMENT.toString())
            .searchParameters(ImmutableList.of(hrIrrPm))
            .modifiers(new ArrayList<>());

    cohortDefinition.getIncludes().get(0).addItemsItem(heartRateIrrSearchGroupItem);

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 3);
  }

  @Test
  public void countSubjectsHeartRateAny() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(),
            ImmutableList.of(hrDetail().conceptId(1586218L)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsHeartRate() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("45")));
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(),
            ImmutableList.of(hrDetail().conceptId(1586218L).attributes(attributes)),
            new ArrayList<>());
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectsHeight() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("168")));
    SearchParameter pm = height().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsWeight() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("201")));
    SearchParameter pm = weight().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsBMI() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                .operands(ImmutableList.of("263")));
    SearchParameter pm = bmi().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectWaistCircumferenceAndHipCircumference() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("31")));
    SearchParameter pm = waistCircumference().attributes(attributes);
    SearchParameter pm1 = hipCircumference().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm, pm1), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectPregnant() {
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("45877994")));
    SearchParameter pm = pregnancy().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectWheelChairUser() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 2);
  }

  @Test
  public void countSubjectHasPhysicalMeasurementData() {
    SearchParameter pm = physicalMeasurementDataNoConceptId();
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsSurveyAll() {
    // Survey
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(),
            ImmutableList.of(survey().conceptId(1585899L)),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 2);
  }

  @Test
  public void countSubjectsSurveyCatiModifier() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(),
            ImmutableList.of(survey().conceptId(1585899L)),
            ImmutableList.of(catiModifier()));
    assertParticipants(
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition), 1);
  }

  @Test
  public void countSubjectsCopeSurveyQuestion() {
    // Cope Survey
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(), ImmutableList.of(copeSurveyQuestion()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyQuestionVersionAndAnyValue() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(),
            ImmutableList.of(copeSurveyQuestionVersionAndAnyValue()),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyQuestionAnyValue() {
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(),
            ImmutableList.of(copeSurveyQuestionAnyValue()),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyAnswer() {
    // Cope Survey
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(), ImmutableList.of(copeSurveyAnswer()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsCopeSurveyCatAndNum() {
    // Cope Survey
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.SURVEY.toString(), ImmutableList.of(copeSurveyCatAndNum()), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsQuestionAll() {
    // Question
    SearchParameter ppiQuestion =
        survey().subtype(CriteriaSubType.QUESTION.toString()).conceptId(1585899L);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            ppiQuestion.getDomain(), ImmutableList.of(ppiQuestion), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 2);
  }

  @Test
  public void countSubjectsQuestionCatiModifier() {
    // Question
    SearchParameter ppiQuestion =
        survey().subtype(CriteriaSubType.QUESTION.toString()).conceptId(1585899L);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            ppiQuestion.getDomain(),
            ImmutableList.of(ppiQuestion),
            ImmutableList.of(catiModifier()));
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsSurveyValueSourceConceptId() {
    // value source concept id
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.CAT)
                .operator(Operator.IN)
                .operands(ImmutableList.of("7")));
    SearchParameter ppiValueAsConceptId = surveyAnswer().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            ppiValueAsConceptId.getDomain(),
            ImmutableList.of(ppiValueAsConceptId),
            new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void countSubjectsSurveyValueAsNumber() {
    // value as number
    List<Attribute> attributes =
        ImmutableList.of(
            new Attribute()
                .name(AttrName.NUM)
                .operator(Operator.EQUAL)
                .operands(ImmutableList.of("7")));
    SearchParameter ppiValueAsNumber = surveyAnswer().attributes(attributes);
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            ppiValueAsNumber.getDomain(), ImmutableList.of(ppiValueAsNumber), new ArrayList<>());
    ResponseEntity<Long> response =
        controller.countParticipants(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition);
    assertParticipants(response, 1);
  }

  @Test
  public void findDemoChartInfo() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                GenderSexRaceOrEthType.GENDER.toString(),
                AgeType.AGE.toString(),
                cohortDefinition)
            .getBody();
    assertDemographics(Objects.requireNonNull(response));
  }

  @Test
  public void findEthnicityInfo() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    EthnicityInfoListResponse response =
        controller.findEthnicityInfo(WORKSPACE_NAMESPACE, WORKSPACE_ID, cohortDefinition).getBody();
    assertEthnicity(Objects.requireNonNull(response));
  }

  @Test
  public void findDemoChartInfoGenderAgeAtConsentWithEHRData() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());
    cohortDefinition.addDataFiltersItem("HAS_EHR_DATA");

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                GenderSexRaceOrEthType.GENDER.toString(),
                AgeType.AGE_AT_CONSENT.toString(),
                cohortDefinition)
            .getBody();
    assertDemographics(Objects.requireNonNull(response));
  }

  @Test
  public void findDemoChartInfoSexAtBirthAgeAtCdr() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                GenderSexRaceOrEthType.SEX_AT_BIRTH.toString(),
                AgeType.AGE_AT_CDR.toString(),
                cohortDefinition)
            .getBody();
    assertDemographics(Objects.requireNonNull(response));
  }

  @Test
  public void findDemoChartInfoRaceAgeAtCdr() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                GenderSexRaceOrEthType.RACE.toString(),
                AgeType.AGE_AT_CDR.toString(),
                cohortDefinition)
            .getBody();
    assertRace(Objects.requireNonNull(response));
  }

  @Test
  public void findDemoChartInfoEthnicityAgeAtCdr() {
    SearchParameter pm = wheelchair().attributes(wheelchairAttributes());
    CohortDefinition cohortDefinition =
        createCohortDefinition(
            Domain.PHYSICAL_MEASUREMENT.toString(), ImmutableList.of(pm), new ArrayList<>());

    DemoChartInfoListResponse response =
        controller
            .findDemoChartInfo(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                GenderSexRaceOrEthType.ETHNICITY.toString(),
                AgeType.AGE_AT_CDR.toString(),
                cohortDefinition)
            .getBody();
    assertEthnicityDemographics(Objects.requireNonNull(response));
  }

  @Test
  public void findCriteriaForCohortEdit() {
    ImmutableList<Long> sourceConceptIds = ImmutableList.of(44823922L);
    ImmutableList<Long> standardConceptIds = ImmutableList.of(44823923L);
    CriteriaRequest request =
        new CriteriaRequest()
            .sourceConceptIds(sourceConceptIds)
            .standardConceptIds(standardConceptIds);
    List<Criteria> criteriaList =
        Objects.requireNonNull(
                controller
                    .findCriteriaForCohortEdit(
                        WORKSPACE_NAMESPACE, WORKSPACE_ID, Domain.CONDITION.toString(), request)
                    .getBody())
            .getItems();
    assertThat(criteriaList).hasSize(2);
    assertThat(criteriaList.get(0).getId()).isEqualTo(icd9.getId());
    assertThat(criteriaList.get(1).getId()).isEqualTo(snomedStandard.getId());
  }

  @Test
  public void filterBigQueryConfig_WithoutTableName() {
    final String statement = "my statement ${projectId}.${dataSetId}.myTableName";
    QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(statement).setUseLegacySql(false).build();
    final String expectedResult = "my statement " + getTablePrefix() + ".myTableName";
    assertThat(expectedResult)
        .isEqualTo(bigQueryService.filterBigQueryConfig(queryJobConfiguration).getQuery());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1-101504524-G-A", "gene", "chr20:955-1000", "rs23346"})
  public void findVariants(String searchTerm) {
    VariantFilterRequest request = new VariantFilterRequest();
    request.searchTerm(searchTerm);
    Variant expectedVariant =
        new Variant()
            .vid("1-101504524-G-A")
            .gene("gene, gene2")
            .consequence("intron_variant, non_coding_transcript_variant")
            .proteinChange("change")
            .clinVarSignificance("likely pathogenic, pathogenic")
            .alleleCount(5L)
            .alleleNumber(18242L)
            .alleleFrequency(0.000277)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByConsequence() {
    VariantFilterRequest request = new VariantFilterRequest();
    request
        .searchTerm("gene")
        .addConsequenceListItem("intron_variant")
        .addConsequenceListItem("non_coding_transcript_variant");
    Variant expectedVariant =
        new Variant()
            .vid("1-101504524-G-A")
            .gene("gene, gene2")
            .consequence("intron_variant, non_coding_transcript_variant")
            .proteinChange("change")
            .clinVarSignificance("likely pathogenic, pathogenic")
            .alleleCount(5L)
            .alleleNumber(18242L)
            .alleleFrequency(0.000277)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByParticipantCount() {
    VariantFilterRequest request = new VariantFilterRequest();
    ParticipantCountFilter pcf =
        new ParticipantCountFilter().operator(Operator.LESS_THAN).operands(ImmutableList.of(2000));
    request.searchTerm("gene4").participantCountRange(pcf);
    Variant expectedVariant =
        new Variant()
            .vid("2-100550658-T-CC")
            .gene("gene4")
            .consequence("")
            .clinVarSignificance("")
            .proteinChange("change")
            .alleleCount(7L)
            .alleleNumber(18226L)
            .alleleFrequency(0.000266)
            .participantCount(100L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByConsequenceNA() {
    VariantFilterRequest request = new VariantFilterRequest();
    request
        .searchTerm("gene3")
        .addConsequenceListItem("n/a")
        .addConsequenceListItem("intron_variant");
    Variant expectedVariant =
        new Variant()
            .vid("1-100550658-T-AA")
            .gene("gene3")
            .consequence("")
            .proteinChange("change")
            .clinVarSignificance("")
            .alleleCount(7L)
            .alleleNumber(18226L)
            .alleleFrequency(0.000266)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByClinicalSignificance() {
    VariantFilterRequest request = new VariantFilterRequest();
    request
        .searchTerm("gene")
        .addClinicalSignificanceListItem("likely pathogenic")
        .addClinicalSignificanceListItem("pathogenic");
    Variant expectedVariant =
        new Variant()
            .vid("1-101504524-G-A")
            .gene("gene, gene2")
            .consequence("intron_variant, non_coding_transcript_variant")
            .proteinChange("change")
            .clinVarSignificance("likely pathogenic, pathogenic")
            .alleleCount(5L)
            .alleleNumber(18242L)
            .alleleFrequency(0.000277)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByClinicalSignificanceNA() {
    VariantFilterRequest request = new VariantFilterRequest();
    request
        .searchTerm("gene3")
        .addClinicalSignificanceListItem("n/a")
        .addClinicalSignificanceListItem("pathogenic");
    Variant expectedVariant =
        new Variant()
            .vid("1-100550658-T-AA")
            .gene("gene3")
            .consequence("")
            .proteinChange("change")
            .clinVarSignificance("")
            .alleleCount(7L)
            .alleleNumber(18226L)
            .alleleFrequency(0.000266)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByGeneList() {
    VariantFilterRequest request = new VariantFilterRequest();
    request.searchTerm("gene").addGeneListItem("gene, gene2");
    Variant expectedVariant =
        new Variant()
            .vid("1-101504524-G-A")
            .gene("gene, gene2")
            .consequence("intron_variant, non_coding_transcript_variant")
            .proteinChange("change")
            .clinVarSignificance("likely pathogenic, pathogenic")
            .alleleCount(5L)
            .alleleNumber(18242L)
            .alleleFrequency(0.000277)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByGenesListNA() {
    VariantFilterRequest request = new VariantFilterRequest();
    request.searchTerm("rs23347").addGeneListItem("n/a");
    Variant expectedVariant =
        new Variant()
            .vid("1-100550658-T-AH")
            .consequence("")
            .proteinChange("change")
            .clinVarSignificance("")
            .alleleCount(7L)
            .alleleNumber(18226L)
            .alleleFrequency(0.000266)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsFilterByMinAndMax() {
    VariantFilterRequest request = new VariantFilterRequest();
    request
        .searchTerm("gene")
        .countMin(4L)
        .countMax(5L)
        .numberMin(18242L)
        .numberMax(18245L)
        .frequencyMin(new BigDecimal("0.000277").setScale(6, RoundingMode.HALF_UP))
        .frequencyMax(new BigDecimal(1).setScale(6, RoundingMode.HALF_UP));
    Variant expectedVariant =
        new Variant()
            .vid("1-101504524-G-A")
            .gene("gene, gene2")
            .consequence("intron_variant, non_coding_transcript_variant")
            .proteinChange("change")
            .clinVarSignificance("likely pathogenic, pathogenic")
            .alleleCount(5L)
            .alleleNumber(18242L)
            .alleleFrequency(0.000277)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 1);
  }

  @Test
  public void findVariantsSortByAlleleCount() {
    VariantFilterRequest request = new VariantFilterRequest();
    request.searchTerm("gene1");
    request.pageSize(1).sortBy("Allele Count");
    Variant expectedVariant =
        new Variant()
            .vid("1-100550658-T-H")
            .gene("gene1")
            .consequence("intron_variant, non_coding_transcript_variant")
            .proteinChange("change")
            .clinVarSignificance("likely pathogenic, pathogenic")
            .alleleCount(7L)
            .alleleNumber(18226L)
            .alleleFrequency(0.000266)
            .participantCount(1L);
    assertFindVariantsResponse(request, expectedVariant, 2);
  }

  private void assertFindVariantsResponse(
      VariantFilterRequest request, Variant expectedVariant, int totalSize) {
    VariantListResponse response =
        controller.findVariants(WORKSPACE_NAMESPACE, WORKSPACE_ID, request).getBody();
    List<Variant> items = Objects.requireNonNull(response).getItems();
    assertThat(response.getTotalSize()).isEqualTo(totalSize);
    if (totalSize > 1) {
      assertThat(response.getNextPageToken()).isNotNull();
    } else {
      assertThat(response.getNextPageToken()).isNull();
    }
    assertThat(items.size()).isEqualTo(1);
    assertThat(items.get(0)).isEqualTo(expectedVariant);
  }

  @Test
  public void findVariants_Pagination() {
    VariantFilterRequest request = new VariantFilterRequest();
    request.searchTerm("gene1");
    request.pageSize(1);
    VariantListResponse response =
        controller.findVariants(WORKSPACE_NAMESPACE, WORKSPACE_ID, request).getBody();
    List<Variant> items = Objects.requireNonNull(response).getItems();
    assertThat(response.getTotalSize()).isEqualTo(2);
    assertThat(response.getNextPageToken()).isNotNull();
    assertThat(items.size()).isEqualTo(1);
    assertThat(items.get(0))
        .isEqualTo(
            new Variant()
                .vid("1-100550658-T-C")
                .gene("gene1")
                .consequence("intron_variant, non_coding_transcript_variant")
                .proteinChange("change")
                .clinVarSignificance("likely pathogenic, pathogenic")
                .alleleCount(22L)
                .alleleNumber(18226L)
                .alleleFrequency(0.000266)
                .participantCount(1L));

    response =
        controller
            .findVariants(
                WORKSPACE_NAMESPACE,
                WORKSPACE_ID,
                request.pageToken(response.getNextPageToken()).pageSize(1))
            .getBody();
    items = Objects.requireNonNull(response).getItems();
    assertThat(response.getTotalSize()).isEqualTo(2);
    assertThat(response.getNextPageToken()).isNull();
    assertThat(items.size()).isEqualTo(1);
    assertThat(items.get(0))
        .isEqualTo(
            new Variant()
                .vid("1-100550658-T-H")
                .gene("gene1")
                .consequence("intron_variant, non_coding_transcript_variant")
                .proteinChange("change")
                .clinVarSignificance("likely pathogenic, pathogenic")
                .alleleCount(7L)
                .alleleNumber(18226L)
                .alleleFrequency(0.000266)
                .participantCount(1L));
  }

  @Test
  public void findVariantFilters() {
    VariantFilterRequest request = new VariantFilterRequest();
    request.searchTerm("chr20:1000-5000");
    VariantFilterResponse expectedVariantFilter = new VariantFilterResponse();
    expectedVariantFilter
        .geneList(Arrays.asList("gene, gene2", "gene3"))
        .consequenceList(Arrays.asList("intron_variant", "n/a", "non_coding_transcript_variant"))
        .clinicalSignificanceList(Arrays.asList("likely pathogenic", "n/a", "pathogenic"))
        .countMin(5L)
        .countMax(7L)
        .numberMin(18226L)
        .numberMax(18242L)
        .frequencyMin(new BigDecimal(0))
        .frequencyMax(new BigDecimal(1));
    expectedVariantFilter.sortByList(VariantQueryBuilder.VatColumns.getDisplayNameList());
    assertFindVariantFiltersResponse(request, expectedVariantFilter);
  }

  private void assertFindVariantFiltersResponse(
      VariantFilterRequest request, VariantFilterResponse expectedVariantFilter) {
    VariantFilterResponse response =
        controller.findVariantFilters(WORKSPACE_NAMESPACE, WORKSPACE_ID, request).getBody();
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(expectedVariantFilter);
  }

  @Test
  public void findVariantFilterInfo() {
    VariantFilter filter = new VariantFilter();
    filter.searchTerm("gene5");

    VariantFilterInfoResponse response =
        controller.findVariantFilterInfo(WORKSPACE_NAMESPACE, WORKSPACE_ID, filter).getBody();
    assertThat(response).isNotNull();
    assertThat(response.getVidsCount()).isEqualTo(2);
    assertThat(response.getParticipantCount()).isEqualTo(2);
    assertThat(response.getLessThanOrEqualToFiveThousand()).isEqualTo(1);
    assertThat(response.getOverFiveThousand()).isEqualTo(1);
    assertThat(response.getOverTenThousand()).isEqualTo(0);
    assertThat(response.getOverHundredThousand()).isEqualTo(0);
    assertThat(response.getOverTwoHundredThousand()).isEqualTo(0);
  }

  @Test
  public void findVariantFilterInfoParticipantCountLessThan5KNoBuckets() {
    VariantFilter filter = new VariantFilter();
    filter
        .searchTerm("gene4")
        .participantCountRange(
            new ParticipantCountFilter()
                .operator(Operator.LESS_THAN)
                .operands(ImmutableList.of(5000)));

    VariantFilterInfoResponse response =
        controller.findVariantFilterInfo(WORKSPACE_NAMESPACE, WORKSPACE_ID, filter).getBody();
    assertThat(response).isNotNull();
    assertThat(response.getVidsCount()).isEqualTo(2);
    assertThat(response.getParticipantCount()).isEqualTo(1);
    assertThat(response.getLessThanOrEqualToFiveThousand()).isEqualTo(0);
    assertThat(response.getOverFiveThousand()).isEqualTo(0);
    assertThat(response.getOverTenThousand()).isEqualTo(0);
    assertThat(response.getOverHundredThousand()).isEqualTo(0);
    assertThat(response.getOverTwoHundredThousand()).isEqualTo(0);
  }

  protected String getTablePrefix() {
    DbCdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
    return cdrVersion.getBigqueryProject() + "." + cdrVersion.getBigqueryDataset();
  }

  private Period getTestPeriod() {
    LocalDate birthDate = LocalDate.of(1980, 8, 1);
    return Period.between(birthDate, LocalDate.now());
  }

  private CohortDefinition createCohortDefinition(
      String type, List<SearchParameter> parameters, List<Modifier> modifiers) {
    final SearchGroupItem searchGroupItem =
        new SearchGroupItem().type(type).searchParameters(parameters).modifiers(modifiers);

    final SearchGroup searchGroup = new SearchGroup().addItemsItem(searchGroupItem);

    List<SearchGroup> groups = new ArrayList<>();
    groups.add(searchGroup);
    return new CohortDefinition().includes(groups);
  }

  private void assertParticipants(ResponseEntity<Long> response, Integer expectedCount) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Long participantCount = response.getBody();
    assertThat(participantCount).isEqualTo(expectedCount);
  }

  private DbCriteria saveCriteriaWithPath(String path, DbCriteria criteria) {
    String pathEnd = String.valueOf(cbCriteriaDao.save(criteria).getId());
    criteria.setPath(path.isEmpty() ? pathEnd : path + "." + pathEnd);
    return cbCriteriaDao.save(criteria);
  }

  private void delete(DbCriteria... criteriaList) {
    Arrays.stream(criteriaList).forEach(c -> cbCriteriaDao.deleteById(c.getId()));
  }

  private void assertDemographics(DemoChartInfoListResponse response) {
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(
            response
                .getItems()
                .contains(new DemoChartInfo().name("MALE").ageRange("45-64").count(1L)))
        .isTrue();
    assertThat(
            response
                .getItems()
                .contains(new DemoChartInfo().name("MALE").ageRange("18-44").count(1L)))
        .isTrue();
  }

  private void assertRace(DemoChartInfoListResponse response) {
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(
            response
                .getItems()
                .contains(new DemoChartInfo().name("Asian").ageRange("45-64").count(1L)))
        .isTrue();
    assertThat(
            response
                .getItems()
                .contains(new DemoChartInfo().name("Caucasian").ageRange("18-44").count(1L)))
        .isTrue();
  }

  private void assertEthnicityDemographics(DemoChartInfoListResponse response) {
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(
            response
                .getItems()
                .contains(
                    new DemoChartInfo().name("Not Hispanic or Latino").ageRange("45-64").count(1L)))
        .isTrue();
    assertThat(
            response
                .getItems()
                .contains(
                    new DemoChartInfo().name("Not Hispanic or Latino").ageRange("18-44").count(1L)))
        .isTrue();
  }

  private void assertEthnicity(EthnicityInfoListResponse response) {
    assertThat(response.getItems().size()).isEqualTo(1);
    assertThat(new EthnicityInfo().ethnicity("Not Hispanic or Latino").count(2L))
        .isEqualTo(response.getItems().get(0));
  }
}
