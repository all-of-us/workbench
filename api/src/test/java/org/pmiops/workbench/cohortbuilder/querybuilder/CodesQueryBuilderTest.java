package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CodesQueryBuilderTest {

    @Autowired
    CodesQueryBuilder queryBuilder;

    @Test
    public void buildQueryJobConfig() throws Exception {
        String measurementNamedParameter = "";
        String conditionNamedParameter = "";
        String cmNamedParameter = "";
        String procNamedParameter = "";
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().domain("Condition").value("10.1"));
        params.add(new SearchParameter().domain("Condition").value("20.2"));
        params.add(new SearchParameter().domain("Measurement").value("30.3"));

        /* Check the generated querybuilder */
        QueryJobConfiguration queryJobConfiguration = queryBuilder
                .buildQueryJobConfig(new QueryParameters().type("ICD9").parameters(params));

        for (String key : queryJobConfiguration.getNamedParameters().keySet()) {
            if (key.startsWith("Condition")) {
                conditionNamedParameter = key;
            } else if (key.startsWith("Measurement")) {
                measurementNamedParameter = key;
            } else if (key.startsWith("cmICD9")) {
                cmNamedParameter = key;
            } else if (key.startsWith("procICD9")) {
                procNamedParameter = key;
            }
        }

        String expected =
                "select person_id\n" +
                        "from `${projectId}.${dataSetId}.person` p\n" +
                        "where person_id in (select distinct person_id\n" +
                        "from `${projectId}.${dataSetId}.condition_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                        "where a.condition_source_concept_id = b.concept_id\n" +
                        "and b.vocabulary_id in (@" + cmNamedParameter + ",@" + procNamedParameter + ")\n" +
                        "and b.concept_code in unnest(@" + conditionNamedParameter + ")\n" +
                        " union distinct\n" +
                        "select distinct person_id\n" +
                        "from `${projectId}.${dataSetId}.measurement` a, `${projectId}.${dataSetId}.concept` b\n" +
                        "where a.measurement_source_concept_id = b.concept_id\n" +
                        "and b.vocabulary_id in (@" + cmNamedParameter + ",@" + procNamedParameter + ")\n" +
                        "and b.concept_code in unnest(@" + measurementNamedParameter + ")\n" +
                        ")\n";

        assertEquals(expected, queryJobConfiguration.getQuery());

        /* Check the querybuilder parameters */
        List<QueryParameterValue> conditionCodes = queryJobConfiguration
                .getNamedParameters()
                .get(conditionNamedParameter)
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

        List<QueryParameterValue> measurementCodes = queryJobConfiguration
                .getNamedParameters()
                .get(measurementNamedParameter)
                .getArrayValues();
        assertTrue(measurementCodes.contains(QueryParameterValue
                .newBuilder()
                .setValue("30.3")
                .setType(StandardSQLTypeName.STRING)
                .build()));

        assertEquals("ICD9CM", queryJobConfiguration.getNamedParameters().get(cmNamedParameter).getValue());
        assertEquals("ICD9Proc", queryJobConfiguration.getNamedParameters().get(procNamedParameter).getValue());
    }

    @Test
    public void getMappedParameters() throws Exception {
        SearchGroupItem item = new SearchGroupItem()
                .type("ICD9")
                .searchParameters(Arrays.asList(
                        new SearchParameter().domain("Condition").value("001"),
                        new SearchParameter().domain("Procedure").value("002"),
                        new SearchParameter().domain("Procedure").value("003")));

        ListMultimap<String, String> mappedParemeters = queryBuilder.getMappedParameters(item.getSearchParameters());
        assertEquals(2, mappedParemeters.keySet().size());
        assertEquals(new HashSet<String>(Arrays.asList("Condition", "Procedure")), mappedParemeters.keySet());
        assertEquals(Arrays.asList("001"), mappedParemeters.get("Condition"));
        assertEquals(Arrays.asList("002", "003"), mappedParemeters.get("Procedure"));
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.CODES, queryBuilder.getType());
    }
}
