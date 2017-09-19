package org.pmiops.workbench.api.util;

import org.junit.Test;
import org.pmiops.workbench.api.CohortBuilderController;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class SQLSearchGeneratorTest {

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

        SQLSearchGenerator controller = new SQLSearchGenerator();
        List<SearchParameter> parameters = request.getSearchParameters();
        assertEquals(2, controller.getMappedParameters(parameters).size());
        assertEquals(new HashSet<String>(Arrays.asList("Condition", "Procedure")), controller.getMappedParameters(parameters).keySet());
        assertEquals(Arrays.asList("001"), controller.getMappedParameters(parameters).get("Condition"));
        assertEquals(Arrays.asList("002", "003"), controller.getMappedParameters(parameters).get("Procedure"));
    }


    @Test
    public void getParameterWithEmptyDomainId() throws Exception {
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

        SQLSearchGenerator controller = new SQLSearchGenerator();
        assertEquals(Arrays.asList("001%"), controller.getParameterWithEmptyDomainId(request.getSearchParameters()));
        assertEquals(1, request.getSearchParameters().size());

        SearchParameter searchParameter = new SearchParameter();
        searchParameter.setCode("002");
        searchParameter.setDomainId("Condition");
        assertEquals(searchParameter, request.getSearchParameters().get(0));
    }
}