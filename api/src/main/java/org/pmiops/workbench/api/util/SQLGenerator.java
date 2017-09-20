package org.pmiops.workbench.api.util;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SQLGenerator {

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

    public QueryRequest findGroupCodes(String type, List<String> codes) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT code, domain_id AS domainId FROM ");
        queryBuilder.append(String.format("`%s.%s_criteria` ", getTablePrefix(), type.toLowerCase()));
        queryBuilder.append("WHERE (");

        Map<String, QueryParameterValue> queryParams = new HashMap<>();
        List<String> queryParts = new ArrayList<>();
        for (String code: codes) {
            queryParams.put(code, QueryParameterValue.string(code));
            queryParts.add("code LIKE @"+code);
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
        Map<String, List<String>> paramMap = getMappedParameters(params);
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

    protected Map<String, List<String>> getMappedParameters(List<SearchParameter> searchParameters) {
        Map<String, List<String>> mappedParameters = new HashMap<String, List<String>>();
        for (SearchParameter parameter : searchParameters) {
            List<String> codes = mappedParameters.get(parameter.getDomainId());
            if (codes != null) {
                codes.add(parameter.getCode());
            } else {
                List<String> codeList = new ArrayList<>();
                codeList.add(parameter.getCode());
                mappedParameters.put(parameter.getDomainId(), codeList);
            }
        }
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

    protected String getSubQuery(String key) {
        String codesSymbol = "@" + key + "codes";
        String tablePrefix = getTablePrefix();
        Map<String, String> subqueryByCode = new HashMap<String, String>();
        subqueryByCode.put("Condition",
            "SELECT DISTINCT PERSON_ID "+
            "FROM `%s.condition_occurrence` a, `%s.CONCEPT` b "+
            "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND a.CONDITION_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Observation",
            "SELECT DISTINCT PERSON_ID "+
            "FROM `%s.observation` a, `%s.CONCEPT` b "+
            "WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND a.OBSERVATION_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Measurement",
            "SELECT DISTINCT PERSON_ID "+
            "FROM `%s.measurement` a, `%s.CONCEPT` b "+
            "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.MEASUREMENT_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Exposure",
            "SELECT DISTINCT PERSON_ID "+
            "FROM `%s.device_exposure` a, `%s.CONCEPT` b "+
            "WHERE a.DEVICE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.DEVICE_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Drug",
            "SELECT DISTINCT PERSON_ID "+
            "FROM `%s.drug_exposure` a, `%s.CONCEPT` b "+
            "WHERE a.DRUG_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.DRUG_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Procedure",
            "SELECT DISTINCT PERSON_ID "+
            "FROM `%s.procedure_occurrence` a, `%s.CONCEPT` b "+
            "WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.PROCEDURE_SOURCE_VALUE IN (%s)"
        );
        String subQuery = subqueryByCode.get(key);
        return String.format(subQuery, tablePrefix, tablePrefix, codesSymbol);
    }

    protected String getTablePrefix() {
        return "pmi-drc-api-test.synpuf";
    }
}
