package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryResult;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class CodeDomainLookupService {

    @Autowired
    private BigQueryService bigQueryService;

    /**
     * TODO: this is temporary and will be removed when we figure out the conceptId mappings
     * for ICD9, ICD10 and CPT codes.
     **/
    public void findCodesForEmptyDomains(List<SearchGroup> searchGroups) {
        AbstractQueryBuilder builder = QueryBuilderFactory.getQueryBuilder(FactoryKey.GROUP_CODES);
        searchGroups.stream()
                .flatMap(searchGroup -> searchGroup.getItems().stream())
                .filter(item -> item.getType().matches("ICD9|ICD10|CPT"))
                .forEach(item -> {

                    for (SearchParameter parameter : item.getSearchParameters()) {
                        if (parameter.getDomain() == null || parameter.getDomain().isEmpty()) {
                            QueryResult result = bigQueryService.executeQuery(
                                    bigQueryService.filterBigQueryConfig(
                                            builder.buildQueryJobConfig(new QueryParameters()
                                                    .type(item.getType())
                                                    .parameters(Arrays.asList(parameter)))));

                            Map<String, Integer> rm = bigQueryService.getResultMapper(result);
                            List<SearchParameter> paramsWithDomains = new ArrayList<>();
                            for (List<FieldValue> row : result.iterateAll()) {
                                paramsWithDomains.add(new SearchParameter()
                                        .domain(bigQueryService.getString(row, rm.get("domainId")))
                                        .value(bigQueryService.getString(row, rm.get("code"))));
                            }
                            item.setSearchParameters(paramsWithDomains);
                        }
                    }
                });
    }
}
