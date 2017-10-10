package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryRequest;
import org.springframework.stereotype.Service;

/**
 * CountQueryBuilder is an object that builds {@link QueryRequest}
 * for BigQuery for that count the number of subjects that exists
 * based on the {@link org.pmiops.workbench.model.SearchRequest}
 * construction.
 */
@Service
public class CountQueryBuilder extends AbstractQueryBuilder {

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
                    "(${excludeSql})\n";

    @Override
    public QueryRequest buildQueryRequest(QueryParameters parameters) {
        return null;
    }

    @Override
    public FactoryKey getType() {
        return FactoryKey.COUNT;
    }
}
