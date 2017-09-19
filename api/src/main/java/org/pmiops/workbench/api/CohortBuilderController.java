package org.pmiops.workbench.api;

import com.google.cloud.bigquery.*;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    @Value("#{dataSetId.getDataSetId()}")
    private String dataSetId;

    @Value("${bigQuery.projectId}")
    private String bigQueryProjectId;

    @Autowired
    BigQuery bigquery;

    public static final String CRITERIA_QUERY =
            "SELECT id,\n" +
                    "type,\n" +
                    "code,\n" +
                    "name,\n" +
                    "est_count,\n" +
                    "is_group,\n" +
                    "is_selectable,\n" +
                    "domain_id\n" +
                    "FROM `%s.%s.%s`\n" +
                    "WHERE parent_id = @parentId\n" +
                    "order by id asc";

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(String type, Long parentId) {

        QueryResult result = getQueryResult(type, parentId);

        Map<String, Integer> rm = getResultMapper(result);

        CriteriaListResponse criteriaResponse = new CriteriaListResponse();
        for (List<FieldValue> row : result.iterateAll()) {
            final Criteria criteria = new Criteria();
            criteria.setId(row.get(rm.get("id")).getLongValue());
            criteria.setType(row.get(rm.get("type")).getStringValue());
            criteria.setCode(row.get(rm.get("code")).getStringValue());
            criteria.setName(row.get(rm.get("name")).getStringValue());
            criteria.setCount(row.get(rm.get("est_count")).isNull() ? 0 : row.get(rm.get("est_count")).getLongValue());
            criteria.setGroup(row.get(rm.get("is_group")).getBooleanValue());
            criteria.setSelectable(row.get(rm.get("is_selectable")).getBooleanValue());
            criteria.setDomainId(row.get(rm.get("domain_id")).isNull() ? null : row.get(rm.get("domain_id")).getStringValue());
            criteriaResponse.addItemsItem(criteria);
        }

        return ResponseEntity.ok(criteriaResponse);
    }

    protected QueryResult getQueryResult(String type, Long parentId) {
        QueryRequest queryRequest =
                QueryRequest.newBuilder(getQueryString(type))
                        .addNamedParameter("parentId", QueryParameterValue.int64(new Long(parentId)))
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

        return response.getResult();
    }

    protected Map<String, Integer> getResultMapper(QueryResult result) {
        Map<String, Integer> resultMapper = new HashMap<String, Integer>();
        int i = 0;
        for (Field field : result.getSchema().getFields()) {
            resultMapper.put(field.getName(), i++);
        }
        return resultMapper;
    }

    protected String getQueryString(String type) {
        return String.format(CRITERIA_QUERY, bigQueryProjectId, dataSetId, type + "_criteria");
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public String getBigQueryProjectId() {
        return bigQueryProjectId;
    }
}
