package org.pmiops.workbench.api;

import com.google.cloud.bigquery.*;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(String type, String parentId) {

        BigQuery bigquery =
                new BigQueryOptions.DefaultBigqueryFactory().create(BigQueryOptions.getDefaultInstance());

        String queryString =
                "SELECT id, type, code, name, est_count, is_group, is_selectable, domain_id "
                        + "FROM `pmi-drc-api-test.synpuf.icd9_crtieria` "
                        + "WHERE parent_id = @parentId "
                        + "order by id asc";
        QueryRequest queryRequest =
                QueryRequest.newBuilder(queryString)
                        .addNamedParameter("parentId", QueryParameterValue.string(parentId))
                        .setUseLegacySql(false)
                        .build();

        // Execute the query.
        QueryResponse response = bigquery.query(queryRequest);

        // Wait for the job to finish
        while (!response.jobCompleted()) {
            response = bigquery.getQueryResults(response.getJobId());
        }

        // Check for errors.
        if (response.hasErrors()) {
            String firstError = "";
            if (response.getExecutionErrors().size() != 0) {
                firstError = response.getExecutionErrors().get(0).getMessage();
            }
            throw new RuntimeException(firstError);
        }

        // Print all pages of the results.
        QueryResult result = response.getResult();

        CriteriaListResponse criteriaResponse = new CriteriaListResponse();
        for (List<FieldValue> row : result.iterateAll()) {
            final Criteria criteria = new Criteria();
            criteria.setId(row.get(0).getLongValue());
            criteria.setType(row.get(1).getStringValue());
            criteria.setCode(row.get(2).getStringValue());
            criteria.setName(row.get(3).getStringValue());
            criteria.setCount(row.get(4).isNull() ? 0 : row.get(4).getLongValue());
            criteria.setGroup(row.get(5).getBooleanValue());
            criteria.setSelectable(row.get(6).getBooleanValue());
            criteria.setDomainId(row.get(7).isNull() ? null : row.get(7).getStringValue());
            criteriaResponse.addItemsItem(criteria);
        }

        return ResponseEntity.ok(criteriaResponse);
    }
}
