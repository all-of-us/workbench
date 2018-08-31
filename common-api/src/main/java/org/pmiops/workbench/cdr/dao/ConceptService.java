package org.pmiops.workbench.cdr.dao;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptSynonym;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConceptService {

    public enum StandardConceptFilter {
        ALL_CONCEPTS,
        STANDARD_CONCEPTS,
        NON_STANDARD_CONCEPTS,
        STANDARD_OR_CODE_ID_MATCH
    }

    @PersistenceContext(unitName = "cdr")
    private EntityManager entityManager;

    @Autowired
    private ConceptSynonymDao conceptSynonymDao;

    public ConceptService() {
    }

    // Used for tests
    public ConceptService(EntityManager entityManager, ConceptSynonymDao conceptSynonymDao) {
        this.entityManager = entityManager;
        this.conceptSynonymDao = conceptSynonymDao;
    }

    public static String modifyMultipleMatchKeyword(String query){
        // This function modifies the keyword to match all the words if multiple words are present(by adding + before each word to indicate match that matching each word is essential)
        if(query == null || query.isEmpty()){
            return null;
        }
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

                    Expression<Double> matchExp = null;
                    Expression<Double> matchSynonymExp = null;

                    Subquery<ConceptSynonym> subquery = criteriaQuery.subquery(ConceptSynonym.class);
                    Root<ConceptSynonym> subqueryRoot = subquery.from(ConceptSynonym.class);


                    List<Predicate> conceptCodeIDName = new ArrayList<>();

                    final String keyword = modifyMultipleMatchKeyword(query);

                    if(keyword != null){
                        conceptCodeIDName.add(criteriaBuilder.equal(root.get("conceptCode"),
                                criteriaBuilder.literal(query)));
                        try {
                            long conceptId = Long.parseLong(query);
                            conceptCodeIDName.add(criteriaBuilder.equal(root.get("conceptId"),
                                    criteriaBuilder.literal(conceptId)));
                        } catch (NumberFormatException e) {
                            // Not a long, don't try to match it to a concept ID.
                        }
                        matchExp = criteriaBuilder.function("match", Double.class,
                                root.get("conceptName"), criteriaBuilder.literal(keyword));

                        subquery.select(subqueryRoot);

                        matchSynonymExp = criteriaBuilder.function("match", Double.class,
                                subqueryRoot.get("conceptSynonymName"), criteriaBuilder.literal(keyword));


                        Predicate synonymNamePredicate = criteriaBuilder.greaterThan(matchSynonymExp, 0.0);
                        subquery.select(subqueryRoot.get("conceptId")).where(synonymNamePredicate);
                    }



                    if (standardConceptFilter.equals(StandardConceptFilter.STANDARD_CONCEPTS)) {

                        if(keyword != null) {
                            conceptCodeIDName.add(criteriaBuilder.greaterThan(matchExp, 0.0));
                            conceptCodeIDName.add(root.get("conceptId").in(subquery));
                            predicates.add(
                                    criteriaBuilder.or(
                                            conceptCodeIDName.toArray(new Predicate[0])
                                    )
                            );
                        }

                        predicates.add(
                                criteriaBuilder.or(standardConceptPredicates.toArray(new Predicate[0]))
                        );

                    }else if (standardConceptFilter.equals(StandardConceptFilter.NON_STANDARD_CONCEPTS)) {

                        if(keyword != null) {
                            conceptCodeIDName.add(criteriaBuilder.greaterThan(matchExp, 0.0));
                            conceptCodeIDName.add(root.get("conceptId").in(subquery));
                            predicates.add(
                                    criteriaBuilder.or(
                                            conceptCodeIDName.toArray(new Predicate[0])
                                    )
                            );
                        }


                        predicates.add(
                                criteriaBuilder.or(
                                        criteriaBuilder.or(criteriaBuilder.isNull(root.get("standardConcept"))),
                                        criteriaBuilder.and(nonStandardConceptPredicates.toArray(new Predicate[0]))
                                ));

                    }else if (standardConceptFilter.equals(StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH)) {

                        if(keyword != null){
                            List<Predicate> conceptNameFilter = new ArrayList<>();
                            List<Predicate> matchFilter = new ArrayList<>();
                            matchFilter.add(criteriaBuilder.greaterThan(matchExp, 0.0));
                            matchFilter.add(root.get("conceptId").in(subquery));
                            conceptNameFilter.add(criteriaBuilder.or(matchFilter.toArray(new Predicate[0])));
                            conceptNameFilter.add(criteriaBuilder.or(standardConceptPredicates.toArray(new Predicate[0])));

                            predicates.add(
                                    criteriaBuilder.or(
                                            criteriaBuilder.or(conceptCodeIDName.toArray(new Predicate[0])),
                                            criteriaBuilder.and(conceptNameFilter.toArray(new Predicate[0]))
                                    ));
                        }


                    } else {

                        if (keyword != null) {

                            conceptCodeIDName.add(criteriaBuilder.greaterThan(matchExp, 0.0));
                            conceptCodeIDName.add(root.get("conceptId").in(subquery));
                            predicates.add(
                                    criteriaBuilder.or(
                                            conceptCodeIDName.toArray(new Predicate[0])
                                    )
                            );
                        }
                    }

                    if (vocabularyIds != null) {
                        predicates.add(root.get("vocabularyId").in(vocabularyIds));
                    }
                    if (domainIds != null) {
                        predicates.add(root.get("domainId").in(domainIds));
                    }

                    if(minCount == 1){
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
        Slice<Concept> conceptSlice = conceptDao.findAll(conceptSpecification, pageable);
        fetchConceptSynonyms(conceptSlice.getContent());
        return conceptSlice;
    }

    public List<Concept> fetchConceptSynonyms(List<Concept> concepts) {
        List<Long> conceptIds = concepts.stream().map(Concept::getConceptId).collect(Collectors.toList());
        Multimap<Long,ConceptSynonym> synonymMap = Multimaps.index(conceptSynonymDao.findByConceptIdIn(conceptIds),ConceptSynonym::getConceptId);
        for(Concept concept: concepts) {
            concept.setSynonyms(synonymMap.get(concept.getConceptId()).stream().collect(Collectors.toList()));
        }
        return concepts;
    }

}
