package org.pmiops.workbench.api;

import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DomainLookupService {

    private CriteriaDao criteriaDao;

    @Autowired
    public DomainLookupService(CriteriaDao criteriaDao) {
        this.criteriaDao = criteriaDao;
    }

    /**
     * Find all domain ids for {@link SearchGroup}s in the following groups:
     * ICD9, ICD10 and CPT.
     *
     * @param searchGroups
     */
    public void findCodesForEmptyDomains(List<SearchGroup> searchGroups) {
        searchGroups.stream()
                .flatMap(searchGroup -> searchGroup.getItems().stream())
                .filter(item -> item.getType().matches("ICD9|ICD10"))
                .forEach(item -> {
                    List<SearchParameter> paramsWithDomains = new ArrayList<>();
                        for (SearchParameter parameter : item.getSearchParameters()) {
                        if (parameter.getDomain() == null || parameter.getDomain().isEmpty()) {
                            List<String> domainLookups =
                                    (parameter.getSubtype() == null)
                                            ? criteriaDao.findCriteriaByTypeAndCode(parameter.getType(), parameter.getValue())
                                            : criteriaDao.findCriteriaByTypeAndSubtypeAndCode(parameter.getType(),
                                            parameter.getSubtype(), parameter.getValue());

                            for (String row : domainLookups) {
                                paramsWithDomains.add(new SearchParameter()
                                        .domain(row)
                                        .value(parameter.getValue())
                                        .type(parameter.getType())
                                        .subtype(parameter.getSubtype())
                                        .group(parameter.getGroup()));
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
