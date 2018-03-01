package org.pmiops.workbench.api;

import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.model.CodeDomainLookup;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodeDomainLookupService {

    @Autowired
    private CriteriaDao criteriaDao;

    /**
     * Find all domain ids for {@link SearchGroup}s in the following groups:
     * ICD9, ICD10 and CPT.
     *
     * @param searchGroups
     */
    public void findCodesForEmptyDomains(List<SearchGroup> searchGroups) {
        searchGroups.stream()
                .flatMap(searchGroup -> searchGroup.getItems().stream())
                .filter(item -> item.getType().matches("ICD9|ICD10|CPT"))
                .forEach(item -> {
                    List<SearchParameter> paramsWithDomains = new ArrayList<>();
                    for (SearchParameter parameter : item.getSearchParameters()) {
                        if (parameter.getDomain() == null || parameter.getDomain().isEmpty()) {
                            List<CodeDomainLookup> codeDomainLookups =
                                    criteriaDao.findCriteriaByTypeAndCode(parameter.getType(), parameter.getValue());

                            for (CodeDomainLookup row : codeDomainLookups) {
                                paramsWithDomains.add(new SearchParameter()
                                        .domain(row.getDomainId())
                                        .value(row.getCode())
                                        .type(parameter.getType()));
                            }
                        }
                        else {
                            paramsWithDomains.add(parameter);
                        }
                    }
                    item.setSearchParameters(paramsWithDomains);
                });
    }
}
