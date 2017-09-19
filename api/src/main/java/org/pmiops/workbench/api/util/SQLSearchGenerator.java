package org.pmiops.workbench.api.util;

import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

import java.util.*;

public class SQLSearchGenerator {

    public Map<String, List<String>> getMappedParameters(List<SearchParameter> searchParameters) {
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

    public List<String> getParameterWithEmptyDomainId(List<SearchParameter> searchParameters) {
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

    public String getSearchSQL(SearchRequest request) {
        List<SearchParameter> params = request.getSearchParameters();
        Map<String, List<String>> paramMap = getMappedParameters(params);
        StringBuilder queryString = new StringBuilder();
        List<String> queryParts = new ArrayList<String>();
        String subquery;

        if (request.getType().equals("ICD9")) {
            subquery = 
                "SELECT PERSON_ID || ',' || gender_source_value || ',' || x_race_ui as val "+
                "FROM `"+getTablePrefix()+".PERSON` "+
                "WHERE PERSON_ID IN "+
                "(SELECT PERSON_ID FROM (";
            queryString.append(subquery);

            for (String key : paramMap.keySet()) {
                List<String> values = paramMap.get(key);
                String codes = String.join(", ", values);
                subquery = formatSubquery(request.getType(), key, codes);
                queryParts.add(subquery);
            }

            queryString.append(String.join("UNION", queryParts));
            queryString.append("))");
        }

        return queryString.toString();
    }

    public String getTypeCM(String codeType) {
        String cm = "";
        if (codeType.equals("ICD9")) {
            cm = codeType + "CM";
        } else if (codeType.equals("ICD10")) {
            cm = codeType + "CM";
        } else if (codeType.equals("CPT")) {
            cm = "CPT4";
        }
        return cm;
    }

    public String getTypeProc(String codeType) {
        String proc = "";
        if (codeType.equals("ICD9")) {
            proc = codeType + "Proc";
        } else if (codeType.equals("ICD10")) {
            proc = codeType + "PCS";
        } else if (codeType.equals("CPT")) {
            proc = "CPT4";
        }
        return proc;
    }

    public String formatSubquery(String codeType, String domainId, String codes) {
        String cm, proc, tablePrefix;

        tablePrefix = getTablePrefix();
        cm = getTypeCM(codeType);
        proc = getTypeProc(codeType);

        Map<String, String> subqueryByCode = new HashMap<String, String>();
        subqueryByCode.put("Condition",
            "SELECT DISTINCT PERSON_ID, CONDITION_START_DATE as ENTRY_DATE "+
            "FROM `%s.CONDITION_OCCURRENCE` a, `%s.CONCEPT` b "+
            "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (%s,%s) " +
            "AND CONDITION_SOURCE_VALUE IN (%s) "
        );
        subqueryByCode.put("Observation",
            "SELECT DISTINCT PERSON_ID, OBSERVATION_DATE as ENTRY_DATE "+
            "FROM `%s.OBSERVATION` a, `%s.CONCEPT` b "+
            "WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (%s,%s) " +
            "AND OBSERVATION_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Measurement",
            "SELECT DISTINCT PERSON_ID, MEASUREMENT_DATE as ENTRY_DATE "+
            "FROM `%s.MEASUREMENT` a, `%s.CONCEPT` b "+
            "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (%s,%s) "+
            "AND MEASUREMENT_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Exposure",
            "SELECT DISTINCT PERSON_ID, DEVICE_EXPOSURE_START_DATE as ENTRY_DATE "+
            "FROM `%s.DEVICE_EXPOSURE` a, `%s.CONCEPT` b "+
            "WHERE a.DEVICE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (%s,%s) "+
            "AND DEVICE_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Drug",
            "SELECT DISTINCT PERSON_ID, DRUG_EXPOSURE_START_DATE as ENTRY_DATE "+
            "FROM `%s.DRUG_EXPOSURE` a, `%s.CONCEPT` b "+
            "WHERE a.DRUG_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (%s,%s) "+
            "AND DRUG_SOURCE_VALUE IN (%s)"
        );
        subqueryByCode.put("Procedure",
            "SELECT DISTINCT PERSON_ID, PROCEDURE_DATE as ENTRY_DATE "+
            "FROM `%s.PROCEDURE_OCCURRENCE` a, `%s.CONCEPT` b "+
            "WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (%s,%s) "+
            "AND PROCEDURE_SOURCE_VALUE IN (%s)"
        );

        String subQuery = subqueryByCode.get(domainId);
        return String.format(subQuery, tablePrefix, tablePrefix, cm, proc, codes);
    }

    public String getTablePrefix() {
        return "pmi-drc-api-test.synpuf";
    }
}
