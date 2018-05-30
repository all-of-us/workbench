package org.pmiops.workbench.cdr.dao;

import java.util.List;
import java.util.ArrayList;
import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;


public class PublicConceptSpecification{


    public Specification<Concept> getConceptSpecification(String keyword,List<String> domainIds,String conceptFilter){

        Specification<Concept> conceptSpecification =
                (root, criteriaQuery, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    List<Predicate> queryPredicates = new ArrayList<>();

                    Expression<Double> matchExp = criteriaBuilder.function("match", Double.class,
                            root.get("conceptName"), criteriaBuilder.literal(keyword));
                    queryPredicates.add(criteriaBuilder.greaterThan(matchExp, 0.0));

                    queryPredicates.add(criteriaBuilder.equal(root.get("conceptCode"),
                            criteriaBuilder.literal(keyword)));

                    try {
                        long conceptId = Long.parseLong(keyword);
                        queryPredicates.add(criteriaBuilder.equal(root.get("conceptId"),
                                criteriaBuilder.literal(conceptId)));
                    } catch (NumberFormatException e) {
                        // Not a long, don't try to match it to a concept ID.
                    }

                    predicates.add(criteriaBuilder.or(queryPredicates.toArray(new Predicate[0])));

                    if(conceptFilter.equals("S")){
                        predicates.add(criteriaBuilder.equal(root.get("standardConcept"),
                                criteriaBuilder.literal("S")));
                    }else{
                        List<Predicate> standardConceptPredicates = new ArrayList<>();
                        standardConceptPredicates.add(criteriaBuilder.isNull(root.get("standardConcept")));
                        standardConceptPredicates.add(criteriaBuilder.notEqual(root.get("standardConcept"),
                                criteriaBuilder.literal("S")));
                        predicates.add(criteriaBuilder.or(
                                standardConceptPredicates.toArray(new Predicate[0])));
                    }

                    if (domainIds != null) {
                        predicates.add(root.get("domainId").in(domainIds));
                    }

                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };
        return conceptSpecification;
        }
}

