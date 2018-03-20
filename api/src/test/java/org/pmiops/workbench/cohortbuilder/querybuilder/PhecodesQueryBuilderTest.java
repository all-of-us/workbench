package org.pmiops.workbench.cohortbuilder.querybuilder;

import static org.junit.Assert.*;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@Import({PhecodesQueryBuilder.class})
public class PhecodesQueryBuilderTest {

    @Autowired
    PhecodesQueryBuilder queryBuilder;

    @Test
    public void buildQueryJobConfig() throws Exception {
        String pheCodesNamedParameter = "";
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().value("008"));
        params.add(new SearchParameter().value("010"));
        params.add(new SearchParameter().value("031"));

        /* Check the generated querybuilder */
        QueryJobConfiguration queryJobConfiguration = queryBuilder
                .buildQueryJobConfig(new QueryParameters().type("PHECODE").parameters(params));

        for (String key : queryJobConfiguration.getNamedParameters().keySet()) {
            if (key.startsWith("PheCodes")) {
                pheCodesNamedParameter = key;
            }
        }

        String expected =
                "select person_id\n" +
                        "from `${projectId}.${dataSetId}.condition_occurrence` co\n" +
                        "where co.condition_source_concept_id in (select concept_id\n" +
                        "from `${projectId}.${dataSetId}.concept`\n" +
                        "where concept_code in\n" +
                        "(select icd9 from `${projectId}.${dataSetId}.phecode_criteria_icd`\n" +
                        "where phecode in unnest(@" + pheCodesNamedParameter + "))\n" +
                        "and vocabulary_id in ('ICD9Proc', 'ICD9CM')\n" +
                        ")\n" +
                        "union all\n" +
                        "select person_id\n" +
                        "from `${projectId}.${dataSetId}.procedure_occurrence` po\n" +
                        "where po.procedure_source_concept_id in (select concept_id\n" +
                        "from `${projectId}.${dataSetId}.concept`\n" +
                        "where concept_code in\n" +
                        "(select icd9 from `${projectId}.${dataSetId}.phecode_criteria_icd`\n" +
                        "where phecode in unnest(@" + pheCodesNamedParameter + "))\n" +
                        "and vocabulary_id in ('ICD9Proc', 'ICD9CM')\n" +
                        ")\n" +
                        "union all\n" +
                        "select person_id\n" +
                        "from `${projectId}.${dataSetId}.measurement` m\n" +
                        "where m.measurement_source_concept_id in (select concept_id\n" +
                        "from `${projectId}.${dataSetId}.concept`\n" +
                        "where concept_code in\n" +
                        "(select icd9 from `${projectId}.${dataSetId}.phecode_criteria_icd`\n" +
                        "where phecode in unnest(@" + pheCodesNamedParameter + "))\n" +
                        "and vocabulary_id in ('ICD9Proc', 'ICD9CM')\n" +
                        ")\n" +
                        "union all\n" +
                        "select person_id\n" +
                        "from `${projectId}.${dataSetId}.observation` o\n" +
                        "where o.observation_source_concept_id in (select concept_id\n" +
                        "from `${projectId}.${dataSetId}.concept`\n" +
                        "where concept_code in\n" +
                        "(select icd9 from `${projectId}.${dataSetId}.phecode_criteria_icd`\n" +
                        "where phecode in unnest(@" + pheCodesNamedParameter + "))\n" +
                        "and vocabulary_id in ('ICD9Proc', 'ICD9CM')\n" +
                        ")\n";

        assertEquals(expected, queryJobConfiguration.getQuery());

        /* Check the querybuilder parameters */
        List<QueryParameterValue> pheCodes = queryJobConfiguration
                .getNamedParameters()
                .get(pheCodesNamedParameter)
                .getArrayValues();
        assertTrue(pheCodes.contains(QueryParameterValue
                .newBuilder()
                .setValue("008")
                .setType(StandardSQLTypeName.STRING)
                .build()));
        assertTrue(pheCodes.contains(QueryParameterValue
                .newBuilder()
                .setValue("010")
                .setType(StandardSQLTypeName.STRING)
                .build()));
        assertTrue(pheCodes.contains(QueryParameterValue
                .newBuilder()
                .setValue("031")
                .setType(StandardSQLTypeName.STRING)
                .build()));
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.PHECODE, queryBuilder.getType());
    }

}
