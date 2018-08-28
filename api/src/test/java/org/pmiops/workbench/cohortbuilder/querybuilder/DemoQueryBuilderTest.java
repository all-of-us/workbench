package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@Import({DemoQueryBuilder.class})
public class DemoQueryBuilderTest {

    @Autowired
    DemoQueryBuilder queryBuilder;

    @Test
    public void buildQueryJobConfig() throws Exception {
        String genderNamedParameter = "";
        String ageNamedParameter = "";
        String raceNamedParameter = "";
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().type("DEMO").subtype("GEN").conceptId(8507L));
        params.add(new SearchParameter().type("DEMO").subtype("AGE").attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL).operands(Arrays.asList("18")))));
        params.add(new SearchParameter().type("DEM0").subtype("RACE").conceptId(1234L));
        params.add(new SearchParameter().type("DEM0").subtype("RACE").conceptId(1235L));

        QueryJobConfiguration queryJobConfiguration = queryBuilder
                .buildQueryJobConfig(new QueryParameters().type("DEMO").parameters(params));

        for (String key : queryJobConfiguration.getNamedParameters().keySet()) {
            if (key.startsWith("gen")) {
                genderNamedParameter = key;
            } else if (key.startsWith("race")) {
                raceNamedParameter = key;
            } else {
                ageNamedParameter = key;
            }
        }

        assertEquals("8507", queryJobConfiguration
                .getNamedParameters()
                .get(genderNamedParameter)
                .getArrayValues().get(0).getValue());
        assertEquals("18", queryJobConfiguration
                .getNamedParameters()
                .get(ageNamedParameter)
                .getValue());
        assertEquals("1234", queryJobConfiguration
                .getNamedParameters()
                .get(raceNamedParameter)
                .getArrayValues().get(0).getValue());
        assertEquals("1235", queryJobConfiguration
                .getNamedParameters()
                .get(raceNamedParameter)
                .getArrayValues().get(1).getValue());
    }

    @Test
    public void buildQueryJobConfig_NoAttributes() throws Exception {
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().domain("DEMO").subtype("AGE"));

        try {
            queryBuilder
                    .buildQueryJobConfig(new QueryParameters().type("DEMO").parameters(params));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException ex) {
            assertEquals("Age must provide an operator and operands.", ex.getMessage());
        }
    }

    @Test
    public void buildQueryJobConfig_AttributeWithNoOperands() throws Exception {
        List<SearchParameter> params = new ArrayList<>();
        params.add(new SearchParameter().domain("DEMO").subtype("AGE").attributes(Arrays.asList(new Attribute().operator(Operator.EQUAL))));

        try {
            queryBuilder
                    .buildQueryJobConfig(new QueryParameters().type("DEMO").parameters(params));
            fail("Should have thrown a BadRequestException!");
        } catch (BadRequestException ex) {
            assertEquals("Age must provide an operator and operands.", ex.getMessage());
        }
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.DEMO, queryBuilder.getType());
    }

}
