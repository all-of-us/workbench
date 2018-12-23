package org.pmiops.workbench.cdr.dao;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {

    public static class ConceptIds {

      private final List<Long> standardConceptIds;
      private final List<Long> sourceConceptIds;

      public ConceptIds(List<Long> standardConceptIds, List<Long> sourceConceptIds) {
        this.standardConceptIds = standardConceptIds;
        this.sourceConceptIds = sourceConceptIds;
      }

      public List<Long> getStandardConceptIds() {
        return standardConceptIds;
      }

      public List<Long> getSourceConceptIds() {
        return sourceConceptIds;
      }

    }

    public enum StandardConceptFilter {
        ALL_CONCEPTS,
        STANDARD_CONCEPTS,
        NON_STANDARD_CONCEPTS,
        STANDARD_OR_CODE_ID_MATCH
    }

    @PersistenceContext(unitName = "cdr")
    private EntityManager entityManager;

    @Autowired
    private ConceptDao conceptDao;

    public ConceptService() {
    }

    // Used for tests
    public ConceptService(EntityManager entityManager, ConceptDao conceptDao) {
        this.entityManager = entityManager;
        this.conceptDao = conceptDao;
    }

    public static String modifyMultipleMatchKeyword(String query) {
        // This function modifies the keyword to match all the words if multiple words are present(by adding + before each word to indicate match that matching each word is essential)
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        String[] keywords = query.split("[,+\\s+]");
        List<String> temp = new ArrayList<>();
        for (String key: keywords) {
            if (!key.isEmpty()) {
                if (key.contains("-") && !temp.contains(key)) {
                    temp.add(key);
                } else if (key.contains("*") && key.length() > 1) {
                    temp.add(new String("+" + key));
                }
                else {
                    if (key.length() < 3) {
                        temp.add(key);
                    } else {
                        temp.add(new String("+" + key));
                    }
                }
            }

        }

        StringBuilder query2 = new StringBuilder();
        for (String key : temp) {
            query2.append(key);
        }

        return query2.toString();
    }

    public static final String STANDARD_CONCEPT_CODE = "S";
    public static final String CLASSIFICATION_CONCEPT_CODE = "C";

    public Slice<Concept> searchConcepts(String query, StandardConceptFilter standardConceptFilter, List<String> vocabularyIds, List<String> domainIds, int limit, int minCount) {


        Specification<Concept> conceptSpecification =
                (root, criteriaQuery, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    List<Predicate> standardConceptPredicates = new ArrayList<>();
                    standardConceptPredicates.add(criteriaBuilder.equal(root.get("standardConcept"),
                            criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
                    standardConceptPredicates.add(criteriaBuilder.equal(root.get("standardConcept"),
                            criteriaBuilder.literal(CLASSIFICATION_CONCEPT_CODE)));

                    List<Predicate> nonStandardConceptPredicates = new ArrayList<>();
                    nonStandardConceptPredicates.add(criteriaBuilder.notEqual(root.get("standardConcept"),
                            criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
                    nonStandardConceptPredicates.add(criteriaBuilder.notEqual(root.get("standardConcept"),
                            criteriaBuilder.literal(CLASSIFICATION_CONCEPT_CODE)));

                    final String keyword = modifyMultipleMatchKeyword(query);

                    if (keyword != null) {
                      Expression<Double> matchExp = criteriaBuilder.function("matchConcept", Double.class,
                                root.get("conceptName"), root.get("conceptCode"), root.get("vocabularyId"),
                          root.get("synonymsStr"), criteriaBuilder.literal(keyword));
                      predicates.add(criteriaBuilder.greaterThan(matchExp, 0.0));
                    }

                    if (standardConceptFilter.equals(StandardConceptFilter.STANDARD_CONCEPTS)) {
                        predicates.add(
                                criteriaBuilder.or(standardConceptPredicates.toArray(new Predicate[0]))
                        );
                    } else if (standardConceptFilter.equals(StandardConceptFilter.NON_STANDARD_CONCEPTS)) {
                        predicates.add(
                                criteriaBuilder.or(
                                        criteriaBuilder.or(criteriaBuilder.isNull(root.get("standardConcept"))),
                                        criteriaBuilder.and(nonStandardConceptPredicates.toArray(new Predicate[0]))
                                ));

                    } else if (standardConceptFilter.equals(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH)) {
                        if(keyword != null){
                          List<Predicate> standardOrCodeOrIdMatch = new ArrayList<>();
                          standardOrCodeOrIdMatch.add(criteriaBuilder.equal(root.get("conceptCode"),
                              criteriaBuilder.literal(query)));
                          try {
                            long conceptId = Long.parseLong(query);
                            standardOrCodeOrIdMatch.add(criteriaBuilder.equal(root.get("conceptId"),
                                criteriaBuilder.literal(conceptId)));
                          } catch (NumberFormatException e) {
                            // Not a long, don't try to match it to a concept ID.
                          }
                          standardOrCodeOrIdMatch.add(
                              criteriaBuilder.or(standardConceptPredicates.toArray(new Predicate[0])));
                          predicates.add(criteriaBuilder.or(standardOrCodeOrIdMatch.toArray(new Predicate[0])));
                        } else {
                          predicates.add(criteriaBuilder.or(standardConceptPredicates.toArray(new Predicate[0])));
                        }

                    }

                    if (vocabularyIds != null) {
                        predicates.add(root.get("vocabularyId").in(vocabularyIds));
                    }
                    if (domainIds != null) {
                        predicates.add(root.get("domainId").in(domainIds));
                    }

                    if (minCount == 1) {
                        List<Predicate> countPredicates = new ArrayList<>();
                        countPredicates.add(criteriaBuilder.greaterThan(root.get("countValue"), 0));
                        countPredicates.add(criteriaBuilder.greaterThan(root.get("sourceCountValue"), 0));

                        predicates.add(criteriaBuilder.or(
                                countPredicates.toArray(new Predicate[0])));
                    }
                    criteriaQuery.distinct(true);
                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };
        // Return up to limit results, sorted in descending count value order.

        Pageable pageable = new PageRequest(0, limit,
                new Sort(Direction.DESC, "countValue"));
        NoCountFindAllDao<Concept, Long> conceptDao = new NoCountFindAllDao<>(Concept.class,
                entityManager);
        return conceptDao.findAll(conceptSpecification, pageable);
    }

    public ConceptIds classifyConceptIds(Set<Long> conceptIds) {
      ImmutableList.Builder<Long> standardConceptIds = ImmutableList.builder();
      ImmutableList.Builder<Long> sourceConceptIds = ImmutableList.builder();

      Iterable<Concept> concepts = conceptDao.findAll(conceptIds);
      for (Concept concept : concepts) {
        if (ConceptService.STANDARD_CONCEPT_CODE.equals(concept.getStandardConcept())
            || ConceptService.CLASSIFICATION_CONCEPT_CODE.equals(concept.getStandardConcept())) {
          standardConceptIds.add(concept.getConceptId());
        } else {
          // We may need to handle classification / concept hierarchy here eventually...
          sourceConceptIds.add(concept.getConceptId());
        }
      }
      return new ConceptIds(standardConceptIds.build(), sourceConceptIds.build());
    }
}
