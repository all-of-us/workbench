package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConceptService {

    public enum StandardConceptFilter {
        ALL_CONCEPTS,
        STANDARD_CONCEPTS,
        NON_STANDARD_CONCEPTS
    }

    @PersistenceContext(unitName = "cdr")
    private EntityManager entityManager;

    public ConceptService() {
    }

    // Used for tests
    public ConceptService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static String modifyMultipleMatchKeyword(String query){
        // This function modifies the keyword to match all the words if multiple words are present(by adding + before each word to indicate match that matching each word is essential)
        String[] keywords = query.split("[,+\\s+]");
        for(int i = 0; i < keywords.length; i++){
            String key = keywords[i];
            if(key.length() < 3 && !key.isEmpty()){
                key = "\"" + key + "\"";
                keywords[i] = key;
            }
        }

        StringBuilder query2 = new StringBuilder();
        for(String key : keywords){
            if(!key.isEmpty()){
                if(query2.length()==0){
                    query2.append("+");
                    query2.append(key);
                }else if(key.contains("\"")){
                    query2.append(key);
                }else{
                    query2.append("+");
                    query2.append(key);
                }
            }

        }
        return query2.toString();
    }

    public static final String STANDARD_CONCEPT_CODE = "S";

    public Slice<Concept> searchConcepts(String query,
                                         StandardConceptFilter standardConceptFilter, List<String> vocabularyIds,
                                         List<String> domainIds, int limit) {


        final String keyword = modifyMultipleMatchKeyword(query);

        Specification<Concept> conceptSpecification =
                (root, criteriaQuery, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    // Check that the concept name, code, or ID matches the query string.
                    List<Predicate> conceptCode_Id = new ArrayList<>();

                    conceptCode_Id.add(criteriaBuilder.equal(root.get("conceptCode"),
                            criteriaBuilder.literal(query)));

                    try {
                        long conceptId = Long.parseLong(query);
                        conceptCode_Id.add(criteriaBuilder.equal(root.get("conceptId"),
                                criteriaBuilder.literal(conceptId)));
                    } catch (NumberFormatException e) {
                        // Not a long, don't try to match it to a concept ID.
                    }

                    List<Predicate> conceptName_Filter = new ArrayList<>();


                    Expression<Double> matchExp = criteriaBuilder.function("match", Double.class,
                            root.get("conceptName"), criteriaBuilder.literal(keyword));
                    conceptName_Filter.add(criteriaBuilder.greaterThan(matchExp, 0.0));


                    // Optionally filter on standard concept, vocabulary ID, domain ID
                    if (standardConceptFilter.equals(StandardConceptFilter.STANDARD_CONCEPTS)) {
                        conceptName_Filter.add(criteriaBuilder.equal(root.get("standardConcept"),
                                criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
                    } else if (standardConceptFilter.equals(StandardConceptFilter.NON_STANDARD_CONCEPTS)) {
                        List<Predicate> standardConceptPredicates = new ArrayList<>();
                        standardConceptPredicates.add(criteriaBuilder.isNull(root.get("standardConcept")));
                        standardConceptPredicates.add(criteriaBuilder.notEqual(root.get("standardConcept"),
                                criteriaBuilder.literal(STANDARD_CONCEPT_CODE)));
                        conceptName_Filter.add(criteriaBuilder.or(
                                standardConceptPredicates.toArray(new Predicate[0])));
                    }

                    predicates.add(
                            criteriaBuilder.or(
                                    criteriaBuilder.or(conceptCode_Id.toArray(new Predicate[0])),
                                    criteriaBuilder.and(conceptName_Filter.toArray(new Predicate[0]))
                            ));

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
