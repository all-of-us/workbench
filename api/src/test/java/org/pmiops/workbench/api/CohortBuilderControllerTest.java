package org.pmiops.workbench.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.util.SQLGenerator;
import org.pmiops.workbench.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Import({CohortBuilderController.class, SQLGenerator.class})
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    @Autowired
    CohortBuilderController controller;

    @Override
    public List<String> getTableNames() {
        return Arrays.asList("icd9_criteria", "person", "CONCEPT", "condition_occurrence");
    }

    @Test
    public void getCriteriaByTypeAndParentId() throws Exception {
        ResponseEntity response = controller.getCriteriaByTypeAndParentId("icd9", 0L );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        CriteriaListResponse listResponse = (CriteriaListResponse) response.getBody();
        Criteria criteria = listResponse.getItems().get(0);
        assertThat(criteria.getId()).isEqualTo(1);
        assertThat(criteria.getType()).isEqualTo("ICD9");
        assertThat(criteria.getCode()).isEqualTo("001.1");
        assertThat(criteria.getName()).isEqualTo("Cholera");
        assertThat(criteria.getGroup()).isEqualTo(true);
        assertThat(criteria.getSelectable()).isEqualTo(false);
        assertThat(criteria.getCount()).isEqualTo(10);
        assertThat(criteria.getDomainId()).isEqualTo("Condition");
    }

    @Test
    public void searchSubjects() throws Exception {
        final SearchParameter searchParameter = new SearchParameter();
        searchParameter.setCode("001.1");
        searchParameter.setDomainId("Condition");

        final SearchRequest searchRequest = new SearchRequest();
        searchRequest.setType("ICD9");
        searchRequest.setSearchParameters(Arrays.asList(searchParameter));

        ResponseEntity response = controller.searchSubjects(searchRequest );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        SubjectListResponse listResponse = (SubjectListResponse) response.getBody();
        Subject subject = listResponse.getItems().get(0);
        assertThat(subject.getVal()).isEqualTo("1,1,1");
    }
}