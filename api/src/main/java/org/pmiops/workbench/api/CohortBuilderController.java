package org.pmiops.workbench.api;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.SubjectCounter;
import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.cdr.dao.Icd9CriteriaDao;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
public class CohortBuilderController implements CohortBuilderApiDelegate {

    @Autowired
    private BigQuery bigquery;

    @Autowired
    private SubjectCounter subjectCounter;

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfig;

    @Autowired
    Icd9CriteriaDao icd9CriteriaDao;

    private static final Logger log = Logger.getLogger(CohortBuilderController.class.getName());

    /**
     * This method list any of the criteria trees.
     *
     * @param type
     * @param parentId
     * @return
     */
    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaByTypeAndParentId(String type, Long parentId) {

//        QueryJobConfiguration queryRequest = QueryBuilderFactory
//                .getQueryBuilder(FactoryKey.CRITERIA)
//                .buildQueryJobConfig(new QueryParameters().type(type).parentId(parentId));
//
//        QueryResult result = executeQuery(filterBigQueryConfig(queryRequest));
//        Map<String, Integer> rm = getResultMapper(result);

        CriteriaListResponse criteriaResponse = new CriteriaListResponse();
        icd9CriteriaDao.findIcd9CriteriaByParentId(parentId);
//        for (List<FieldValue> row : result.iterateAll()) {
//            criteriaResponse.addItemsItem(new Criteria()
//                    .id(getLong(row, rm.get("id")))
//                    .type(getString(row, rm.get("type")))
//                    .code(getString(row, rm.get("code")))
//                    .name(getString(row, rm.get("name")))
//                    .count(getLong(row, rm.get("est_count")))
//                    .group(getBoolean(row, rm.get("is_group")))
//                    .selectable(getBoolean(row, rm.get("is_selectable")))
//                    .conceptId(getLong(row, rm.get("concept_id")))
//                    .domainId(getString(row, rm.get("domain_id"))));
//        }

        return ResponseEntity.ok(criteriaResponse);
    }

    /**
     * This method will return a count of unique subjects
     * defined by the provided {@link SearchRequest}.
     *
     * @param request
     * @return
     */
    @Override
    public ResponseEntity<Long> countSubjects(SearchRequest request) {

        /** TODO: this is temporary and will be removed when we figure out the conceptId mappings **/
        findCodesForEmptyDomains(request.getIncludes());
        findCodesForEmptyDomains(request.getExcludes());

        QueryResult result = executeQuery(filterBigQueryConfig(subjectCounter.buildSubjectCounterQuery(request)));
        Map<String, Integer> rm = getResultMapper(result);

        List<FieldValue> row = result.iterateAll().iterator().next();
        return ResponseEntity.ok(getLong(row, rm.get("count")));
    }

    /**
     * TODO: this is temporary and will be removed when we figure out the conceptId mappings
     * for ICD9, ICD10 and CPT codes.
     **/
    private void findCodesForEmptyDomains(List<SearchGroup> searchGroups) {
        AbstractQueryBuilder builder = QueryBuilderFactory.getQueryBuilder(FactoryKey.GROUP_CODES);
        searchGroups.stream()
                .flatMap(searchGroup -> searchGroup.getItems().stream())
                .filter(item -> item.getType().matches("ICD9|ICD10|CPT"))
                .forEach(item -> {

                    for (SearchParameter parameter : item.getSearchParameters()) {
                        if (parameter.getDomain() == null || parameter.getDomain().isEmpty()) {
                            QueryResult result = executeQuery(
                                    filterBigQueryConfig(
                                            builder.buildQueryJobConfig(new QueryParameters()
                                            .type(item.getType())
                                            .parameters(Arrays.asList(parameter)))));

                            Map<String, Integer> rm = getResultMapper(result);
                            List<SearchParameter> paramsWithDomains = new ArrayList<>();
                            for (List<FieldValue> row : result.iterateAll()) {
                                paramsWithDomains.add(new SearchParameter()
                                        .domain(getString(row, rm.get("domainId")))
                                        .value(getString(row, rm.get("code"))));
                            }
                            item.setSearchParameters(paramsWithDomains);
                        }
                    }
                });
    }

    /**
     * Execute the provided query using bigquery.
     *
     * @param query
     * @return
     */
    private QueryResult executeQuery(QueryJobConfiguration query) {

        // Execute the query
        QueryResponse response = null;
        try {
            response = bigquery.query(query);
        } catch (InterruptedException e) {
            throw new BigQueryException(500, "Something went wrong with BigQuery: " + e.getMessage());
        }

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

    protected QueryJobConfiguration filterBigQueryConfig(QueryJobConfiguration queryJobConfiguration) {
        String returnSql = queryJobConfiguration.getQuery().replace("${projectId}", workbenchConfig.get().bigquery.projectId);
        returnSql = returnSql.replace("${dataSetId}", workbenchConfig.get().bigquery.dataSetId);
        return queryJobConfiguration
                .toBuilder()
                .setQuery(returnSql)
                .build();
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
}
