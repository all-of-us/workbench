package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.api.util.QueryBuilderFactory;
import org.pmiops.workbench.api.util.query.FactoryKey;
import org.pmiops.workbench.api.util.query.QueryParameters;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SubjectListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    @Autowired
    private BigQuery bigquery;

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(String type, Long parentId) {

        QueryRequest queryRequest = QueryBuilderFactory
                .getQueryBuilder(FactoryKey.getKey(type))
                .buildQueryRequest(new QueryParameters().type(type).parentId(parentId));

        QueryResult result = executeQuery(queryRequest);

        Map<String, Integer> rm = getResultMapper(result);

        CriteriaListResponse criteriaResponse = new CriteriaListResponse();
        for (List<FieldValue> row : result.iterateAll()) {
            final Criteria criteria = new Criteria();
            criteria.setId(row.get(rm.get("id")).getLongValue());
            criteria.setType(row.get(rm.get("type")).getStringValue());
            criteria.setCode(row.get(rm.get("code")).isNull() ? null : row.get(rm.get("code")).getStringValue());
            criteria.setName(row.get(rm.get("name")).getStringValue());
            criteria.setCount(row.get(rm.get("est_count")).isNull() ? 0 : row.get(rm.get("est_count")).getLongValue());
            criteria.setGroup(row.get(rm.get("is_group")).getBooleanValue());
            criteria.setSelectable(row.get(rm.get("is_selectable")).getBooleanValue());
            criteria.setConceptId(row.get(rm.get("concept_id")).isNull() ? 0: row.get(rm.get("concept_id")).getLongValue());
            criteria.setDomainId(row.get(rm.get("domain_id")).isNull() ? null : row.get(rm.get("domain_id")).getStringValue());
            criteriaResponse.addItemsItem(criteria);
        }

        return ResponseEntity.ok(criteriaResponse);
    }

    @Override
    public ResponseEntity<SubjectListResponse> searchSubjects(SearchRequest request) {
        /* TODO: this function currently processes a SearchGroupItem; higher level handling will be needed */
        SearchGroupItem item = request.getInclude().get(0).get(0);
        SubjectListResponse subjectSet = new SubjectListResponse();

        List<SearchParameter> params = item.getSearchParameters();
        List<String> paramsWithoutDomains = findParametersWithEmptyDomainIds(item.getSearchParameters());

        /** TODO: this is temporary and will be removed when we figure out the conceptId mappings **/
        if (!paramsWithoutDomains.isEmpty()) {
            QueryRequest queryRequest = QueryBuilderFactory
                    .getQueryBuilder(FactoryKey.getKey("group-codes"))
                    .buildQueryRequest(new QueryParameters().type(item.getType()).codes(paramsWithoutDomains));

            QueryResult result = executeQuery(queryRequest);
            Map<String, Integer> resultMapper = getResultMapper(result);
            for (List<FieldValue> row : result.iterateAll()) {
                final SearchParameter newParam = new SearchParameter();
                newParam.setDomainId(row.get(resultMapper.get("domainId")).getStringValue());
                newParam.setCode(row.get(resultMapper.get("code")).getStringValue());
                params.add(newParam);
            }
        }

        QueryRequest queryRequest = QueryBuilderFactory
                .getQueryBuilder(FactoryKey.getKey(item.getType()))
                .buildQueryRequest(new QueryParameters().type(item.getType()).parameters(params));

        QueryResult result = executeQuery(queryRequest);
        Map<String, Integer> resultMapper = getResultMapper(result);
        for (List<FieldValue> row : result.iterateAll()) {
            String subject = row.get(resultMapper.get("val")).getStringValue();
            subjectSet.add(subject);
        }

        return subjectSet.isEmpty() ? ResponseEntity.badRequest().build() : ResponseEntity.ok(subjectSet);
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

    public List<String> findParametersWithEmptyDomainIds(List<SearchParameter> searchParameters) {
        List<String> returnParameters = new ArrayList<>();
        for (Iterator<SearchParameter> iter = searchParameters.iterator(); iter.hasNext();) {
            SearchParameter parameter = iter.next();
            if (parameter.getDomainId() == null || parameter.getDomainId().isEmpty()) {
                returnParameters.add(parameter.getCode() + "%");
                iter.remove();
            }
        }
        return returnParameters;
    }
}
