package org.pmiops.workbench.api.util;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Component;
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
        queryBuilder.append(String.format("`%s.%s_criteria` ", getTablePrefix(), type));
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

        return QueryRequest.newBuilder(queryBuilder.toString())
                .setNamedParameters(queryParams)
                .setUseLegacySql(false)     // required for queries that use named parameters
                .build();
    }

    public QueryRequest handleSearch(String type, List<SearchParameter> params) {
        Map<String, List<String>> paramMap = getMappedParameters(params);
        List<String> queryParts = new ArrayList<String>();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(
                "SELECT PERSON_ID || ',' || gender_source_value || ',' || x_race_ui AS val "+
                "FROM `"+getTablePrefix()+".PERSON` "+
                "WHERE PERSON_ID IN "+
                "(SELECT PERSON_ID FROM ("
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

        queryBuilder.append(String.join(" UNION ", queryParts));
        queryBuilder.append("))");

        return QueryRequest.newBuilder(queryBuilder.toString())
                .setNamedParameters(queryParams)
                .setUseLegacySql(false)
                .build();
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
            "SELECT DISTINCT PERSON_ID, CONDITION_START_DATE as ENTRY_DATE "+
            "FROM `%s.CONDITION_OCCURRENCE` a, `%s.CONCEPT` b "+
            "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND CONDITION_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Observation",
            "SELECT DISTINCT PERSON_ID, OBSERVATION_DATE as ENTRY_DATE "+
            "FROM `%s.OBSERVATION` a, `%s.CONCEPT` b "+
            "WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND OBSERVATION_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Measurement",
            "SELECT DISTINCT PERSON_ID, MEASUREMENT_DATE as ENTRY_DATE "+
            "FROM `%s.MEASUREMENT` a, `%s.CONCEPT` b "+
            "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND MEASUREMENT_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Exposure",
            "SELECT DISTINCT PERSON_ID, DEVICE_EXPOSURE_START_DATE as ENTRY_DATE "+
            "FROM `%s.DEVICE_EXPOSURE` a, `%s.CONCEPT` b "+
            "WHERE a.DEVICE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND DEVICE_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Drug",
            "SELECT DISTINCT PERSON_ID, DRUG_EXPOSURE_START_DATE as ENTRY_DATE "+
            "FROM `%s.DRUG_EXPOSURE` a, `%s.CONCEPT` b "+
            "WHERE a.DRUG_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND DRUG_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Procedure",
            "SELECT DISTINCT PERSON_ID, PROCEDURE_DATE as ENTRY_DATE "+
            "FROM `%s.PROCEDURE_OCCURRENCE` a, `%s.CONCEPT` b "+
            "WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND PROCEDURE_SOURCE_VALUE IN (%s)"
        );
        String subQuery = subqueryByCode.get(key);
        return String.format(subQuery, tablePrefix, tablePrefix, codesSymbol);
    }

    protected String getTablePrefix() {
        return "pmi-drc-api-test.synpuf";
    }
}
