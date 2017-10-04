package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({GroupCodesQueryBuilder.class})
public class GroupCodesQueryBuilderTest extends BaseQueryBuilderTest {

    @Autowired
    GroupCodesQueryBuilder queryBuilder;

    @Test
    public void buildQueryRequest() throws Exception {
        QueryParameters parameters = new QueryParameters()
                .type("ICD9")
                .parameters(Arrays.asList(new SearchParameter().value("11.1"),
                        new SearchParameter().value("11.2"),
                        new SearchParameter().value("11.3")));
        QueryRequest result = queryBuilder.buildQueryRequest(parameters);
        String expected =
                "select code,\n" +
                        "domain_id as domainId\n" +
                        "from `" + getTablePrefix() + ".icd9_criteria`\n" +
                        "where (code like @code0 or code like @code1 or code like @code2)\n" +
                        "and is_selectable = TRUE and is_group = FALSE order by code asc";
        String actual = result.getQuery();
        assertEquals(expected, actual);
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.GROUP_CODES, queryBuilder.getType());
    }

}
