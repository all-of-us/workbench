package org.pmiops.workbench.api;

import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.util.SQLGenerator;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.Subject;
import org.pmiops.workbench.model.SubjectListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({CohortBuilderController.class, SQLGenerator.class})
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    @Autowired
    CohortBuilderController controller;

    @Override
    public List<String> getTableNames() {
        return Arrays.asList(
                "icd9_criteria",
                "person",
                "CONCEPT",
                "condition_occurrence",
                "procedure_occurrence",
                "measurement");
    }

    @Test
    public void getCriteriaByTypeAndParentId() throws Exception {
        ResponseEntity response = controller.getCriteriaByTypeAndParentId("icd9", 0L );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CriteriaListResponse listResponse = (CriteriaListResponse) response.getBody();
        Criteria criteria = listResponse.getItems().get(0);
        assertThat(criteria.getId()).isEqualTo(1);
        assertThat(criteria.getType()).isEqualTo("ICD9");
        assertThat(criteria.getCode()).isEqualTo("001-139.99");
        assertThat(criteria.getName()).isEqualTo("Infectious and parasitic diseases");
        assertThat(criteria.getGroup()).isEqualTo(false);
        assertThat(criteria.getSelectable()).isEqualTo(false);
        assertThat(criteria.getCount()).isEqualTo(0);
        assertThat(criteria.getDomainId()).isNull();
    }

    @Test
    public void searchSubjects_ConditionOccurrenceLeaf() throws Exception {
        ResponseEntity response = controller
                .searchSubjects(createSearchRequest("ICD9", "001.1", "Condition") );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SubjectListResponse listResponse = (SubjectListResponse) response.getBody();
        Subject subject = listResponse.getItems().get(0);
        assertThat(subject.getVal()).isEqualTo("1,1,1");
    }

    @Test
    public void searchSubjects_ConditionOccurrenceParent() throws Exception {
        ResponseEntity response = controller
                .searchSubjects(createSearchRequest("ICD9", "001", null) );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SubjectListResponse listResponse = (SubjectListResponse) response.getBody();
        Subject subject = listResponse.getItems().get(0);
        assertThat(subject.getVal()).isEqualTo("1,1,1");
    }

    @Test
    public void searchSubjects_ProcedureOccurrenceLeaf() throws Exception {
        ResponseEntity response = controller
                .searchSubjects(createSearchRequest("ICD9", "002.1", "Procedure") );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SubjectListResponse listResponse = (SubjectListResponse) response.getBody();
        Subject subject = listResponse.getItems().get(0);
        assertThat(subject.getVal()).isEqualTo("2,2,2");
    }

    @Test
    public void searchSubjects_ProcedureOccurrenceParent() throws Exception {
        ResponseEntity response = controller
                .searchSubjects(createSearchRequest("ICD9", "002", null) );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SubjectListResponse listResponse = (SubjectListResponse) response.getBody();
        Subject subject = listResponse.getItems().get(0);
        assertThat(subject.getVal()).isEqualTo("2,2,2");
    }

    @Test
    public void searchSubjects_Measurement() throws Exception {
        ResponseEntity response = controller
                .searchSubjects(createSearchRequest("ICD9", "001.3", "Measurement") );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SubjectListResponse listResponse = (SubjectListResponse) response.getBody();
        Subject subject = listResponse.getItems().get(0);
        assertThat(subject.getVal()).isEqualTo("3,3,3");
    }

    private SearchRequest createSearchRequest(String type, String code, String domainId) {
        List<SearchParameter> parameters = new ArrayList<>();
        final SearchParameter searchParameter = new SearchParameter();
        searchParameter.setCode(code);
        searchParameter.setDomainId(domainId);
        parameters.add(searchParameter);

        final SearchRequest searchRequest = new SearchRequest();
        searchRequest.setType(type);
        searchRequest.setSearchParameters(parameters);
        return searchRequest;
    }
}
