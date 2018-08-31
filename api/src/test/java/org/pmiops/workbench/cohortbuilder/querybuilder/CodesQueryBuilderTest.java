package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;
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
        final SearchParameter searchParam1 = new SearchParameter().type(TreeType.ICD9.name()).group(false).domain("Condition").value("001");
        final SearchParameter searchParam2 = new SearchParameter().type(TreeType.ICD9.name()).group(false).domain("Procedure").value("002");
        final SearchParameter searchParam3 = new SearchParameter().type(TreeType.ICD9.name()).group(false).domain("Procedure").value("003");
        final SearchParameter searchParam4 = new SearchParameter().type(TreeType.ICD9.name()).group(true).domain("Procedure").value("0");
        final SearchParameter searchParam5 = new SearchParameter().type(TreeType.ICD10.name()).group(false).domain("Condition").value("A001");
        final SearchParameter searchParam6 = new SearchParameter().type(TreeType.ICD10.name()).group(false).domain("Procedure").value("A002");
        final SearchParameter searchParam7 = new SearchParameter().type(TreeType.ICD10.name()).group(false).domain("Procedure").value("A003");
        final SearchParameter searchParam8 = new SearchParameter().type(TreeType.ICD10.name()).group(true).domain("Procedure").value("A0");
        SearchGroupItem item = new SearchGroupItem()
          .type(TreeType.ICD9.name())
          .searchParameters(
            Arrays.asList(
              searchParam1,
              searchParam2,
              searchParam3,
              searchParam4,
              searchParam5,
              searchParam6,
              searchParam7,
              searchParam8));

        ListMultimap<CodesQueryBuilder.MultiKey, SearchParameter> mappedParemeters =
          queryBuilder.getMappedParameters(item.getSearchParameters());
        assertEquals(6, mappedParemeters.keySet().size());
        assertEquals(
          new HashSet<CodesQueryBuilder.MultiKey>(
            Arrays.asList(
              queryBuilder.new MultiKey(searchParam1),
              queryBuilder.new MultiKey(searchParam2),
              queryBuilder.new MultiKey(searchParam4),
              queryBuilder.new MultiKey(searchParam5),
              queryBuilder.new MultiKey(searchParam6),
              queryBuilder.new MultiKey(searchParam8)
            )
          ), mappedParemeters.keySet());
        assertEquals(Arrays.asList(searchParam1), mappedParemeters.get(queryBuilder.new MultiKey(searchParam1)));
        assertEquals(Arrays.asList(searchParam2, searchParam3), mappedParemeters.get(queryBuilder.new MultiKey(searchParam2)));
        assertEquals(Arrays.asList(searchParam4), mappedParemeters.get(queryBuilder.new MultiKey(searchParam4)));
        assertEquals(Arrays.asList(searchParam5), mappedParemeters.get(queryBuilder.new MultiKey(searchParam5)));
        assertEquals(Arrays.asList(searchParam6, searchParam7), mappedParemeters.get(queryBuilder.new MultiKey(searchParam6)));
        assertEquals(Arrays.asList(searchParam8), mappedParemeters.get(queryBuilder.new MultiKey(searchParam8)));
    }

    @Test
    public void getType() throws Exception {
        assertEquals(FactoryKey.CODES, queryBuilder.getType());
    }
}
