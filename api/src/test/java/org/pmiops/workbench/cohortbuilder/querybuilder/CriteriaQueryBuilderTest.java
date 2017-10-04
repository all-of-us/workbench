package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({CriteriaQueryBuilder.class})
@SpringBootTest(classes = {TestBigQueryConfig.class})
public class CriteriaQueryBuilderTest {

    @Autowired
    CriteriaQueryBuilder queryBuilder;

    @Autowired
    WorkbenchConfig workbenchConfig;

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
                        "from `" + getTablePrefix() + ".icd9_criteria`\n" +
                        "where parent_id = @parentId\n" +
                        "order by id asc";

        QueryRequest request = queryBuilder
                .buildQueryRequest(new QueryParameters().type("ICD9").parentId(0L));

        assertEquals(expected, request.getQuery());
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.CRITERIA.getName(), queryBuilder.getType());
    }

    private String getTablePrefix() {
        return workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
    }

}
