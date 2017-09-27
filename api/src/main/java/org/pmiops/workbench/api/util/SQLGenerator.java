package org.pmiops.workbench.api.util;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.util.*;

@Service
public class SQLGenerator {

    @Autowired
    private Provider<WorkbenchConfig> workbenchConfig;

    private static final String CRITERIA_QUERY =
            "SELECT id,\n" +
                    "type,\n" +
                    "code,\n" +
                    "name,\n" +
                    "est_count,\n" +
                    "is_group,\n" +
                    "is_selectable,\n" +
                    "domain_id\n" +
                    "FROM `%s.%s.%s`\n" +
                    "WHERE parent_id = @parentId\n" +
                    "order by id asc";

    /*
     * The format placeholders stand for, in order:
     *  - the table prefix (e.g., something like "pmi-drc-api-test.synpuf")
     *  - the table name
     *  - the table prefix again
     *  - the *_SOURCE_CONCEPT_ID column identifier for that table
     *  - a BigQuery "named parameter" indicating the list of codes to search by
     *  See the link below about IN and UNNEST
     * https://cloud.google.com/bigquery/docs/reference/standard-sql/functions-and-operators#in-operators
     */
    private static final String SUBQUERY_TEMPLATE =
            "SELECT DISTINCT PERSON_ID " +
                    "FROM `%s.%s.%s` a, `%s.%s.%s` b "+
                    "WHERE a.%s = b.CONCEPT_ID "+
                    "AND b.VOCABULARY_ID IN (@cm,@proc) " +
                    "AND b.CONCEPT_CODE IN UNNEST(%s)";

    private static final Map<String, String> typeCM = new HashMap<>();
    private static final Map<String, String> typeProc = new HashMap<>();
    private static final Map<String, Map<String, String>> tableInfo = new HashMap<>();
    static {
        typeCM.put("ICD9", "ICD9CM");
        typeCM.put("ICD10", "ICD10CM");
        typeCM.put("CPT", "CPT4");

        typeProc.put("ICD9", "ICD9Proc");
        typeProc.put("ICD10", "ICD10PCS");
        typeProc.put("CPT", "CPT4");

        Map<String, String> table = new HashMap<>();
        table.put("tableName", "condition_occurrence");
        table.put("sourceConceptIdColumn", "CONDITION_SOURCE_CONCEPT_ID");
        tableInfo.put("Condition", table);

        table = new HashMap<>();
        table.put("tableName", "observation");
        table.put("sourceConceptIdColumn", "OBSERVATION_SOURCE_CONCEPT_ID");
        table.put("sourceValueColumn", "OBSERVATION_SOURCE_VALUE");
        tableInfo.put("Observation", table);

        table = new HashMap<>();
        table.put("tableName", "measurement");
        table.put("sourceConceptIdColumn", "MEASUREMENT_SOURCE_CONCEPT_ID");
        table.put("sourceValueColumn", "MEASUREMENT_SOURCE_VALUE");
        tableInfo.put("Measurement", table);

        table = new HashMap<>();
        table.put("tableName", "device_exposure");
        table.put("sourceConceptIdColumn", "DEVICE_SOURCE_CONCEPT_ID");
        table.put("sourceValueColumn", "DEVICE_SOURCE_VALUE");
        tableInfo.put("Exposure", table);

        table = new HashMap<>();
        table.put("tableName", "drug_exposure");
        table.put("sourceConceptIdColumn", "DRUG_SOURCE_CONCEPT_ID");
        table.put("sourceValueColumn", "DRUG_SOURCE_VALUE");
        tableInfo.put("Drug", table);

        table = new HashMap<>();
        table.put("tableName", "procedure_occurrence");
        table.put("sourceConceptIdColumn", "PROCEDURE_SOURCE_CONCEPT_ID");
        table.put("sourceValueColumn", "PROCEDURE_SOURCE_VALUE");
        tableInfo.put("Procedure", table);
    }

