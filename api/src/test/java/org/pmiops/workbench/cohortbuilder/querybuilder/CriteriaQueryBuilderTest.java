package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({CriteriaQueryBuilder.class})
public class CriteriaQueryBuilderTest {

    @Autowired
    CriteriaQueryBuilder queryBuilder;

    @Test
    public void buildQueryRequest() throws Exception {

        final String expected =
                "select id,\n" +
                        "type,\n" +
                        "code,\n" +
                        "name,\n" +
                        "est_count,\n" +
                        "is_group,\n" +
                        "is_selectable,\n" +
                        "concept_id,\n" +
                        "domain_id\n" +
                        "from `${projectId}.${dataSetId}.icd9_criteria`\n" +
                        "where parent_id = @parentId\n" +
                        "order by id asc";

        QueryJobConfiguration queryJobConfiguration = queryBuilder
                .buildQueryRequest(new QueryParameters().type("ICD9").parentId(0L));

        assertEquals(expected, queryJobConfiguration.getQuery());

        assertEquals("0", queryJobConfiguration.getNamedParameters()
                .get("parentId")
                .getValue());
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.CRITERIA, queryBuilder.getType());
    }

}
