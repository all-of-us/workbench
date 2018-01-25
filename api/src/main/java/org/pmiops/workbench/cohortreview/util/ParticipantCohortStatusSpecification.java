package org.pmiops.workbench.cohortreview.util;

import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class ParticipantCohortStatusSpecification implements Specification<ParticipantCohortStatus> {

    private SearchCriteria criteria;

    public ParticipantCohortStatusSpecification(final SearchCriteria criteria) {
        super();
        this.criteria = criteria;
    }

    public SearchCriteria getCriteria() {
        return criteria;
    }

    @Override
    public Predicate toPredicate(Root<ParticipantCohortStatus> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        switch (criteria.getOperation()) {
            case EQUALITY:
                if (criteria.getKey().contains(".")) {
                    query.orderBy(builder.asc(root.get("genderConceptId")));
                    return builder.equal(root.get(criteria.getKey().substring(0, criteria.getKey().indexOf(".")))
                            .get(criteria.getKey().substring(criteria.getKey().indexOf(".")+1, criteria.getKey().length())), criteria.getValue());
                }
                return builder.equal(root.get(criteria.getKey()), criteria.getValue());
            case CONTAINS:
                return builder.like(root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
            default:
                return null;
        }
    }
}
