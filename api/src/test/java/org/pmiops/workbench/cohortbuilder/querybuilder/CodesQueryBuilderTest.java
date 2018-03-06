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
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@Import({CodesQueryBuilder.class})
public class CodesQueryBuilderTest {

    @Autowired
    CodesQueryBuilder queryBuilder;

    @Test
    public void buildQueryJobConfigICD9NotGroup() throws Exception {
        String measurementNamedParameter = "";
        String conditionNamedParameter = "";
        String cmConditionParameter = "";
        String procConditionParameter = "";
        String cmMeasurementParameter = "";
        String procMeasurementParameter = "";
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().group(false).type("ICD9").domain("Condition").value("10.1"));
        params.add(new SearchParameter().group(false).type("ICD9").domain("Condition").value("20.2"));
        params.add(new SearchParameter().group(false).type("ICD9").domain("Measurement").value("30.3"));

        /* Check the generated querybuilder */
        QueryJobConfiguration queryJobConfiguration = queryBuilder
                .buildQueryJobConfig(new QueryParameters().type("ICD9").parameters(params));

        for (String key : queryJobConfiguration.getNamedParameters().keySet()) {
            if (key.startsWith("Condition")) {
                conditionNamedParameter = key;
                cmConditionParameter = "cm" + key.replace("Condition", "");
                procConditionParameter = "proc" + key.replace("Condition", "");
            } else if (key.startsWith("Measurement")) {
                measurementNamedParameter = key;
                cmMeasurementParameter = "cm" + key.replace("Measurement", "");
                procMeasurementParameter = "proc" + key.replace("Measurement", "");
            }
        }

        String expected =
                "select person_id\n" +
                        "from `${projectId}.${dataSetId}.person` p\n" +
                        "where person_id in (select distinct person_id\n" +
                        "from `${projectId}.${dataSetId}.condition_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                        "where a.condition_source_concept_id = b.concept_id\n" +
                        "and b.vocabulary_id in (@" + cmConditionParameter + ",@" + procConditionParameter + ")\n" +
                        "and b.concept_code in unnest(@" + conditionNamedParameter + ")\n" +
                        " union distinct\n" +
                        "select distinct person_id\n" +
                        "from `${projectId}.${dataSetId}.measurement` a, `${projectId}.${dataSetId}.concept` b\n" +
                        "where a.measurement_source_concept_id = b.concept_id\n" +
                        "and b.vocabulary_id in (@" + cmMeasurementParameter + ",@" + procMeasurementParameter + ")\n" +
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

