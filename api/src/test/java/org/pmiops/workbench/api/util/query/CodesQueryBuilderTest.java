package org.pmiops.workbench.api.util.query;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryRequest;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@Import({CodesQueryBuilder.class})
@SpringBootTest(classes = {TestBigQueryConfig.class})
public class CodesQueryBuilderTest {

    @Autowired
    CodesQueryBuilder queryBuilder;

    @Autowired
    WorkbenchConfig workbenchConfig;

    @Test
    public void buildQueryRequest() throws Exception {
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().domainId("Condition").code("10.1"));
        params.add(new SearchParameter().domainId("Condition").code("20.2"));
        params.add(new SearchParameter().domainId("Measurement").code("30.3"));

        /* Check the generated query */
        QueryRequest request = queryBuilder
                .buildQueryRequest(new QueryParameters().type("ICD9").parameters(params));

        String expected =
                "select distinct concat(" +
                        "cast(p.person_id as string), ',', " +
                        "p.gender_source_value, ',', " +
                        "p.race_source_value) as val " +
                        "from `" + getTablePrefix() + ".person` p " +
                        "where person_id in (" +
                        "select distinct person_id " +
                        "from `" + getTablePrefix() + ".condition_occurrence` a, `" + getTablePrefix() + ".concept` b " +
                        "where a.condition_source_concept_id = b.concept_id " +
                        "and b.vocabulary_id in (@cm,@proc) " +
                        "and b.concept_code in unnest(@Conditioncodes)" +
                        " union distinct " +
                        "select distinct person_id " +
                        "from `" + getTablePrefix() + ".measurement` a, `" + getTablePrefix() + ".concept` b " +
                        "where a.measurement_source_concept_id = b.concept_id " +
                        "and b.vocabulary_id in (@cm,@proc) " +
                        "and b.concept_code in unnest(@Measurementcodes))";

        assertEquals(expected, request.getQuery());

        /* Check the query parameters */
        List<QueryParameterValue> conditionCodes = request
                .getNamedParameters()
                .get("Conditioncodes")
                .getArrayValues();
        assertTrue(conditionCodes.contains(QueryParameterValue
                .newBuilder()
                .setValue("10.1")
                .setType(StandardSQLTypeName.STRING)
                .build()));
        assertTrue(conditionCodes.contains(QueryParameterValue
                .newBuilder()
                .setValue("20.2")
                .setType(StandardSQLTypeName.STRING)
                .build()));

        List<QueryParameterValue> measurementCodes = request
                .getNamedParameters()
                .get("Measurementcodes")
                .getArrayValues();
        assertTrue(measurementCodes.contains(QueryParameterValue
                .newBuilder()
                .setValue("30.3")
                .setType(StandardSQLTypeName.STRING)
                .build()));

        assertEquals("ICD9CM", request.getNamedParameters().get("cm").getValue());
        assertEquals("ICD9Proc", request.getNamedParameters().get("proc").getValue());
    }

    @Test
    public void getMappedParameters() throws Exception {
        SearchGroupItem item = new SearchGroupItem()
                .type("ICD9")
                .searchParameters(Arrays.asList(
                        new SearchParameter().domainId("Condition").code("001"),
                        new SearchParameter().domainId("Procedure").code("002"),
                        new SearchParameter().domainId("Procedure").code("003")));

        ListMultimap<String, String> mappedParemeters = queryBuilder.getMappedParameters(item.getSearchParameters());
        assertEquals(2, mappedParemeters.keySet().size());
        assertEquals(new HashSet<String>(Arrays.asList("Condition", "Procedure")), mappedParemeters.keySet());
        assertEquals(Arrays.asList("001"), mappedParemeters.get("Condition"));
        assertEquals(Arrays.asList("002", "003"), mappedParemeters.get("Procedure"));
    }

    private String getTablePrefix() {
        return workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
    }
}
