package org.pmiops.workbench.cdr.dao;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {

  private final EntityManager entityManager;

  public ConceptService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public static final String STANDARD_CONCEPT_CODE = "S";

  public Slice<Concept> searchConcepts(String query,
      Boolean standardConcept, String vocabularyId, String domainId,
      int limit) {
    Specification<Concept> conceptSpecification =
        (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Check that the concept name, code, or ID matches the query string.
            List<Predicate> queryPredicates = new ArrayList<>();
            Expression<Double> matchExp = criteriaBuilder.function("match", Double.class,
                root.get("conceptName"), criteriaBuilder.literal(query));
            queryPredicates.add(criteriaBuilder.greaterThan(matchExp, 0.0));
            queryPredicates.add(criteriaBuilder.equal(root.get("conceptCode"),
                criteriaBuilder.literal(query)));
            try {
              long conceptId = Long.parseLong(query);
              queryPredicates.add(criteriaBuilder.equal(root.get("conceptId"),
                  criteriaBuilder.literal(conceptId)));
            } catch (NumberFormatException e) {
              // Not a long, don't try to match it to a concept ID.
            }
            predicates.add(criteriaBuilder.or(queryPredicates.toArray(new Predicate[0])));

            // Optionally filter on standard concept, vocabulary ID, domain ID
            if (standardConcept != null) {
              if (standardConcept) {
                predicates.add(criteriaBuilder.equal(root.get("standardConcept"),
                    criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
              } else {
                List<Predicate> standardConceptPredicates = new ArrayList<>();
                standardConceptPredicates.add(criteriaBuilder.isNull(root.get("standardConcept")));
                standardConceptPredicates.add(criteriaBuilder.notEqual(root.get("standardConcept"),
                    criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
                predicates.add(criteriaBuilder.or(
                    standardConceptPredicates.toArray(new Predicate[0])));
              }
            }
            if (vocabularyId != null) {
              predicates.add(criteriaBuilder.equal(root.get("vocabularyId"),
                  criteriaBuilder.literal(vocabularyId)));
            }
            if (domainId != null) {
              predicates.add(criteriaBuilder.equal(root.get("domainId"),
                  criteriaBuilder.literal(domainId)));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    // Return up to limit results, sorted in descending count value order.
    Pageable pageable = new PageRequest(0, limit,
        new Sort(Direction.DESC, "countValue"));
    NoCountFindAllDao<Concept, Long> conceptDao = new NoCountFindAllDao<>(Concept.class,
        entityManager);
    return conceptDao.findAll(conceptSpecification, pageable);
  }
}
