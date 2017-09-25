package org.pmiops.workbench.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Import(CohortBuilderController.class)
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    @Autowired
    CohortBuilderController controller;

    @Autowired
    WorkbenchConfig workbenchConfig;

    @Before
    public void setUp() throws Exception {
        createDataSet(workbenchConfig.bigquery.dataSetId);
        createTable(workbenchConfig.bigquery.dataSetId, "icd9_criteria");
        insertData(workbenchConfig.bigquery.dataSetId, "icd9_criteria");
    }

    @After
    public void tearDown() {
        deleteTable(workbenchConfig.bigquery.dataSetId, "icd9_criteria");
        deleteDataSet(workbenchConfig.bigquery.dataSetId);
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

}