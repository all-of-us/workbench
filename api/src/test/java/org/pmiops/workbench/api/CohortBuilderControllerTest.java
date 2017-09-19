package org.pmiops.workbench.api;

import com.google.cloud.bigquery.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

@Import(CohortBuilderController.class)
public class CohortBuilderControllerTest extends BigQueryBaseTest {

    @Autowired
    CohortBuilderController controller;

    @Before
    public void setUp() {
        List<Field> fields = Arrays.asList(
                Field.of("id", Field.Type.integer()),
                Field.of("parent_id", Field.Type.integer()),
                Field.of("type", Field.Type.string()),
                Field.of("code", Field.Type.string()),
                Field.of("name", Field.Type.string()),
                Field.of("is_group", Field.Type.bool()),
                Field.of("is_selectable", Field.Type.bool()),
                Field.of("est_count", Field.Type.integer()),
                Field.of("domain_id", Field.Type.string())
        );
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("id", 1);
        row.put("parent_id", 0);
        row.put("type", "ICD9");
        row.put("code", "001.1");
        row.put("name", "Cholera");
        row.put("is_group", true);
        row.put("is_selectable", false);
        row.put("est_count", 10);
        row.put("domain_id", "Condition");
        createDataSet(controller.getDataSetId());
        createTable(controller.getDataSetId(), "icd9_criteria", fields );
        insertRow(controller.getDataSetId(), "icd9_criteria", row);
    }

    @After
    public void tearDown() {
        deleteTable(controller.getDataSetId(), "icd9_criteria");
        deleteDataSet(controller.getDataSetId());
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