    public QueryRequest getCriteriaByTypeAndParentId(String type, Long parentId) {
        return QueryRequest
                .newBuilder(setBigQueryConfig(CRITERIA_QUERY, type + "_criteria"))
                .addNamedParameter("parentId", QueryParameterValue.int64(new Long(parentId)))
                .setUseLegacySql(false)
                .build();
    }

    public QueryRequest findGroupCodes(String type, List<String> codes) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT code, domain_id AS domainId FROM ");
        queryBuilder.append(setBigQueryConfig("`%s.%s.%s` ", type.toLowerCase() + "_criteria"));
        queryBuilder.append("WHERE (");

        Map<String, QueryParameterValue> queryParams = new HashMap<>();
        List<String> queryParts = new ArrayList<>();
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            String name = String.format("code%d", i);
            queryParams.put(name, QueryParameterValue.string(code));
            queryParts.add("code LIKE @"+name);
        }
        queryBuilder.append(String.join(" OR ", queryParts));
        queryBuilder.append(") ");
        queryBuilder.append("AND is_selectable = TRUE AND is_group = FALSE ");
        queryBuilder.append("ORDER BY code ASC");

        QueryRequest request = QueryRequest.newBuilder(queryBuilder.toString())
            .setNamedParameters(queryParams)
            .setUseLegacySql(false)     // required for queries that use named parameters
            .build();
        return request;
    }

    public QueryRequest handleSearch(String type, List<SearchParameter> params) {
        ListMultimap<String, String> paramMap = getMappedParameters(params);
        List<String> queryParts = new ArrayList<String>();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(
            "SELECT DISTINCT CONCAT(" +
                "CAST(p.person_id AS string), ',', " +
                "p.gender_source_value, ',', " +
                "p.race_source_value) AS val "+
            "FROM " + setBigQueryConfig("`%s.%s.%s` ", "person") + " p "+
            "WHERE PERSON_ID IN ("
        );

        Map<String, QueryParameterValue> queryParams = new HashMap<>();

        queryParams.put("cm", QueryParameterValue.string(typeCM.get(type)));
        queryParams.put("proc", QueryParameterValue.string(typeProc.get(type)));

        for (String key : paramMap.keySet()) {
            List<String> rawValues = paramMap.get(key);
            String[] values = rawValues.toArray(new String[rawValues.size()]);
            String subquery = getSubQuery(key);
            queryParams.put(key + "codes", QueryParameterValue.array(values, String.class));
            queryParts.add(subquery);
        }

        queryBuilder.append(String.join(" UNION DISTINCT ", queryParts));
        queryBuilder.append(")");

        QueryRequest request = QueryRequest.newBuilder(queryBuilder.toString())
            .setNamedParameters(queryParams)
            .setUseLegacySql(false)
            .build();
        return request;
    }

    protected ListMultimap<String, String> getMappedParameters(List<SearchParameter> searchParameters) {
        ListMultimap<String, String> mappedParameters = ArrayListMultimap.create();
        for (SearchParameter parameter : searchParameters)
            mappedParameters.put(parameter.getDomainId(), parameter.getCode());
        return mappedParameters;
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

    private String getSubQuery(String key) {
        Map<String, String> info = tableInfo.get(key);

        return String.format(SUBQUERY_TEMPLATE,
                workbenchConfig.get().bigquery.projectId,
                workbenchConfig.get().bigquery.dataSetId,
                info.get("tableName"),
                workbenchConfig.get().bigquery.projectId,
                workbenchConfig.get().bigquery.dataSetId,
                "CONCEPT",
                info.get("sourceConceptIdColumn"),
                "@" + key + "codes"
        );
    }

    private String setBigQueryConfig(String sqlStatement, String tableName) {
        return String.format(sqlStatement,
                workbenchConfig.get().bigquery.projectId,
                workbenchConfig.get().bigquery.dataSetId,
                tableName);
    }
}
