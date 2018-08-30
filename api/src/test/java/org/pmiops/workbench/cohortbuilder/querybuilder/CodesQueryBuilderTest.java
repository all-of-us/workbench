package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@Import({CodesQueryBuilder.class})
public class CodesQueryBuilderTest {

    @Autowired
    CodesQueryBuilder queryBuilder;

    @Test
    public void getMappedParameters() throws Exception {
        final SearchParameter searchParam1 = new SearchParameter().group(false).domain("Condition").value("001");
        final SearchParameter searchParam2 = new SearchParameter().group(false).domain("Procedure").value("002");
        final SearchParameter searchParam3 = new SearchParameter().group(false).domain("Procedure").value("003");
        final SearchParameter searchParam4 = new SearchParameter().group(true).domain("Procedure").value("0");
        SearchGroupItem item = new SearchGroupItem()
                .type(TreeType.ICD9.name())
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
