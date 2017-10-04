package org.pmiops.workbench.api;

import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SubjectListResponse;
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

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({QueryBuilderFactory.class, CohortBuilderController.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    @Autowired
    CohortBuilderController controller;

    @Override
    public List<String> getTableNames() {
        return Arrays.asList(
                "icd9_criteria",
                "demo_criteria",
                "person",
                "concept",
                "condition_occurrence",
                "procedure_occurrence",
                "measurement");
    }

    @Test
    public void getCriteriaByTypeAndParentId_Icd9() throws Exception {
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
    }

    @Test
    public void getCriteriaByTypeAndParentId_demo() throws Exception {
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
    }

    @Test
    public void searchSubjects() throws Exception {
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("ICD9", "001.1", "Condition", null)),
                "1,1,1");
    }

    @Test
    public void searchSubjects_ConditionOccurrenceParent() throws Exception {
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("ICD9", "001", null, null) ),
                "1,1,1");
    }

    @Test
    public void searchSubjects_ProcedureOccurrenceLeaf() throws Exception {
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("ICD9", "002.1", "Procedure", null) ),
                "2,2,2");
    }

    @Test
    public void searchSubjects_ProcedureOccurrenceParent() throws Exception {
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("ICD9", "002", null, null) ),
                "2,2,2");
    }

    @Test
    public void searchSubjects_MeasurementLeaf() throws Exception {
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("ICD9", "003.1", "Measurement", null) ),
                "3,3,3");
    }

    @Test
    public void searchSubjects_MeasurementParent() throws Exception {
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("ICD9", "003", null, null) ),
                "3,3,3");
    }

    @Test
    public void searchSubjects_DemoGender() throws Exception {
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("DEMO", null, "DEMO_GEN", 8507L) ),
                "4,4,4");
    }

    @Test
    public void searchSubjects_DemoAge() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        int age = Period.between(birthdate, now).getYears();
        assertSubjects(
                controller.searchSubjects(
                        createSearchRequest("DEMO", String.valueOf(age), "DEMO_AGE", null) ),
                "5,5,5");
    }

    @Test
    public void filterSearchParametersWithoutDomainId() throws Exception {
        final SearchParameter expectedParameter = new SearchParameter().value("001");
        List<SearchParameter> parameterList = new ArrayList<>();
        parameterList.add(expectedParameter);
        parameterList.add(new SearchParameter().value("002").domain("Condition"));
        assertThat(controller.filterSearchParametersWithoutDomain(parameterList))
                .isEqualTo(Arrays.asList(expectedParameter));
    }

    @Test
    public void filterSearchParametersWithDomainId() throws Exception {
        final SearchParameter expectedParameter = new SearchParameter().value("002").domain("Condition");
        List<SearchParameter> parameterList = new ArrayList<>();
        parameterList.add(new SearchParameter().value("001"));
        parameterList.add(expectedParameter);
        assertThat(controller.filterSearchParametersWithDomain(parameterList))
                .isEqualTo(Arrays.asList(expectedParameter));
    }

    private void assertCriteria(ResponseEntity response, Criteria expectedCriteria) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CriteriaListResponse listResponse = (CriteriaListResponse) response.getBody();
        assertThat(listResponse.getItems().get(0)).isEqualTo(expectedCriteria);
    }

    private SearchRequest createSearchRequest(String type, String value, String domain, Long conceptId) {
        List<SearchParameter> parameters = new ArrayList<>();
        parameters.add(new SearchParameter().value(value).domain(domain).conceptId(conceptId));

        final SearchGroupItem searchGroupItem = new SearchGroupItem().type(type).searchParameters(parameters);

        final SearchGroup searchGroup = new SearchGroup();
        searchGroup.add(searchGroupItem);

        return new SearchRequest().include(Arrays.asList(searchGroup));
    }

    private void assertSubjects(ResponseEntity response, String expectedSubject) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SubjectListResponse listResponse = (SubjectListResponse) response.getBody();
        String subject = listResponse.get(0);
        assertThat(subject).isEqualTo(expectedSubject);
    }
}
