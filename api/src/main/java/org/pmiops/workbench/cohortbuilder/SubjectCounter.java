package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubjectCounter {

    private static final String COUNT_SQL_TEMPLATE =
            "select count(distinct person_id) as count\n" +
                    "  from `${projectId}.${dataSetId}.person` person\n" +
                    " where 1 = 1\n";

    private static final String UNION_TEMPLATE =
            " union distinct\n";

    private static final String INCLUDE_SQL_TEMPLATE =
            "and person.person_id in (${includeSql})\n";

    private static final String EXCLUDE_SQL_TEMPLATE =
            "and not exists\n" +
                    "(select 'x' from\n" +
                    "(${excludeSql})\n" +
                    "x where x.person_id = person.person_id)\n";

    public QueryRequest buildSubjectCounterQuery(SearchRequest request) {
        Map<String, QueryParameterValue> params = new HashMap<>();
        List<String> queryParts = new ArrayList<>();
        String finalSql = COUNT_SQL_TEMPLATE;

        // build query for included search groups
        for (SearchGroup includeGroup : request.getIncludes()) {
            finalSql = finalSql + INCLUDE_SQL_TEMPLATE;
            for (SearchGroupItem includeItem : includeGroup.getItems()) {
                QueryRequest queryRequest = QueryBuilderFactory
                        .getQueryBuilder(FactoryKey.getType(includeItem.getType()))
                        .buildQueryRequest(new QueryParameters()
                                .type(includeItem.getType())
                                .parameters(includeItem.getSearchParameters()));
                params.putAll(queryRequest.getNamedParameters());
                queryParts.add(queryRequest.getQuery());
            }
            finalSql = finalSql.replace("${includeSql}", String.join(UNION_TEMPLATE, queryParts));
            queryParts = new ArrayList<>();
        }

        // build query for excluded search groups
        for (SearchGroup excludeGroup : request.getExcludes()) {
            finalSql = finalSql + EXCLUDE_SQL_TEMPLATE;
            for (SearchGroupItem excludeItem : excludeGroup.getItems()) {
                QueryRequest queryRequest = QueryBuilderFactory
                        .getQueryBuilder(FactoryKey.getType(excludeItem.getType()))
                        .buildQueryRequest(new QueryParameters()
                                .type(excludeItem.getType())
                                .parameters(excludeItem.getSearchParameters()));
                params.putAll(queryRequest.getNamedParameters());
                queryParts.add(queryRequest.getQuery());
            }
            finalSql = finalSql.replace("${excludeSql}", String.join(UNION_TEMPLATE, queryParts));
            queryParts = new ArrayList<>();
        }

        return QueryRequest
                        .newBuilder(finalSql)
                        .setNamedParameters(params)
                        .setUseLegacySql(false)
                        .build();
    }
}
