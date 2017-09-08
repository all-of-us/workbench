package org.pmiops.workbench.api;

import com.google.cloud.bigquery.*;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(String type, String parentId) {

        // Instantiates a client
        BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(
                        "SELECT id, " +
                                "type, " +
                                "code, " +
                                "name, " +
                                "est_count, " +
                                "is_group, " +
                                "is_selectable, " +
                                "domain_id " +
                                "FROM `pmi-drc-api-test.synpuf.icd9_criteria` " +
                                "where parent_id = 0 " +
                                "order by id asc;")
                        // Use standard SQL syntax for queries.
                        // See: https://cloud.google.com/bigquery/sql-reference/
                        .setUseLegacySql(false)
                        .build();

        // Create a job ID so that we can safely retry.
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

        // Wait for the query to complete.
        try {
            queryJob = queryJob.waitFor();
        } catch (Exception e) {
            log.log(Level.INFO, "Timeout occurred: ", e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
        }

        // Check for errors
        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        // Get the results.
        QueryResponse response = bigquery.getQueryResults(jobId);
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
