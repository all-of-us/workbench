package org.pmiops.workbench.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.CodeDomainLookup;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CodeDomainLookupServiceTest {

    @Mock
    private CriteriaDao criteriaDao;

    @Mock
    private CodeDomainLookup codeDomainLookup;

    @InjectMocks
    CodeDomainLookupService codeDomainLookupService;

    @Test
    public void findCodesForEmptyDomains() throws Exception {
        SearchParameter searchParameter1 = new SearchParameter()
                .type("ICD9")
                .value("001")
                .domain("Condition");
        SearchParameter searchParameter2 = new SearchParameter()
                .type("ICD9")
                .value("002")
                .domain(null);

        SearchGroupItem searchGroupItem1 = new SearchGroupItem()
                .type("ICD9")
                .addSearchParametersItem(searchParameter1)
                .addSearchParametersItem(searchParameter2);

        SearchRequest request = new SearchRequest()
                .addIncludesItem(new SearchGroup()
                        .addItemsItem(searchGroupItem1));

        List<CodeDomainLookup> lookups = new ArrayList<>();
        lookups.add(codeDomainLookup);

        when(criteriaDao.findCriteriaByTypeAndCode(searchParameter2.getType(), searchParameter2.getValue()))
                .thenReturn(lookups);
        when(codeDomainLookup.getDomainId()).thenReturn("Procedure");
        when(codeDomainLookup.getCode()).thenReturn("002");

        codeDomainLookupService.findCodesForEmptyDomains(request.getIncludes());

        assertEquals(2, searchGroupItem1.getSearchParameters().size());
        assertEquals("001", searchGroupItem1.getSearchParameters().get(0).getValue());
        assertEquals("Condition", searchGroupItem1.getSearchParameters().get(0).getDomain());
        assertEquals("002", searchGroupItem1.getSearchParameters().get(1).getValue());
        assertEquals("Procedure", searchGroupItem1.getSearchParameters().get(1).getDomain());

        verify(criteriaDao, times(1))
                .findCriteriaByTypeAndCode(searchParameter2.getType(), searchParameter2.getValue());
        verifyNoMoreInteractions(criteriaDao);
    }

}
