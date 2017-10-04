package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SubjectListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    @Autowired
    private BigQuery bigquery;

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(String type, Long parentId) {

        QueryRequest queryRequest = QueryBuilderFactory
                .getQueryBuilder(FactoryKey.getKey(type + "-TREE"))
                .buildQueryRequest(new QueryParameters().type(type).parentId(parentId));

        QueryResult result = executeQuery(queryRequest);
        Map<String, Integer> rm = getResultMapper(result);

        CriteriaListResponse criteriaResponse = new CriteriaListResponse();
        for (List<FieldValue> row : result.iterateAll()) {
            criteriaResponse.addItemsItem(new Criteria()
                    .id(getLong(row, rm.get("id")))
                    .type(getString(row, rm.get("type")))
                    .code(getString(row, rm.get("code")))
                    .name(getString(row, rm.get("name")))
                    .count(getLong(row, rm.get("est_count")))
                    .group(getBoolean(row, rm.get("is_group")))
                    .selectable(getBoolean(row, rm.get("is_selectable")))
                    .conceptId(getLong(row, rm.get("concept_id")))
                    .domainId(getString(row, rm.get("domain_id"))));
        }

        return ResponseEntity.ok(criteriaResponse);
    }

    @Override
    public ResponseEntity<SubjectListResponse> searchSubjects(SearchRequest request) {
        /* TODO: this function currently processes a SearchGroupItem; higher level handling will be needed */
        SearchGroupItem item = request.getInclude().get(0).get(0);
        SubjectListResponse subjectSet = new SubjectListResponse();

        List<SearchParameter> paramsWithDomains = filterSearchParametersWithDomain(item.getSearchParameters());
        List<SearchParameter> paramsWithoutDomains = filterSearchParametersWithoutDomain(item.getSearchParameters());

        /** TODO: this is temporary and will be removed when we figure out the conceptId mappings **/
        if (!paramsWithoutDomains.isEmpty()) {
            QueryRequest queryRequest = QueryBuilderFactory
                    .getQueryBuilder(FactoryKey.getKey("GROUP_CODES"))
                    .buildQueryRequest(new QueryParameters().type(item.getType()).parameters(paramsWithoutDomains));

            QueryResult result = executeQuery(queryRequest);
            Map<String, Integer> rm = getResultMapper(result);
            for (List<FieldValue> row : result.iterateAll()) {
                paramsWithDomains.add(new SearchParameter()
                        .domain(getString(row, rm.get("domainId")))
                        .value(getString(row, rm.get("code"))));
            }
        }

        QueryRequest queryRequest = QueryBuilderFactory
                .getQueryBuilder(FactoryKey.getKey(item.getType()))
                .buildQueryRequest(new QueryParameters().type(item.getType()).parameters(paramsWithDomains));

        QueryResult result = executeQuery(queryRequest);
        Map<String, Integer> rm = getResultMapper(result);

        for (List<FieldValue> row : result.iterateAll()) {
            subjectSet.add(getString(row, rm.get("val")));
        }

        return ResponseEntity.ok(subjectSet);
    }

    private Long getLong(List<FieldValue> row, int index) {
        return row.get(index).isNull() ? 0: row.get(index).getLongValue();
    }

    private String getString(List<FieldValue> row, int index) {
        return row.get(index).isNull() ? null : row.get(index).getStringValue();
    }

    private Boolean getBoolean(List<FieldValue> row, int index) {
        return row.get(index).getBooleanValue();
    }

    private QueryResult executeQuery(QueryRequest query) {

        // Execute the querybuilder
        QueryResponse response = bigquery.query(query);

        // Wait for the job to finish
        while (!response.jobCompleted()) {
            response = bigquery.getQueryResults(response.getJobId());
        }

        // Check for errors.
        if (response.hasErrors()) {
            if (response.getExecutionErrors().size() != 0) {
                throw new BigQueryException(500, "Something went wrong with BigQuery: ", response.getExecutionErrors().get(0));
            }
        }

        return response.getResult();
    }

    private Map<String, Integer> getResultMapper(QueryResult result) {
        AtomicInteger index = new AtomicInteger();
        return result.getSchema().getFields().stream().collect(
                Collectors.toMap(Field::getName, s -> index.getAndIncrement()));
    }

    protected List<SearchParameter> filterSearchParametersWithoutDomain(List<SearchParameter> searchParameters) {
        return searchParameters.stream()
                .filter(parameter -> parameter.getDomain() == null || parameter.getDomain().isEmpty())
                .collect(Collectors.toList());
    }

    protected List<SearchParameter> filterSearchParametersWithDomain(List<SearchParameter> searchParameters) {
        return searchParameters.stream()
                .filter(parameter -> parameter.getDomain() != null && !parameter.getDomain().isEmpty())
                .collect(Collectors.toList());
    }
}
