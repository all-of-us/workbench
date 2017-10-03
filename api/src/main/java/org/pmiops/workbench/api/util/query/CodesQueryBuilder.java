package org.pmiops.workbench.api.util.query;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.UnmodifiableIterator;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CodesQueryBuilder extends AbstractQueryBuilder {

    private static final Map<String, String> typeCM = new HashMap<>();
    private static final Map<String, String> typeProc = new HashMap<>();
    static {
        typeCM.put("ICD9", "ICD9CM");
        typeCM.put("ICD10", "ICD10CM");
        typeCM.put("CPT", "CPT4");

        typeProc.put("ICD9", "ICD9Proc");
        typeProc.put("ICD10", "ICD10PCS");
        typeProc.put("CPT", "CPT4");
    }

    /*
     * The format placeholders stand for, in order:
     *  - the table prefix (e.g., something like "pmi-drc-api-test.synpuf")
     *  - the table name
     *  - the table prefix again
     *  - the *_source_concept_id column identifier for that table
     *  - a BigQuery "named parameter" indicating the list of codes to search by
     *  See the link below about IN and UNNEST
     * https://cloud.google.com/bigquery/docs/reference/standard-sql/functions-and-operators#in-operators
     */
    private static final String INNER_SQL_TEMPLATE =
            "select distinct person_id " +
                    "from `${projectId}.${dataSetId}.${tableName}` a, `${projectId}.${dataSetId}.concept` b "+
                    "where a.${tableId} = b.concept_id "+
                    "and b.vocabulary_id in (@cm,@proc) " +
                    "and b.concept_code in unnest(${conceptCodes})";

    private static final String UNION_TEMPLATE = " union distinct ";

    private static final String OUTER_SQL_TEMPLATE =
            "select distinct concat(" +
                    "cast(p.person_id as string), ',', " +
                    "p.gender_source_value, ',', " +
                    "p.race_source_value) as val "+
                    "from `${projectId}.${dataSetId}.person` p "+
                    "where person_id in (${innerSql})";

    @Override
    public QueryRequest buildQueryRequest(QueryParameters params) {
        ListMultimap<String, String> paramMap = getMappedParameters(params.getParameters());
        List<String> queryParts = new ArrayList<String>();
        Map<String, QueryParameterValue> queryParams = new HashMap<>();

        queryParams.put("cm", QueryParameterValue.string(typeCM.get(params.getType())));
        queryParams.put("proc", QueryParameterValue.string(typeProc.get(params.getType())));

        for (String key : paramMap.keySet()) {
            queryParams.put(TableEnum.getConceptCodes(key),
                    QueryParameterValue.array(paramMap.get(key).stream().toArray(String[]::new), String.class));
            queryParts.add(filterSql(INNER_SQL_TEMPLATE,
                    ImmutableMap.of("${tableName}", TableEnum.getTableName(key),
                            "${tableId}", TableEnum.getSourceConceptId(key),
                            "${conceptCodes}", "@" + TableEnum.getConceptCodes(key))));
        }


        String finalSql = OUTER_SQL_TEMPLATE.replace("${innerSql}", String.join(UNION_TEMPLATE, queryParts));

        return QueryRequest
                .newBuilder(filterBigQueryConfig(finalSql))
                .setNamedParameters(queryParams)
                .setUseLegacySql(false)
                .build();
    }

    @Override
    public String getType() {
        return FactoryKey.CODES.getName();
    }

    protected ListMultimap<String, String> getMappedParameters(List<SearchParameter> searchParameters) {
        ListMultimap<String, String> mappedParameters = ArrayListMultimap.create();
        for (SearchParameter parameter : searchParameters)
            mappedParameters.put(parameter.getDomainId(), parameter.getCode());
        return mappedParameters;
    }

    private String filterSql(String sqlStatement, ImmutableMap replacements) {
        String returnSql = sqlStatement;
        for (UnmodifiableIterator iterator = replacements.keySet().iterator(); iterator.hasNext();) {
            String key = (String)iterator.next();
            returnSql = returnSql.replace(key, replacements.get(key).toString());
        }
        return returnSql;

    }
}
