package org.pmiops.workbench.cdr.dao;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

  public enum StandardConceptFilter {
    ALL_CONCEPTS,
    STANDARD_CONCEPTS,
    NON_STANDARD_CONCEPTS
  }

  @PersistenceContext(unitName = "cdr")
  private EntityManager entityManager;

  public ConceptService() {}

  // Used for tests
  public ConceptService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public static final String STANDARD_CONCEPT_CODE = "S";

  public Slice<Concept> searchConcepts(String query,
      StandardConceptFilter standardConceptFilter, List<String> vocabularyIds,
      List<String> domainIds, int limit) {
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
            if (standardConceptFilter.equals(StandardConceptFilter.STANDARD_CONCEPTS)) {
                predicates.add(criteriaBuilder.equal(root.get("standardConcept"),
                    criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
            } else if (standardConceptFilter.equals(StandardConceptFilter.NON_STANDARD_CONCEPTS)) {
              List<Predicate> standardConceptPredicates = new ArrayList<>();
              standardConceptPredicates.add(criteriaBuilder.isNull(root.get("standardConcept")));
              standardConceptPredicates.add(criteriaBuilder.notEqual(root.get("standardConcept"),
                  criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
              predicates.add(criteriaBuilder.or(
                  standardConceptPredicates.toArray(new Predicate[0])));
            }
            if (vocabularyIds != null) {
              predicates.add(root.get("vocabularyId").in(vocabularyIds));
            }
            if (domainIds != null) {
              predicates.add(root.get("domainId").in(domainIds));
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
