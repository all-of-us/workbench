package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.SubjectCounter;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
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
@Import({QueryBuilderFactory.class, CohortBuilderController.class, SubjectCounter.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    @Autowired
    CohortBuilderController controller;

    @Autowired
    WorkbenchConfig workbenchConfig;

    @Override
    public List<String> getTableNames() {
        return Arrays.asList(
                "icd9_criteria",
                "person",
                "concept",
                "condition_occurrence",
                "procedure_occurrence",
                "measurement");
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
                        createSearchRequests("ICD9", Arrays.asList(new SearchParameter().value("001.1")))),
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
                        createSearchRequests("DEMO", Arrays.asList(new SearchParameter().domain("DEMO_GEN").conceptId(8507L)))),
                        2);
    }

    @Test
    public void countSubjects_DemoAge() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        int age = Period.between(birthdate, now).getYears();
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("DEMO", Arrays.asList(new SearchParameter().value(String.valueOf(age)).domain("DEMO_AGE")))),
                        1);
    }

    @Test
    public void countSubjects_DemoGenderAndAge() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        int age = Period.between(birthdate, now).getYears();
        SearchParameter ageParameter = new SearchParameter().value(String.valueOf(age)).domain("DEMO_AGE");
        SearchParameter genderParameter = new SearchParameter().domain("DEMO_GEN").conceptId(8507L);
        assertSubjects(
                controller.countSubjects(
                        createSearchRequests("DEMO", Arrays.asList(ageParameter, genderParameter))),
                        3);
    }

    @Test
    public void countSubjects_ICD9_Demo_SameSearchGroup() throws Exception {
        LocalDate birthdate = LocalDate.of(1980, 8, 01);
        LocalDate now = LocalDate.now();
        int age = Period.between(birthdate, now).getYears();

        SearchParameter ageParameter = new SearchParameter().value(String.valueOf(age)).domain("DEMO_AGE");
        SearchParameter genderParameter = new SearchParameter().domain("DEMO_GEN").conceptId(8507L);

        SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type("ICD9")
                .searchParameters(Arrays.asList(new SearchParameter().value("003.1").domain("Measurement")));

        SearchRequest testSearchRequest = createSearchRequests("DEMO", Arrays.asList(ageParameter, genderParameter));
        testSearchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem);

        assertSubjects( controller.countSubjects(testSearchRequest), 3);
    }

    @Test
    public void countSubjects_ICD9_Demo_DiffSearchGroup() throws Exception {
        SearchParameter genderParameter = new SearchParameter().domain("DEMO_GEN").conceptId(8507L);

        SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type("ICD9")
                .searchParameters(Arrays.asList(new SearchParameter().value("003.1").domain("Measurement")));
        SearchGroup anotherSearchGroup = new SearchGroup().addItemsItem(anotherSearchGroupItem);

        SearchRequest testSearchRequest = createSearchRequests("DEMO", Arrays.asList(genderParameter));
        testSearchRequest.getIncludes().add(anotherSearchGroup);

        assertSubjects( controller.countSubjects(testSearchRequest), 1);
    }

    @Test
    public void countSubjects_DemoExcluded() throws Exception {
        SearchParameter genderParameter = new SearchParameter().domain("DEMO_GEN").conceptId(8507L);

        SearchGroupItem anotherSearchGroupItem = new SearchGroupItem().type("DEMO")
                .searchParameters(Arrays.asList(new SearchParameter().domain("DEMO_GEN").conceptId(8507L)));
        SearchGroup anotherSearchGroup = new SearchGroup().addItemsItem(anotherSearchGroupItem);

        SearchRequest testSearchRequest = createSearchRequests("DEMO", Arrays.asList(genderParameter));
        testSearchRequest.getExcludes().add(anotherSearchGroup);

        assertSubjects( controller.countSubjects(testSearchRequest), 0);
    }

    @Test
    public void filterBigQueryConfig_WithoutTableName() throws Exception {
        final String statement = "my statement ${projectId}.${dataSetId}.myTableName";
        QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration.newBuilder(statement).setUseLegacySql(false).build();
        final String expectedResult = "my statement " + getTablePrefix() + ".myTableName";
        assertThat(expectedResult).isEqualTo(controller.filterBigQueryConfig(queryJobConfiguration).getQuery());
    }

    protected String getTablePrefix() {
        return workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
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
}
