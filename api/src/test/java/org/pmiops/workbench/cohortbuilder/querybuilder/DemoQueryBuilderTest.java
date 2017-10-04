package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({DemoQueryBuilder.class})
public class DemoQueryBuilderTest extends BaseQueryBuilderTest {

    @Autowired
    DemoQueryBuilder queryBuilder;

    @Test
    public void buildQueryRequest() throws Exception {
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().domain("DEMO_GEN").conceptId(8507L));
        params.add(new SearchParameter().domain("DEMO_AGE").value("20"));

        QueryRequest request = queryBuilder
                .buildQueryRequest(new QueryParameters().type("DEMO").parameters(params));

        String expected = "select distinct concat(cast(p.person_id as string), ',',\n" +
                "p.gender_source_value, ',',\n" +
                "p.race_source_value) as val\n" +
                "FROM `" + getTablePrefix() + ".person` p\n" +
                "where p.gender_concept_id = @genderConceptId\n" +
                "union distinct\n" +
                "select distinct concat(cast(p.person_id as string), ',',\n" +
                "p.gender_source_value, ',',\n" +
                "p.race_source_value) as val\n" +
                "FROM `" + getTablePrefix() + ".person` p\n" +
                "where DATE_DIFF(CURRENT_DATE, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), YEAR) = @age\n";

        assertEquals(expected, request.getQuery());

        assertEquals("8507", request
                .getNamedParameters()
                .get("genderConceptId")
                .getValue());
        assertEquals("20", request
                .getNamedParameters()
                .get("age")
                .getValue());
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.DEMO.getName(), queryBuilder.getType());
    }

}
