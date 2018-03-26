package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({QueryBuilderFactory.class, BigQueryService.class, CohortBuilderController.class,
        ParticipantCounter.class, DomainLookupService.class, TestJpaConfig.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    private static final String TYPE_ICD9 = "ICD9";
    private static final String SUBTYPE_NONE = null;
    private static final String TYPE_ICD10 = "ICD10";
    private static final String TYPE_CPT = "CPT";
    private static final String SUBTYPE_CPT4 = "CPT4";
    private static final String SUBTYPE_ICD10CM = "ICD10CM";
    private static final String SUBTYPE_ICD10PCS = "ICD10PCS";
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
                "death");
    }

    @Before
    public void setUp() {
        cdrVersion = new CdrVersion();
        cdrVersion.setCdrVersionId(1L);
        cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
        cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
        CdrVersionContext.setCdrVersion(cdrVersion);

        cdrVersionDao.save(cdrVersion);

        icd9ConditionParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE,"001"));
        icd9ConditionChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9ConditionParent.getId(), "001", DOMAIN_CONDITION));
        icd9ProcedureParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE,"002"));
        icd9ProcedureChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9ProcedureParent.getId(), "002", DOMAIN_PROCEDURE));
        icd9MeasurementParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE,"003"));
        icd9MeasurementChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9MeasurementParent.getId(), "003", DOMAIN_MEASUREMENT));
        icd10ConditionParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10CM,"A"));
        icd10ConditionChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10CM, icd10ConditionParent.getId(), "A09", DOMAIN_CONDITION));
        icd10ProcedureParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10PCS,"16"));
        icd10ProcedureChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10PCS, icd10ProcedureParent.getId(), "16070", DOMAIN_PROCEDURE));
        icd10MeasurementParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10CM,"R92"));
        icd10MeasurementChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10CM, icd10MeasurementParent.getId(), "R92.2", DOMAIN_MEASUREMENT));
        cptProcedure = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001T", DOMAIN_PROCEDURE));
        cptObservation = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001Z", DOMAIN_OBSERVATION));
        cptMeasurement = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001Q", DOMAIN_MEASUREMENT));
        cptDrug = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "90703", DOMAIN_DRUG));
    }

    @Test
    public void countSubjectsICD9ConditionOccurrenceChild() throws Exception {
        SearchParameter icd9 = createSearchParameter(icd9ConditionChild, "001.1");
        SearchRequest searchRequest = createSearchRequests(icd9ConditionChild.getType(), Arrays.asList(icd9));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD9ConditionOccurrenceParent() throws Exception {
        SearchParameter icd9 = createSearchParameter(icd9ConditionParent, "001");
        SearchRequest searchRequest = createSearchRequests(icd9ConditionParent.getType(), Arrays.asList(icd9));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD9ProcedureOccurrenceChild() throws Exception {
        SearchParameter icd9 = createSearchParameter(icd9ProcedureChild, "002.1");
        SearchRequest searchRequest = createSearchRequests(icd9ProcedureChild.getType(), Arrays.asList(icd9));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD9ProcedureOccurrenceParent() throws Exception {
        SearchParameter icd9 = createSearchParameter(icd9ProcedureParent, "002");
        SearchRequest searchRequest = createSearchRequests(icd9ProcedureParent.getType(), Arrays.asList(icd9));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD9MeasurementChild() throws Exception {
        SearchParameter icd9 = createSearchParameter(icd9MeasurementChild, "003.1");
        SearchRequest searchRequest = createSearchRequests(icd9MeasurementChild.getType(), Arrays.asList(icd9));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD9MeasurementParent() throws Exception {
        SearchParameter icd9 = createSearchParameter(icd9MeasurementParent, "003");
        SearchRequest searchRequest = createSearchRequests(icd9MeasurementParent.getType(), Arrays.asList(icd9));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsDemoGender() throws Exception {
        Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
        SearchParameter demo = createSearchParameter(demoGender, null);
        SearchRequest searchRequest = createSearchRequests(demoGender.getType(), Arrays.asList(demo));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsDemoDec() throws Exception {
        Criteria demoGender = createDemoCriteria("DEMO", "DEC", null);
        SearchParameter demo = createSearchParameter(demoGender, "Deceased");
        SearchRequest searchRequest = createSearchRequests(demoGender.getType(), Arrays.asList(demo));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsDemoAge() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        Integer age = Period.between(birthdate, now).getYears();
        Criteria demoAge = createDemoCriteria("DEMO", "AGE", null);
        SearchParameter demo = createSearchParameter(demoAge, null);
        demo.attribute(new Attribute().operator("=").operands(Arrays.asList(age.toString())));
        SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demo));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests),1);
    }

    @Test
    public void countSubjectsDemoGenderAndAge() throws Exception {
        Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
        SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        Integer age = Period.between(birthdate, now).getYears();
        Criteria demoAge = createDemoCriteria("DEMO", "AGE", null);
        SearchParameter demoAgeSearchParam = createSearchParameter(demoAge, null);
        demoAgeSearchParam.attribute(new Attribute().operator("=").operands(Arrays.asList(age.toString())));

        SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demoGenderSearchParam, demoAgeSearchParam));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests),1);
    }

    @Test
    public void countSubjectsICD9AndDemo() throws Exception {
        Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
        SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        Integer age = Period.between(birthdate, now).getYears();
        Criteria demoAge = createDemoCriteria("DEMO", "AGE", null);
        SearchParameter demoAgeSearchParam = createSearchParameter(demoAge, null);
        demoAgeSearchParam.attribute(new Attribute().operator("=").operands(Arrays.asList(age.toString())));

        SearchRequest searchRequests = createSearchRequests(demoAge.getType(), Arrays.asList(demoGenderSearchParam, demoAgeSearchParam));

        SearchParameter icd9 = createSearchParameter(icd9MeasurementChild, "003.1");
        SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type(icd9.getType()).searchParameters(Arrays.asList(icd9));

        searchRequests.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

        assertParticipants( controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 1);
    }

    @Test
    public void countSubjectsDemoExcluded() throws Exception {
        Criteria demoGender = createDemoCriteria("DEMO", "GEN", "8507");
        SearchParameter demoGenderSearchParam = createSearchParameter(demoGender, null);

        SearchParameter demoGenderSearchParamExclude = createSearchParameter(demoGender, null);

        SearchGroupItem excludeSearchGroupItem = new SearchGroupItem().type(demoGender.getType())
                .searchParameters(Arrays.asList(demoGenderSearchParamExclude));
        SearchGroup excludeSearchGroup = new SearchGroup().addItemsItem(excludeSearchGroupItem);

        SearchRequest searchRequests = createSearchRequests(demoGender.getType(), Arrays.asList(demoGenderSearchParam));
        searchRequests.getExcludes().add(excludeSearchGroup);

        assertParticipants( controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequests), 0);
    }

    @Test
    public void countSubjectsICD10ConditionOccurrenceChild() throws Exception {
        SearchParameter icd10 = createSearchParameter(icd10ConditionChild, "A09");
        SearchRequest searchRequest = createSearchRequests(icd10ConditionChild.getType(), Arrays.asList(icd10));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD10ConditionOccurrenceParent() throws Exception {
        SearchParameter icd10 = createSearchParameter(icd10ConditionParent, "A");
        SearchRequest searchRequest = createSearchRequests(icd10ConditionParent.getType(), Arrays.asList(icd10));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD10ProcedureOccurrenceChild() throws Exception {
        SearchParameter icd10 = createSearchParameter(icd10ProcedureChild, "16070");
        SearchRequest searchRequest = createSearchRequests(icd10ProcedureChild.getType(), Arrays.asList(icd10));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD10ProcedureOccurrenceParent() throws Exception {
        SearchParameter icd10 = createSearchParameter(icd10ProcedureParent, "16");
        SearchRequest searchRequest = createSearchRequests(icd10ProcedureParent.getType(), Arrays.asList(icd10));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD10MeasurementChild() throws Exception {
        SearchParameter icd10 = createSearchParameter(icd10MeasurementChild, "R92.2");
        SearchRequest searchRequest = createSearchRequests(icd10MeasurementChild.getType(), Arrays.asList(icd10));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsICD10MeasurementParent() throws Exception {
        SearchParameter icd10 = createSearchParameter(icd10MeasurementParent, "R92");
        SearchRequest searchRequest = createSearchRequests(icd10MeasurementParent.getType(), Arrays.asList(icd10));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsCPTProcedureOccurrence() throws Exception {
        SearchParameter cpt = createSearchParameter(cptProcedure, "0001T");
        SearchRequest searchRequest = createSearchRequests(cptProcedure.getType(), Arrays.asList(cpt));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsCPTObservation() throws Exception {
        SearchParameter cpt = createSearchParameter(cptObservation, "0001Z");
        SearchRequest searchRequest = createSearchRequests(cptObservation.getType(), Arrays.asList(cpt));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsCPTMeasurement() throws Exception {
        SearchParameter cpt = createSearchParameter(cptMeasurement, "0001Q");
        SearchRequest searchRequest = createSearchRequests(cptMeasurement.getType(), Arrays.asList(cpt));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjectsCPTDrugExposure() throws Exception {
        SearchParameter cpt = createSearchParameter(cptDrug, "90703");
        SearchRequest searchRequest = createSearchRequests(cptDrug.getType(), Arrays.asList(cpt));
        assertParticipants(controller.countParticipants(cdrVersion.getCdrVersionId(), searchRequest),1);
    }

    @Test
    public void countSubjects_EmptyIcludesAndExcludes() throws Exception {
        try {
            controller.countParticipants(1L, new SearchRequest());
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

    private SearchParameter createSearchParameter(Criteria criteria, String code) {
        return new SearchParameter()
                .type(criteria.getType())
                .subtype(criteria.getSubtype())
                .group(criteria.getGroup())
                .value(code)
                .domain(criteria.getDomainId())
                .conceptId(criteria.getConceptId() == null ? null : new Long(criteria.getConceptId()));
    }

    private SearchRequest createSearchRequests(String type, List<SearchParameter> parameters) {
        final SearchGroupItem searchGroupItem = new SearchGroupItem().type(type).searchParameters(parameters);

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
