package org.pmiops.workbench.api.util;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SQLGenerator {

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
        table.put("sourceValueColumn", "CONDITION_SOURCE_VALUE");
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

    public QueryRequest findGroupCodes(String type, List<String> codes) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT code, domain_id AS domainId FROM ");
        queryBuilder.append(String.format("`%s.%s_criteria` ", getTablePrefix(), type.toLowerCase()));
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
        queryBuilder.append("AND is_selectable = 1 AND is_group = 0 ");
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
            "FROM `" + getTablePrefix() + ".person` p "+
            "WHERE PERSON_ID IN ("
        );

        Map<String, QueryParameterValue> queryParams = new HashMap<>();

        queryParams.put("cm", QueryParameterValue.string(typeCM.get(type)));
        queryParams.put("proc", QueryParameterValue.string(typeProc.get(type)));

        for (String key : paramMap.keySet()) {
            List<String> values = paramMap.get(key);
            String codes = String.join(", ", values);
            String subquery = getSubQuery(key);
            queryParams.put(key+"codes", QueryParameterValue.string(codes));
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

    /*
     * The format placeholders stand for, in order:
     *  - the table prefix (e.g., something like "pmi-drc-api-test.synpuf")
     *  - the table name
     *  - the table prefix again
     *  - the *_SOURCE_CONCEPT_ID column identifier for that table
     *  - the *_SOURCE_VALUE column identifier for that table
     *  - a BigQuery "named parameter" indicating the list of codes to search by
     */
    private static final String subqueryTemplate = 
        "SELECT DISTINCT PERSON_ID " +
        "FROM `%s.%s` a, `%s.CONCEPT` b "+
        "WHERE a.%s = b.CONCEPT_ID "+
        "AND b.VOCABULARY_ID IN (@cm,@proc) " +
        "AND a.%s IN (%s)";

    protected String getSubQuery(String key) {
        String tablePrefix = getTablePrefix();
        Map<String, String> info = tableInfo.get(key);

        return String.format(subqueryTemplate, 
            tablePrefix, 
            info.get("tableName"),
            tablePrefix, 
            info.get("sourceConceptIdColumn"),
            info.get("sourceValueColumn"),
            "@" + key + "codes"
        );
    }

    protected String getTablePrefix() {
        // TODO: use the injectable config to grab this info
        return "pmi-drc-api-test.synpuf";
    }
}
