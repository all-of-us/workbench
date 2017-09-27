package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.api.util.SQLGenerator;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SubjectListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    @Autowired
    private SQLGenerator generator;

    @Autowired
    private BigQuery bigquery;

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(String type, Long parentId) {

        QueryRequest queryRequest = generator.getCriteriaByTypeAndParentId(type, parentId);

        QueryResult result = executeQuery(queryRequest);

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

    @Override
    public ResponseEntity<SubjectListResponse> searchSubjects(SearchRequest request) {
        /* TODO: this function currently processes a SearchGroupItem; higher level handling will be needed */
        SearchGroupItem item = request.getInclude().get(0).get(0);
        QueryRequest query;
        QueryResult result;
        Map<String, Integer> resultMapper;

        SubjectListResponse subjectSet = new SubjectListResponse();

        if (item.getType().equals("ICD9")) {
            List<SearchParameter> params = item.getSearchParameters();

            /*  Check for SearchParameters without Domain IDs.
                If any are present, generate a query to retrieve the missing domain IDs, then append the new parameters
                to the parameter set
             */
            List<String> paramsWithoutDomains = generator.findParametersWithEmptyDomainIds(params);
            if (!paramsWithoutDomains.isEmpty()) {
                query = generator.findGroupCodes(item.getType(), paramsWithoutDomains);
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
            query = generator.handleSearch(item.getType(), params);
            result = executeQuery(query);
            resultMapper = getResultMapper(result);
            for (List<FieldValue> row : result.iterateAll()) {
                String subject = row.get(resultMapper.get("val")).getStringValue();
                subjectSet.add(subject);
            }
            return ResponseEntity.ok(subjectSet);
        }
        return ResponseEntity.badRequest().build();
    }

    private QueryResult executeQuery(QueryRequest query) {

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

    private Map<String, Integer> getResultMapper(QueryResult result) {
        Map<String, Integer> resultMapper = new HashMap<>();
        int i = 0;
        for (Field field : result.getSchema().getFields()) {
            resultMapper.put(field.getName(), i++);
        }
        return resultMapper;
    }
}