        assertEquals("ICD9CM", queryJobConfiguration.getNamedParameters().get(cmConditionParameter).getValue());
        assertEquals("ICD9Proc", queryJobConfiguration.getNamedParameters().get(procConditionParameter).getValue());
    }

    @Test
    public void buildQueryJobConfigICD9Group() throws Exception {
        String procedureNamedParameter = "";
        String cmProcedureParameter = "";
        String procProcedureParameter = "";
        String conditionNamedParameter = "";
        String cmConditionParameter = "";
        String procConditionParameter = "";
        String measurementNamedParameter = "";
        String cmMeasurementParameter = "";
        String procMeasurementParameter = "";
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().group(false).type("ICD9").domain("Condition").value("10.1"));
        params.add(new SearchParameter().group(false).type("ICD9").domain("Condition").value("20.2"));
        params.add(new SearchParameter().group(true).type("ICD9").domain("Measurement").value("0"));
        params.add(new SearchParameter().group(true).type("ICD9").domain("Procedure").value("1"));

        /* Check the generated querybuilder */
        QueryJobConfiguration queryJobConfiguration = queryBuilder
                .buildQueryJobConfig(new QueryParameters().type("ICD9").parameters(params));

        for (String key : queryJobConfiguration.getNamedParameters().keySet()) {
            if (key.startsWith("Condition")) {
                conditionNamedParameter = key;
                cmConditionParameter = "cm" + key.replace("Condition", "");
                procConditionParameter = "proc" + key.replace("Condition", "");
            } else if (key.startsWith("Measurement")) {
                measurementNamedParameter = key;
                cmMeasurementParameter = "cm" + key.replace("Measurement", "");
                procMeasurementParameter = "proc" + key.replace("Measurement", "");
            } else if (key.startsWith("Procedure")) {
                procedureNamedParameter = key;
                cmProcedureParameter = "cm" + key.replace("Procedure", "");
                procProcedureParameter = "proc" + key.replace("Procedure", "");
            }
        }

        String expected =
                "select person_id\n" +
                        "from `${projectId}.${dataSetId}.person` p\n" +
                        "where person_id in (select distinct person_id\n" +
                        "from `${projectId}.${dataSetId}.measurement` a, `${projectId}.${dataSetId}.concept` b\n" +
                        "where a.measurement_source_concept_id = b.concept_id\n" +
                        "and b.vocabulary_id in (@" + cmMeasurementParameter + ",@" + procMeasurementParameter + ")\n" +
                        "and b.concept_code like @" + measurementNamedParameter + "\n" +
                        " union distinct\n" +
                        "select distinct person_id\n" +
                        "from `${projectId}.${dataSetId}.procedure_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                        "where a.procedure_source_concept_id = b.concept_id\n" +
                        "and b.vocabulary_id in (@" + cmProcedureParameter + ",@" + procProcedureParameter + ")\n" +
                        "and b.concept_code like @" + procedureNamedParameter + "\n" +
                        " union distinct\n" +
                        "select distinct person_id\n" +
                        "from `${projectId}.${dataSetId}.condition_occurrence` a, `${projectId}.${dataSetId}.concept` b\n" +
                        "where a.condition_source_concept_id = b.concept_id\n" +
                        "and b.vocabulary_id in (@" + cmConditionParameter + ",@" + procConditionParameter + ")\n" +
                        "and b.concept_code in unnest(@" + conditionNamedParameter + ")\n" +
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

        String measurementCode = queryJobConfiguration
                .getNamedParameters()
                .get(measurementNamedParameter)
                .getValue();
        assertTrue("0%".equals(measurementCode));

        String procedureCode = queryJobConfiguration
                .getNamedParameters()
                .get(procedureNamedParameter)
                .getValue();
        assertTrue("1%".equals(procedureCode));

        assertEquals("ICD9CM", queryJobConfiguration.getNamedParameters().get(cmConditionParameter).getValue());
        assertEquals("ICD9Proc", queryJobConfiguration.getNamedParameters().get(procConditionParameter).getValue());
    }

    @Test
    public void getMappedParameters() throws Exception {
        final SearchParameter searchParam1 = new SearchParameter().group(false).domain("Condition").value("001");
        final SearchParameter searchParam2 = new SearchParameter().group(false).domain("Procedure").value("002");
        final SearchParameter searchParam3 = new SearchParameter().group(false).domain("Procedure").value("003");
        final SearchParameter searchParam4 = new SearchParameter().group(true).domain("Procedure").value("0");
        SearchGroupItem item = new SearchGroupItem()
                .type("ICD9")
                .searchParameters(
                        Arrays.asList(
                                searchParam1,
                                searchParam2,
                                searchParam3,
                                searchParam4));

        Map<CodesQueryBuilder.GroupType, ListMultimap<String, SearchParameter>> mappedParemeters =
                queryBuilder.getMappedParameters(item.getSearchParameters());
        assertEquals(2, mappedParemeters.keySet().size());
        assertEquals(new HashSet<CodesQueryBuilder.GroupType>(Arrays.asList(CodesQueryBuilder.GroupType.GROUP,
                CodesQueryBuilder.GroupType.NOT_GROUP)), mappedParemeters.keySet());
        assertEquals(Arrays.asList(searchParam1), mappedParemeters.get(CodesQueryBuilder.GroupType.NOT_GROUP).get("Condition"));
        assertEquals(Arrays.asList(searchParam2, searchParam3), mappedParemeters.get(CodesQueryBuilder.GroupType.NOT_GROUP).get("Procedure"));
        assertEquals(Arrays.asList(searchParam4), mappedParemeters.get(CodesQueryBuilder.GroupType.GROUP).get("Procedure"));
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.CODES, queryBuilder.getType());
    }
}
