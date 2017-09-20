package org.pmiops.workbench.api.util;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({SQLGenerator.class})
public class SQLGeneratorTest {
    static String TABLE_PREFIX = "pmi-drc-api-test.synpuf";

    private String showValues(String expected, String actual) {
        return String.format("\nExpected: %s\nActual:   %s", expected, actual);
    }

    @Autowired
    SQLGenerator generator;

    @Test
    public void findGroupCodes() throws Exception {
        QueryRequest result = generator.findGroupCodes("ICD9", Arrays.asList("11.1", "11.2", "11.3"));
        String expected =
                "SELECT code, domain_id AS domainId FROM `" + TABLE_PREFIX + ".icd9_criteria` " +
                "WHERE (code LIKE @11.1 OR code LIKE @11.2 OR code LIKE @11.3) " +
                "AND is_selectable = 1 AND is_group = 0 ORDER BY code ASC";
        String actual = result.getQuery();
        assert actual.equals(expected) : showValues(expected, actual);
    }

    @Test
    public void handleSearch() throws Exception {
        List<SearchParameter> params = new ArrayList<>();
        SearchParameter p;

        p = new SearchParameter();
        p.setDomainId("Condition");
        p.setCode("10.1");
        params.add(p);

        p = new SearchParameter();
        p.setDomainId("Condition");
        p.setCode("20.2");
        params.add(p);

        p = new SearchParameter();
        p.setDomainId("Measurement");
        p.setCode("30.3");
        params.add(p);

        /* Check the generated query */
        QueryRequest request = generator.handleSearch("ICD9", params);
        String actual = request.getQuery();
        String expected = 
            "SELECT DISTINCT CONCAT(" +
                "CAST(p.person_id AS string), ',', " +
                "p.gender_source_value, ',', " +
                "p.race_source_value) AS val "+
            "FROM `" + TABLE_PREFIX + ".person` p " +
            "WHERE PERSON_ID IN (" +
                "SELECT DISTINCT PERSON_ID " +
                "FROM `" + TABLE_PREFIX + ".condition_occurrence` a, `" + TABLE_PREFIX + ".CONCEPT` b " +
                "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID " +
                "AND b.VOCABULARY_ID IN (@cm,@proc) " +
                "AND a.CONDITION_SOURCE_VALUE IN (@Conditioncodes)"+
                " UNION DISTINCT " +
                "SELECT DISTINCT PERSON_ID " +
                "FROM `" + TABLE_PREFIX + ".measurement` a, `" + TABLE_PREFIX + ".CONCEPT` b " +
                "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID " +
                "AND b.VOCABULARY_ID IN (@cm,@proc) " +
                "AND a.MEASUREMENT_SOURCE_VALUE IN (@Measurementcodes))";
        assert actual.equals(expected) : showValues(expected, actual);

        /* Check the query parameters */
        QueryParameterValue param;
        Map<String, QueryParameterValue> preparedParams = request.getNamedParameters();
        param = preparedParams.get("Conditioncodes");
        assert param.getValue().equals("10.1, 20.2");
        param = preparedParams.get("Measurementcodes");
        assert param.getValue().equals("30.3");
        param = preparedParams.get("cm");
        assert param.getValue().equals("ICD9CM");
        param = preparedParams.get("proc");
        assert param.getValue().equals("ICD9Proc");
    }

    @Test
    public void findParametersWithEmptyDomainIds() throws Exception {
        final SearchParameter searchParameterCondtion = new SearchParameter();
        searchParameterCondtion.setCode("001");

        final SearchParameter searchParameterCondtion2 = new SearchParameter();
        searchParameterCondtion2.setCode("002");
        searchParameterCondtion2.setDomainId("Condition");

        List<SearchParameter> parameterList = new ArrayList<>();
        parameterList.add(searchParameterCondtion);
        parameterList.add(searchParameterCondtion2);

        SearchRequest request = new SearchRequest();
        request.setType("ICD9");
        request.setSearchParameters(parameterList);

        assertEquals(Arrays.asList("001%"), generator.findParametersWithEmptyDomainIds(request.getSearchParameters()));
        assertEquals(1, request.getSearchParameters().size());

        SearchParameter searchParameter = new SearchParameter();
        searchParameter.setCode("002");
        searchParameter.setDomainId("Condition");
        assertEquals(searchParameter, request.getSearchParameters().get(0));
    }

    @Test
    public void getSubQuery() throws Exception {
        Map<String, String> expectedByKey = new HashMap<String, String>();
        expectedByKey.put("Condition",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".condition_occurrence` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND a.CONDITION_SOURCE_VALUE IN (@Conditioncodes)"
        );
        expectedByKey.put("Observation",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".observation` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) " +
            "AND a.OBSERVATION_SOURCE_VALUE IN (@Observationcodes)"
        );
        expectedByKey.put("Measurement",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".measurement` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.MEASUREMENT_SOURCE_VALUE IN (@Measurementcodes)"
        );
        expectedByKey.put("Exposure",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".device_exposure` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.DEVICE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.DEVICE_SOURCE_VALUE IN (@Exposurecodes)"
        );
        expectedByKey.put("Drug",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".drug_exposure` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.DRUG_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.DRUG_SOURCE_VALUE IN (@Drugcodes)"
        );
        expectedByKey.put("Procedure",
            "SELECT DISTINCT PERSON_ID " +
            "FROM `" + TABLE_PREFIX + ".procedure_occurrence` a, `" + TABLE_PREFIX + ".CONCEPT` b "+
            "WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.CONCEPT_ID "+
            "AND b.VOCABULARY_ID IN (@cm,@proc) "+
            "AND a.PROCEDURE_SOURCE_VALUE IN (@Procedurecodes)"
        );

        List<String> keys = Arrays.asList("Condition", "Observation", "Measurement", "Exposure", "Drug", "Procedure");
        String actual, expected;
        for (String key : keys) {
            actual = generator.getSubQuery(key);
            expected = expectedByKey.get(key);
            assert actual.equals(expected) : showValues(expected, actual);
        }
    }

    @Test
    public void getTablePrefix() throws Exception {
        // FYI: This is basically a Stub
        String actual = generator.getTablePrefix();
        assert actual.equals(TABLE_PREFIX) : showValues(TABLE_PREFIX, actual);
    }

    @Test
    public void getMappedParameters() throws Exception {
        final SearchParameter searchParameterCondtion = new SearchParameter();
        searchParameterCondtion.setDomainId("Condition");
        searchParameterCondtion.setCode("001");

        final SearchParameter searchParameterProc1 = new SearchParameter();
        searchParameterProc1.setDomainId("Procedure");
        searchParameterProc1.setCode("002");

        final SearchParameter searchParameterProc2 = new SearchParameter();
        searchParameterProc2.setDomainId("Procedure");
        searchParameterProc2.setCode("003");

        SearchRequest request = new SearchRequest();
        request.setType("ICD9");
        request.setSearchParameters(Arrays.asList(searchParameterCondtion, searchParameterProc1, searchParameterProc2));

        List<SearchParameter> parameters = request.getSearchParameters();
        assertEquals(2, generator.getMappedParameters(parameters).size());
        assertEquals(new HashSet<String>(Arrays.asList("Condition", "Procedure")), generator.getMappedParameters(parameters).keySet());
        assertEquals(Arrays.asList("001"), generator.getMappedParameters(parameters).get("Condition"));
        assertEquals(Arrays.asList("002", "003"), generator.getMappedParameters(parameters).get("Procedure"));
    }
}
