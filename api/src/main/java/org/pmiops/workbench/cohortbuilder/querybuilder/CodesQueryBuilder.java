package org.pmiops.workbench.cohortbuilder.querybuilder;
      //org.pmiops.workbench.api.cohortbuilder.querybuilder

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
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

/**
 * CodesQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for the following criteria types:
 * ICD9, ICD10 and CPT.
 */
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
            "select distinct person_id\n" +
                    "from `${projectId}.${dataSetId}.${tableName}` a, `${projectId}.${dataSetId}.concept` b\n"+
                    "where a.${tableId} = b.concept_id\n"+
                    "and b.vocabulary_id in (${cm},${proc})\n" +
                    "and b.concept_code in unnest(${conceptCodes})\n";

    private static final String UNION_TEMPLATE = " union distinct\n";

    private static final String OUTER_SQL_TEMPLATE =
            "select person_id\n"+
                    "from `${projectId}.${dataSetId}.person` p\n"+
                    "where person_id in (${innerSql})\n";

    @Override
    public QueryJobConfiguration buildQueryJobConfig(QueryParameters params) {
        ListMultimap<String, String> paramMap = getMappedParameters(params.getParameters());
        List<String> queryParts = new ArrayList<String>();
        Map<String, QueryParameterValue> queryParams = new HashMap<>();

        final String cmUniqueParam = getUniqueNamedParameter("cm" + params.getType());
        final String procUniqueParam = getUniqueNamedParameter("proc" + params.getType());
        queryParams.put(cmUniqueParam, QueryParameterValue.string(typeCM.get(params.getType())));
        queryParams.put(procUniqueParam, QueryParameterValue.string(typeProc.get(params.getType())));

        for (String key : paramMap.keySet()) {
            String namedParameter = getUniqueNamedParameter(key);
            queryParams.put(namedParameter,
                    QueryParameterValue.array(paramMap.get(key).stream().toArray(String[]::new), String.class));
            queryParts.add(filterSql(INNER_SQL_TEMPLATE,
                    ImmutableMap.of("${tableName}", DomainTableEnum.getTableName(key),
                            "${tableId}", DomainTableEnum.getSourceConceptId(key),
                            "${conceptCodes}", "@" + namedParameter,
                            "${cm}", "@" + cmUniqueParam,
                            "${proc}", "@" + procUniqueParam)));
        }


        String finalSql = OUTER_SQL_TEMPLATE.replace("${innerSql}", String.join(UNION_TEMPLATE, queryParts));

        return QueryJobConfiguration
                .newBuilder(finalSql)
                .setNamedParameters(queryParams)
                .setUseLegacySql(false)
                .build();
    }

    @Override
    public FactoryKey getType() {
        return FactoryKey.CODES;
    }

    protected ListMultimap<String, String> getMappedParameters(List<SearchParameter> searchParameters) {
        ListMultimap<String, String> mappedParameters = ArrayListMultimap.create();
        for (SearchParameter parameter : searchParameters)
            mappedParameters.put(parameter.getDomain(), parameter.getValue());
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
