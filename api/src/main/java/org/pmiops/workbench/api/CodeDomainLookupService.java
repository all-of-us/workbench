package org.pmiops.workbench.api;

import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.CodeDomainLookup;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.AbstractQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CodeDomainLookupService {

    @Autowired
    private CriteriaDao criteriaDao;

    /**
     * TODO: this is temporary and will be removed when we figure out the conceptId mappings
     * for ICD9, ICD10 and CPT codes.
     **/
    public void findCodesForEmptyDomains(List<SearchGroup> searchGroups) {
        AbstractQueryBuilder builder = QueryBuilderFactory.getQueryBuilder(FactoryKey.GROUP_CODES);
        searchGroups.stream()
                .flatMap(searchGroup -> searchGroup.getItems().stream())
                .filter(item -> item.getType().matches("ICD9|ICD10|CPT"))
                .forEach(item -> {

                    for (SearchParameter parameter : item.getSearchParameters()) {
                        if (parameter.getDomain() == null || parameter.getDomain().isEmpty()) {
                            List<CodeDomainLookup> codeDomainLookups =
                                    criteriaDao.findByCodeInAndSelectableIsTrueAndGroupIsFalseOrderByCodeAsc(
                                    Arrays.asList(parameter.getValue()));

                            List<SearchParameter> paramsWithDomains = new ArrayList<>();
                            for (CodeDomainLookup row : codeDomainLookups) {
                                paramsWithDomains.add(new SearchParameter()
                                        .domain(row.getDomainId())
                                        .value(row.getCode()));
                            }
                            item.setSearchParameters(paramsWithDomains);
                        }
                    }
                });
    }
}
