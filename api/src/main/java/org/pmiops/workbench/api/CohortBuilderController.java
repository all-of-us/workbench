package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.api.util.SQLGenerator;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SubjectListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    @Autowired
    private SQLGenerator generator;

    @Autowired
    BigQuery bigquery;

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    public static final String CRITERIA_QUERY =
            "SELECT id,\n" +
                    "type,\n" +
                    "code,\n" +
                    "name,\n" +
                    "est_count,\n" +
                    "is_group,\n" +
                    "is_selectable,\n" +
                    "domain_id\n" +
                    "FROM `pmi-drc-api-test.synpuf.%s`\n" +
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
    }

    @Override
    public ResponseEntity<SubjectListResponse> searchSubjects(SearchRequest request) {
        QueryRequest query;
        QueryResult result;
        Map<String, Integer> resultMapper;

        SubjectListResponse subjectSet = new SubjectListResponse();

        if (request.getType().equals("ICD9")) {
            List<SearchParameter> params = request.getSearchParameters();

            /*  Check for SearchParameters without Domain IDs.
                If any are present, generate a query to retrieve the missing domain IDs, then append the new parameters
                to the parameter set
             */
            List<String> paramsWithoutDomains = generator.findParametersWithEmptyDomainIds(params);
            if (!paramsWithoutDomains.isEmpty()) {
                query = generator.findGroupCodes(request.getType(), paramsWithoutDomains);
                result = executeQuery(query);
                Map <String, Integer> resultMap = getResultMapper(result);
                for (List<FieldValue> row: result.iterateAll()) {
                    final SearchParameter newParam = new SearchParameter();
                    newParam.setDomainId(row.get(resultMap.get("domainId")).getStringValue());
                    newParam.setCode(row.get(resultMap.get("code")).getStringValue());
                    params.add(newParam);
                }
            }

            /*  Generate the actual query that will grab all the subjects based on criteria.
                TODO: modifier handling
             */
            query = generator.handleSearch(request.getType(), params);
            result = executeQuery(query);
            resultMapper = getResultMapper(result);
            for (List<FieldValue> row : result.iterateAll()) {
                final Subject subject = new Subject();
                subject.setVal(row.get(resultMapper.get("val")).getStringValue());
                subjectSet.addItemsItem(subject);
            }
            return ResponseEntity.ok(subjectSet);
        }
        return ResponseEntity.badRequest().build();
    }

    protected QueryResult executeQuery(QueryRequest query) {
        BigQuery bigquery = new BigQueryOptions
                .DefaultBigqueryFactory()
                .create(BigQueryOptions.getDefaultInstance());

        // Execute the query
        QueryResponse response = bigquery.query(query);

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
        Map<String, Integer> resultMapper = new HashMap<>();
        int i = 0;
        for (Field field : result.getSchema().getFields()) {
            resultMapper.put(field.getName(), i++);
        }
        return resultMapper;
    }

    protected String getQueryString(String type) {
        return String.format(CRITERIA_QUERY, type + "_criteria");
    }
}
