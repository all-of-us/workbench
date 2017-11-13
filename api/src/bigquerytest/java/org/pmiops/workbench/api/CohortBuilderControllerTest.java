package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.SubjectCounter;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.ChartInfo;
import org.pmiops.workbench.model.ChartInfoListResponse;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.inject.Provider;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({QueryBuilderFactory.class, CohortBuilderController.class, SubjectCounter.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    private CohortBuilderController controller;

    @Autowired
    private BigQuery bigquery;

    @Autowired
    private SubjectCounter subjectCounter;

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfig;

    @Mock
    private CriteriaDao mockCriteriaDao;

    @Override
    public List<String> getTableNames() {
        return Arrays.asList(
                "criteria",
                "person",
                "concept",
                "condition_occurrence",
                "procedure_occurrence",
                "measurement",
                "observation",
                "drug_exposure",
                "phecode_criteria_icd",
                "concept_relationship");
    }

    @Before
    public void setUp() {
        this.controller = new CohortBuilderController(bigquery, subjectCounter,
                workbenchConfig, mockCriteriaDao);
    }

    @Test
    public void getCriteriaByTypeAndParentId_Icd9() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("ICD9")
                        .code("001-139.99")
                        .name("Infectious and parasitic diseases")
                        .group(false)
                        .selectable(false)
                        .count("0")
                        .conceptId("0");

        when(mockCriteriaDao
                .findCriteriaByTypeAndParentId("ICD9", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId("ICD9", 0L),
                new Criteria()
                        .id(1L)
                        .type("ICD9")
                        .code("001-139.99")
                        .name("Infectious and parasitic diseases")
                        .group(false)
                        .selectable(false)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCriteriaDao).findCriteriaByTypeAndParentId("ICD9", 0L);
        verifyNoMoreInteractions(mockCriteriaDao);
    }

    @Test
    public void getCriteriaByTypeAndParentId_demo() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("DEMO_AGE")
                        .name("Age")
                        .group(false)
                        .selectable(true)
                        .count("0")
                        .conceptId("12");

        when(mockCriteriaDao
                .findCriteriaByTypeAndParentId("DEMO", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId("DEMO", 0L),
                new Criteria()
                        .id(1L)
                        .type("DEMO_AGE")
                        .name("Age")
                        .group(false)
                        .selectable(true)
                        .count(0L)
                        .conceptId(12L));

        verify(mockCriteriaDao).findCriteriaByTypeAndParentId("DEMO", 0L);
        verifyNoMoreInteractions(mockCriteriaDao);
    }

    @Test
    public void getCriteriaByTypeAndParentId_icd10() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("ICD10")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count("0")
                        .conceptId("0");

        when(mockCriteriaDao
                .findCriteriaByTypeAndParentId("ICD10", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId("ICD10", 0L),
                new Criteria()
                        .id(1L)
                        .type("ICD10")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCriteriaDao).findCriteriaByTypeAndParentId("ICD10", 0L);
        verifyNoMoreInteractions(mockCriteriaDao);
    }

    @Test
    public void getCriteriaByTypeAndParentId_CPT() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("CPT")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count("0")
                        .conceptId("0");

        when(mockCriteriaDao
                .findCriteriaByTypeAndParentId("CPT", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId("CPT", 0L),
                new Criteria()
                        .id(1L)
                        .type("CPT")
                        .name("DIAGNOSIS CODES")
                        .group(true)
                        .selectable(true)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCriteriaDao).findCriteriaByTypeAndParentId("CPT", 0L);
        verifyNoMoreInteractions(mockCriteriaDao);
    }

    @Test
    public void getCriteriaByTypeAndParentId_Phecodes() throws Exception {
        org.pmiops.workbench.cdr.model.Criteria expectedCriteria =
                new org.pmiops.workbench.cdr.model.Criteria()
                        .id(1L)
                        .type("PHECODE")
                        .name("Intestinal infection")
                        .group(true)
                        .selectable(true)
                        .count("0")
                        .conceptId("0");

        when(mockCriteriaDao
                .findCriteriaByTypeAndParentId("PHECODE", 0L))
                .thenReturn(Arrays.asList(expectedCriteria));

        assertCriteria(
                controller.getCriteriaByTypeAndParentId("PHECODE", 0L),
                new Criteria()
                        .id(1L)
                        .type("PHECODE")
                        .name("Intestinal infection")
                        .group(true)
                        .selectable(true)
                        .count(0L)
                        .conceptId(0L));

        verify(mockCriteriaDao).findCriteriaByTypeAndParentId("PHECODE", 0L);
        verifyNoMoreInteractions(mockCriteriaDao);
    }

    @Test
    public void countSubjects_ICD9ConditionOccurrenceLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD9", Arrays.asList(new SearchParameter().value("001.1").domain("Condition")))),
                1);
    }

    @Test
    public void countSubjects_ICD9ConditionOccurrenceParent() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD9", Arrays.asList(new SearchParameter().value("001")))),
                1);
    }

    @Test
    public void countSubjects_ICD9ProcedureOccurrenceLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD9", Arrays.asList(new SearchParameter().value("002.1").domain("Procedure")))),
                1);
    }

    @Test
    public void countSubjects_ICD9ProcedureOccurrenceParent() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD9", Arrays.asList(new SearchParameter().value("002")))),
                        1);
    }

    @Test
    public void countSubjects_ICD9MeasurementLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD9", Arrays.asList(new SearchParameter().value("003.1").domain("Measurement")))),
                        1);
    }

    @Test
    public void countSubjects_ICD9MeasurementParent() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD9", Arrays.asList(new SearchParameter().value("003")))),
                        1);
    }

    @Test
    public void countSubjects_DemoGender() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("DEMO", Arrays.asList(new SearchParameter().domain("DEMO").conceptId(8507L).subtype("GEN")))),
                        1);
    }

    @Test
    public void countSubjects_DemoAge() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        Integer age = Period.between(birthdate, now).getYears();
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("DEMO", Arrays.asList(new SearchParameter().value(String.valueOf(age)).domain("DEMO").subtype("AGE")
                                .attribute(new Attribute().operator("=").operands(Arrays.asList(age.toString())))))),
                        1);
    }

    @Test
    public void countSubjects_DemoGenderAndAge() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        Integer age = Period.between(birthdate, now).getYears();
        SearchParameter ageParameter = new SearchParameter().value(String.valueOf(age)).domain("DEMO").subtype("AGE")
                .attribute(new Attribute().operator("=").operands(Arrays.asList(age.toString())));
        SearchParameter genderParameter = new SearchParameter().domain("DEMO").conceptId(8507L).subtype("GEN");
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("DEMO", Arrays.asList(ageParameter, genderParameter))),
                        1);
    }

    @Test
    public void countSubjects_ICD9_Demo_SameSearchGroup() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        Integer age = Period.between(birthdate, now).getYears();

        SearchParameter ageParameter = new SearchParameter().value(String.valueOf(age)).domain("DEMO").subtype("AGE")
                .attribute(new Attribute().operator("=").operands(Arrays.asList(age.toString())));
        SearchParameter genderParameter = new SearchParameter().domain("DEMO").conceptId(8507L).subtype("GEN");

        SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type("ICD9")
                .searchParameters(Arrays.asList(new SearchParameter().value("003.1").domain("Measurement")));

        SearchRequest testSearchRequest = createSearchRequests("DEMO", Arrays.asList(ageParameter, genderParameter));
        testSearchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

        assertSubjects( controller.countSubjects(testSearchRequest), 1);
    }

    @Test
    public void countSubjects_ICD9_Demo_DiffSearchGroup() throws Exception {
        SearchParameter genderParameter = new SearchParameter().domain("DEMO").conceptId(8507L).subtype("GEN");

        SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type("ICD9")
                .searchParameters(Arrays.asList(new SearchParameter().value("003.1").domain("Measurement")));
        SearchGroup anotherSearchGroup = new SearchGroup().addItemsItem(anotherSearchGroupItem);

        SearchRequest testSearchRequest = createSearchRequests("DEMO", Arrays.asList(genderParameter));
        testSearchRequest.getIncludes().add(anotherSearchGroup);

        assertSubjects( controller.countSubjects(testSearchRequest), 1);
    }

    @Test
    public void countSubjects_DemoExcluded() throws Exception {
        SearchParameter genderParameter = new SearchParameter().domain("DEMO").conceptId(8507L).subtype("GEN");

        SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type("DEMO")
                .searchParameters(Arrays.asList(new SearchParameter().domain("DEMO").conceptId(8507L).subtype("GEN")));
        SearchGroup anotherSearchGroup = new SearchGroup().addItemsItem(anotherSearchGroupItem);

        SearchRequest testSearchRequest = createSearchRequests("DEMO", Arrays.asList(genderParameter));
        testSearchRequest.getExcludes().add(anotherSearchGroup);

        assertSubjects( controller.countSubjects(testSearchRequest), 0);
    }

    @Test
    public void countSubjects_ICD10ConditionOccurrenceLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD10", Arrays.asList(new SearchParameter().value("A09").domain("Condition")))),
                1);
    }

    @Test
    public void countSubjects_ICD10ConditionOccurrenceParent() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD10", Arrays.asList(new SearchParameter().value("C00")))),
                1);
    }

    @Test
    public void countSubjects_ICD10ProcedureOccurrenceLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD10", Arrays.asList(new SearchParameter().value("16070").domain("Procedure")))),
                1);
    }

    @Test
    public void countSubjects_ICD10MeasurementLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("ICD10", Arrays.asList(new SearchParameter().value("R92.2").domain("Measurement")))),
                1);
    }

    @Test
    public void countSubjects_CPTProcedureOccurrenceLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("CPT", Arrays.asList(new SearchParameter().value("0001T").domain("Procedure")))),
                1);
    }

    @Test
    public void countSubjects_CPTObservationLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("CPT", Arrays.asList(new SearchParameter().value("0001Z").domain("Observation")))),
                1);
    }

    @Test
    public void countSubjects_CPTMeasurementLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("CPT", Arrays.asList(new SearchParameter().value("0001Q").domain("Measurement")))),
                1);
    }

    @Test
    public void countSubjects_CPTDrugExposureLeaf() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("CPT", Arrays.asList(new SearchParameter().value("90703").domain("Drug")))),
                1);
    }

    @Test
    public void countSubjects_EmptyIcludesAndExcludes() throws Exception {
        try {
            controller.countSubjects(new SearchRequest());
        } catch (BadRequestException e) {
            assertEquals("Invalid SearchRequest: includes[] and excludes[] cannot both be empty", e.getMessage());
        }
    }

    @Test
    public void countSubjects_PheCodes() throws Exception {
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("PHECODE", Arrays.asList(new SearchParameter().value("008")))),
                1);
    }

    @Test
    public void getChartInfo_PheCodes() throws Exception {
        assertChartInfoCounts(
                controller.getChartInfo(
                        createSearchRequests("PHECODE", Arrays.asList(new SearchParameter().value("008")))),
                "F", "Unknown", "> 65", 1);
    }

    @Test
    public void filterBigQueryConfig_WithoutTableName() throws Exception {
        final String statement = "my statement ${projectId}.${dataSetId}.myTableName";
        QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration.newBuilder(statement).setUseLegacySql(false).build();
        final String expectedResult = "my statement " + getTablePrefix() + ".myTableName";
        assertThat(expectedResult).isEqualTo(controller.filterBigQueryConfig(queryJobConfiguration).getQuery());
    }

    protected String getTablePrefix() {
        return workbenchConfig.get().bigquery.projectId + "." + workbenchConfig.get().bigquery.dataSetId;
    }

    private void assertCriteria(ResponseEntity response, Criteria expectedCriteria) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CriteriaListResponse listResponse = (CriteriaListResponse) response.getBody();
        assertThat(listResponse.getItems().get(0)).isEqualTo(expectedCriteria);
    }

    private SearchRequest createSearchRequests(String type, List<SearchParameter> parameters) {
        final SearchGroupItem searchGroupItem = new SearchGroupItem().type(type).searchParameters(parameters);

        final SearchGroup searchGroup = new SearchGroup().addItemsItem(searchGroupItem);

        List<SearchGroup> groups = new ArrayList<>();
        groups.add(searchGroup);
        return new SearchRequest().includes(groups);
    }

    private void assertSubjects(ResponseEntity response, Integer expectedCount) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Long subjectCount = (Long) response.getBody();
        assertThat(subjectCount).isEqualTo(expectedCount);
    }

    private void assertChartInfoCounts(ResponseEntity response, String gender, String race, String ageRange, int count) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ChartInfoListResponse chartInfoResponse = (ChartInfoListResponse) response.getBody();
        final ChartInfo chartInfo = chartInfoResponse.getItems().get(0);
        assertThat(chartInfo.getGender()).isEqualTo(gender);
        assertThat(chartInfo.getRace()).isEqualTo(race);
        assertThat(chartInfo.getAgeRange()).isEqualTo(ageRange);
        assertThat(chartInfo.getCount()).isEqualTo(count);
    }
}
