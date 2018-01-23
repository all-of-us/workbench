package org.pmiops.workbench.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CodeDomainLookupServiceTest {

    @Mock
    private CriteriaDao criteriaDao;

    @InjectMocks
    CodeDomainLookupService codeDomainLookupService;

    @Test
    public void findCodesForEmptyDomains_HasDomains() throws Exception {
        SearchRequest request = createSearchRequest();

        codeDomainLookupService.findCodesForEmptyDomains(request.getIncludes());

        List<SearchParameter> searchParameters = request.getIncludes().get(0).getItems().get(0).getSearchParameters();

        assertEquals(1, searchParameters.size());
        assertEquals("001", searchParameters.get(0).getValue());
        assertEquals("Condition", searchParameters.get(0).getDomain());
    }

    private SearchRequest createSearchRequest() {
        SearchParameter searchParameter1 = new SearchParameter()
                .value("001")
                .domain("Condition");

        List<SearchParameter> searchParameters = new ArrayList<>();
        searchParameters.add(searchParameter1);

        SearchGroupItem searchGroupItem1 = new SearchGroupItem()
                .type("ICD9")
                .searchParameters(searchParameters);

        List<SearchGroupItem> searchGroupItems = new ArrayList<>();
        searchGroupItems.add(searchGroupItem1);

        SearchGroup searchGroup1 = new SearchGroup();
        searchGroup1.items(searchGroupItems);

        List<SearchGroup> searchGroups = new ArrayList<>();
        searchGroups.add(searchGroup1);

        SearchRequest request = new SearchRequest();
        request.setIncludes(searchGroups);

        return request;
    }

}
